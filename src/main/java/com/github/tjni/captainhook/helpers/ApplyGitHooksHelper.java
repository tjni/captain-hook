package com.github.tjni.captainhook.helpers;

import com.github.tjni.captainhook.annotations.ImmutableStyle;
import com.github.tjni.captainhook.annotations.VisibleForTesting;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.immutables.value.Value;

/**
 * Adds and removes Git hooks.
 *
 * <p>Consider adding a pre-commit hook. The file layout after running the task will be:
 *
 * <pre>
 *   .git/
 *   |-- hooks/
 *   |   |-- pre-commit
 *   |   |-- git-hooks/
 *   |   |   `-- pre-commit
 * </pre>
 *
 * <p>The {@code pre-commit} file underneath {@code hooks} executes the {@code pre-commit} file
 * underneath {@code git-hooks}, which contains the majority of the logic.
 *
 * <p>This helper can add and remove its hooks without affecting existing hooks, assuming that the
 * existing hook is a shell script.
 */
@Singleton
public class ApplyGitHooksHelper {
  private static final Logger LOG = Logging.getLogger(ApplyGitHooksHelper.class);

  private final FilesHelper filesHelper;
  private final GitHelper gitHelper;
  private final GradleHelper gradleHelper;
  private final Clock clock;

  @Inject
  public ApplyGitHooksHelper(
      FilesHelper filesHelper, GitHelper gitHelper, GradleHelper gradleHelper, Clock clock) {
    this.filesHelper = filesHelper;
    this.gitHelper = gitHelper;
    this.gradleHelper = gradleHelper;
    this.clock = clock;
  }

  public void apply(Map<GitHook, String> gitHooks) {
    Path gitHooksDir = findGitHooksDirectory();

    if (!gitHooks.isEmpty()) {
      createGitHookDirectories(gitHooksDir);
    }

    GitHookTemplate template = new GitHookTemplate(filesHelper, createFreeMarkerConfiguration());

    addHookScripts(gitHooksDir, template, gitHooks);
    applyHooks(gitHooksDir, gitHooks);
    removeHookScripts(gitHooksDir, gitHooks);
    cleanEmptyHookScriptsDirectory(gitHooksDir);
  }

  @VisibleForTesting
  void addHookScripts(Path gitHooksDir, GitHookTemplate template, Map<GitHook, String> gitHooks) {
    EntryStream.of(gitHooks)
        .removeValues(String::isEmpty)
        .mapKeys(gitHook -> getGitHookScriptFile(gitHooksDir, gitHook))
        .mapValues(this::getGitHookTemplateData)
        .mapKeyValue(template::writeTemplate)
        .forEach(this::makeExecutable);
  }

  @VisibleForTesting
  void removeHookScripts(Path gitHooksDir, Map<GitHook, String> gitHooks) {
    StreamEx.of(GitHook.values())
        .mapToEntry(gitHook -> gitHooks.getOrDefault(gitHook, ""))
        .filterValues(String::isEmpty)
        .keys()
        .map(gitHook -> getGitHookScriptFile(gitHooksDir, gitHook))
        .forEach(filesHelper::deleteIfExists);
  }

  @VisibleForTesting
  void applyHooks(Path gitHooksDir, Map<GitHook, String> gitHooks) {
    Map<GitHook, String> fullGitHooks =
        StreamEx.of(GitHook.values())
            .mapToEntry(gitHook -> gitHooks.getOrDefault(gitHook, ""))
            .toImmutableMap();

    EntryStream.of(fullGitHooks)
        .removeValues(String::isEmpty)
        .keys()
        .map(gitHook -> addGitHook(gitHooksDir, gitHook))
        .forEach(this::makeExecutable);

    EntryStream.of(fullGitHooks)
        .filterValues(String::isEmpty)
        .keys()
        .mapPartial(gitHook -> removeGitHook(gitHooksDir, gitHook))
        .forEach(this::makeExecutable);
  }

  @VisibleForTesting
  Path findGitHooksDirectory() {
    return gitHelper.getCommonDirectory().resolve("hooks");
  }

  @VisibleForTesting
  void createGitHookDirectories(Path gitHooksDir) {
    filesHelper.createDirectories(getGitHookScriptsDirectory(gitHooksDir));
  }

  private static Path getGitHookScriptsDirectory(Path gitHooksDir) {
    return gitHooksDir.resolve("git-hooks");
  }

  private static Path getGitHookScriptFile(Path gitHooksDir, GitHook gitHook) {
    return getGitHookScriptsDirectory(gitHooksDir).resolve(gitHook.getHookName());
  }

  private Path addGitHook(Path gitHooksDir, GitHook gitHook) {
    Path gitHookFile = gitHooksDir.resolve(gitHook.getHookName());
    String currentHookString =
        filesHelper.exists(gitHookFile) ? filesHelper.toString(gitHookFile).trim() : "#!/bin/sh -";

    Path gitHookScriptFile = getGitHookScriptFile(gitHooksDir, gitHook);
    String executeCommand = String.format("%n%n%s", gitHookScriptFile);

    if (currentHookString.contains(executeCommand)) {
      return gitHookFile;
    }

    String newHookString = currentHookString + executeCommand;
    return filesHelper.write(gitHookFile, newHookString);
  }

  private Optional<Path> removeGitHook(Path gitHooksDir, GitHook gitHook) {
    Path gitHookFile = gitHooksDir.resolve(gitHook.getHookName());

    if (!filesHelper.exists(gitHookFile)) {
      return Optional.empty();
    }

    String currentHookString = filesHelper.toString(gitHookFile).trim();

    Path gitHookScriptFile = getGitHookScriptFile(gitHooksDir, gitHook);
    String executeCommand = String.format("%n%n%s", gitHookScriptFile);

    if (!currentHookString.contains(executeCommand)) {
      return Optional.of(gitHookFile);
    }

    String newHookString = currentHookString.replace(executeCommand, "");

    if ("#!/bin/sh -".equals(newHookString)) {
      filesHelper.delete(gitHookFile);
      return Optional.empty();
    } else {
      return Optional.of(filesHelper.write(gitHookFile, newHookString));
    }
  }

  private void makeExecutable(Path gitHookFile) {
    try {
      filesHelper.setPosixFilePermissions(gitHookFile, "rwxr--r--");
    } catch (UnsupportedOperationException e) {
      LOG.debug("Setting POSIX file permissions is unsupported, skipping.", e);
    } catch (Exception e) {
      LOG.warn("An error occurred when setting POSIX file permissions for {}.", gitHookFile);
    }
  }

  @VisibleForTesting
  void cleanEmptyHookScriptsDirectory(Path gitHooksDir) {
    Path gitHookScriptsDirectory = getGitHookScriptsDirectory(gitHooksDir);

    if (!filesHelper.exists(gitHookScriptsDirectory)) {
      return;
    }

    if (!filesHelper.isDirectoryEmpty(gitHookScriptsDirectory)) {
      return;
    }

    filesHelper.delete(gitHookScriptsDirectory);
  }

  @VisibleForTesting
  Configuration createFreeMarkerConfiguration() {
    Configuration freeMarkerConfiguration = new Configuration(Configuration.VERSION_2_3_29);
    freeMarkerConfiguration.setClassForTemplateLoading(getClass(), "/templates");
    freeMarkerConfiguration.setDefaultEncoding(StandardCharsets.UTF_8.name());
    freeMarkerConfiguration.setTemplateExceptionHandler(
        gradleHelper.isDebugLoggingEnabled()
            ? TemplateExceptionHandler.DEBUG_HANDLER
            : TemplateExceptionHandler.RETHROW_HANDLER);
    freeMarkerConfiguration.setLogTemplateExceptions(false);
    freeMarkerConfiguration.setWrapUncheckedExceptions(true);
    freeMarkerConfiguration.setFallbackOnNullLoopVariable(false);
    return freeMarkerConfiguration;
  }

  @VisibleForTesting
  GitHookTemplateData getGitHookTemplateData(String hookScript) {
    return ImmutableGitHookTemplateData.builder()
        .setCreatedAt(ZonedDateTime.ofInstant(clock.instant(), clock.getZone()))
        .setHookScript(hookScript)
        .build();
  }

  @VisibleForTesting
  static class GitHookTemplate {
    private final FilesHelper _filesHelper;
    private final Template _template;

    @VisibleForTesting
    GitHookTemplate(FilesHelper filesHelper, Configuration freeMarkerConfiguration) {
      _filesHelper = filesHelper;
      _template = getTemplate(freeMarkerConfiguration);
    }

    private Template getTemplate(Configuration freeMarkerConfiguration) {
      try {
        return freeMarkerConfiguration.getTemplate("git-hook.ftl");
      } catch (IOException e) {
        throw new RuntimeException(
            "An error occurred when reading the git-hook.ftl template file.", e);
      }
    }

    @VisibleForTesting
    Path writeTemplate(Path outputFile, GitHookTemplateData data) {
      try (Writer writer = _filesHelper.createFileOutputStreamWriter(outputFile)) {
        _template.process(data, writer);
      } catch (IOException | TemplateException e) {
        throw new RuntimeException("An error occurred when writing to " + outputFile + ".", e);
      }
      return outputFile;
    }
  }

  @Value.Immutable
  @ImmutableStyle
  public abstract static class GitHookTemplateData {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a z");

    @Value.Auxiliary
    abstract ZonedDateTime getCreatedAt();

    public abstract String getHookScript();

    @Value.Derived
    public String getFormattedCreatedAt() {
      return DATE_TIME_FORMATTER.format(getCreatedAt());
    }
  }
}
