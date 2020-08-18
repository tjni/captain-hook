package com.github.tjni.captainhook.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.github.tjni.captainhook.helpers.ApplyGitHooksHelper.GitHookTemplate;
import com.github.tjni.captainhook.helpers.ApplyGitHooksHelper.GitHookTemplateData;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class ApplyGitHooksHelperTest {
  private static final Path GIT_COMMON_DIR = Paths.get("/Workspace/.git");
  private static final Path GIT_HOOKS_DIR = GIT_COMMON_DIR.resolve("hooks");
  private static final Path GIT_HOOK_SCRIPTS_DIR = GIT_HOOKS_DIR.resolve("git-hooks");

  private final FilesHelper filesHelper;
  private final GitHelper gitHelper;
  private final GradleHelper gradleHelper;
  private final Clock clock;
  private final ApplyGitHooksHelper applyGitHooksHelper;

  ApplyGitHooksHelperTest(
      @Mock(name = "filesHelper") FilesHelper filesHelper,
      @Mock(name = "gitHelper") GitHelper gitHelper,
      @Mock(name = "gradleHelper") GradleHelper gradleHelper,
      @Mock(name = "clock") Clock clock) {
    this.filesHelper = filesHelper;
    this.gitHelper = gitHelper;
    this.gradleHelper = gradleHelper;
    this.clock = clock;
    applyGitHooksHelper = new ApplyGitHooksHelper(filesHelper, gitHelper, gradleHelper, clock);
  }

  @Test
  void addHookScripts_ShouldWriteExecutableHookScriptFiles() {
    // Given:
    given(clock.instant()).willReturn(Instant.now());
    given(clock.getZone()).willReturn(ZoneId.of("America/Los_Angeles"));

    String preCommitScript = "echo \"pre-commit\"";
    String preRebaseScript = "echo \"pre-rebase\"";

    Map<GitHook, String> gitHooks = new HashMap<>();
    gitHooks.put(GitHook.PRE_COMMIT, preCommitScript);
    gitHooks.put(GitHook.PRE_REBASE, preRebaseScript);

    GitHookTemplate template = mock(GitHookTemplate.class);
    given(template.writeTemplate(any(), any())).will(returnsFirstArg());

    // When:
    applyGitHooksHelper.addHookScripts(GIT_HOOKS_DIR, template, gitHooks);

    // Then:
    Path preCommitScriptFile = GIT_HOOK_SCRIPTS_DIR.resolve(GitHook.PRE_COMMIT.getHookName());
    Path preRebaseScriptFile = GIT_HOOK_SCRIPTS_DIR.resolve(GitHook.PRE_REBASE.getHookName());
    GitHookTemplateData preCommitData = applyGitHooksHelper.getGitHookTemplateData(preCommitScript);
    GitHookTemplateData preRebaseData = applyGitHooksHelper.getGitHookTemplateData(preRebaseScript);

    verify(template).writeTemplate(preCommitScriptFile, preCommitData);
    verify(template).writeTemplate(preRebaseScriptFile, preRebaseData);
    verify(filesHelper).setPosixFilePermissions(preCommitScriptFile, "rwxr--r--");
    verify(filesHelper).setPosixFilePermissions(preRebaseScriptFile, "rwxr--r--");
  }

  @Test
  void removeHookScripts_ShouldDeleteHookScriptFiles() {
    // Given:
    Map<GitHook, String> gitHooks = Collections.singletonMap(GitHook.PRE_COMMIT, "");

    // When:
    applyGitHooksHelper.removeHookScripts(GIT_HOOKS_DIR, gitHooks);

    // Then:
    Path preCommitScriptFile = GIT_HOOK_SCRIPTS_DIR.resolve(GitHook.PRE_COMMIT.getHookName());
    Path preRebaseScriptFile = GIT_HOOK_SCRIPTS_DIR.resolve(GitHook.PRE_REBASE.getHookName());
    verify(filesHelper).deleteIfExists(preCommitScriptFile);
    verify(filesHelper).deleteIfExists(preRebaseScriptFile);
  }

  @Test
  void applyHooks_WhenAddingHookAndFileDoesNotExist_ShouldWriteNewExecutableHookFile() {
    // Given:
    Map<GitHook, String> gitHooks =
        Collections.singletonMap(GitHook.PRE_COMMIT, "echo \"pre-commit\"");

    Path gitHookFile = GIT_HOOKS_DIR.resolve(GitHook.PRE_COMMIT.getHookName());

    given(filesHelper.exists(gitHookFile)).willReturn(false);
    given(filesHelper.write(any(), anyString())).will(returnsFirstArg());

    // When:
    applyGitHooksHelper.applyHooks(GIT_HOOKS_DIR, gitHooks);

    // Then:
    String expectedHookString =
        String.format(
            "#!/bin/sh -%n%n`dirname \"$0\"`/git-hooks/%s", GitHook.PRE_COMMIT.getHookName());
    verify(filesHelper).write(gitHookFile, expectedHookString);
    verify(filesHelper).setPosixFilePermissions(gitHookFile, "rwxr--r--");
  }

  @Test
  void applyHooks_WhenAddingHookButFileExists_ShouldAddToHookFileAndMakeFileExecutable() {
    // Given:
    Map<GitHook, String> gitHooks =
        Collections.singletonMap(GitHook.PRE_COMMIT, "echo \"pre-commit\"");

    Path gitHookFile = GIT_HOOKS_DIR.resolve(GitHook.PRE_COMMIT.getHookName());

    String currentHookString = String.format("#!/bin/bash -%n%necho \"Already exists.\"");
    given(filesHelper.exists(gitHookFile)).willReturn(true);
    given(filesHelper.toString(gitHookFile)).willReturn(currentHookString);
    given(filesHelper.write(any(), anyString())).will(returnsFirstArg());

    // When:
    applyGitHooksHelper.applyHooks(GIT_HOOKS_DIR, gitHooks);

    // Then:
    String expectedHookString =
        String.format(
            "%s%n%n%s%s",
            currentHookString, "`dirname \"$0\"`/git-hooks/", GitHook.PRE_COMMIT.getHookName());
    verify(filesHelper).write(gitHookFile, expectedHookString);
    verify(filesHelper).setPosixFilePermissions(gitHookFile, "rwxr--r--");
  }

  @Test
  void applyHooks_WhenAddingHookButHookExists_ShouldSkipWriteAndMakeFileExecutable() {
    // Given:
    Map<GitHook, String> gitHooks =
        Collections.singletonMap(GitHook.PRE_COMMIT, "echo \"pre-commit\"");

    Path gitHookFile = GIT_HOOKS_DIR.resolve(GitHook.PRE_COMMIT.getHookName());

    String existingHookString =
        String.format(
            "#!/bin/sh -%n%n`dirname \"$0\"`/git-hooks/%s", GitHook.PRE_COMMIT.getHookName());

    given(filesHelper.exists(gitHookFile)).willReturn(true);
    given(filesHelper.toString(gitHookFile)).willReturn(existingHookString);

    // When:
    applyGitHooksHelper.applyHooks(GIT_HOOKS_DIR, gitHooks);

    // Then:
    verify(filesHelper, never()).write(eq(gitHookFile), anyString());
    verify(filesHelper).setPosixFilePermissions(gitHookFile, "rwxr--r--");
  }

  @Test
  void applyHooks_WhenRemovingHookLeavesNothingBehind_ShouldDeleteFile() {
    // Given:
    Map<GitHook, String> gitHooks = Collections.singletonMap(GitHook.PRE_COMMIT, "");

    Path preCommitFile = GIT_HOOKS_DIR.resolve(GitHook.PRE_COMMIT.getHookName());
    Path preRebaseFile = GIT_HOOKS_DIR.resolve(GitHook.PRE_REBASE.getHookName());

    String leftoverHookString = "#!/bin/sh -";
    given(filesHelper.exists(any())).willReturn(false);
    given(filesHelper.exists(preCommitFile)).willReturn(true);
    given(filesHelper.exists(preRebaseFile)).willReturn(true);
    given(filesHelper.toString(preCommitFile))
        .willReturn(
            String.format(
                "%s%n%n%s%s",
                leftoverHookString,
                "`dirname \"$0\"`/git-hooks/",
                GitHook.PRE_COMMIT.getHookName()));
    given(filesHelper.toString(preRebaseFile))
        .willReturn(
            String.format(
                "%s%n%n%s%s",
                leftoverHookString,
                "`dirname \"$0\"`/git-hooks/",
                GitHook.PRE_REBASE.getHookName()));

    // When:
    applyGitHooksHelper.applyHooks(GIT_HOOKS_DIR, gitHooks);

    // Then:
    verify(filesHelper).delete(preCommitFile);
    verify(filesHelper).delete(preRebaseFile);
    verify(filesHelper, never()).write(any(), anyString());
    verify(filesHelper, never()).setPosixFilePermissions(any(), anyString());
  }

  @Test
  void applyHooks_WhenRemovingHookLeavesSomethingBehind_ShouldRemoveHookAndMakeFileExecutable() {
    // Given:
    Map<GitHook, String> gitHooks = Collections.singletonMap(GitHook.PRE_COMMIT, "");

    Path preCommitFile = GIT_HOOKS_DIR.resolve(GitHook.PRE_COMMIT.getHookName());
    Path preRebaseFile = GIT_HOOKS_DIR.resolve(GitHook.PRE_REBASE.getHookName());

    String leftoverHookString = String.format("#!/bin/bash -%n%necho \"Already exists.\"");
    given(filesHelper.exists(any())).willReturn(false);
    given(filesHelper.exists(preCommitFile)).willReturn(true);
    given(filesHelper.exists(preRebaseFile)).willReturn(true);
    given(filesHelper.toString(preCommitFile))
        .willReturn(
            String.format(
                "%s%n%n%s%s",
                leftoverHookString,
                "`dirname \"$0\"`/git-hooks/",
                GitHook.PRE_COMMIT.getHookName()));
    given(filesHelper.toString(preRebaseFile))
        .willReturn(
            String.format(
                "%s%n%n%s%s",
                leftoverHookString,
                "`dirname \"$0\"`/git-hooks/",
                GitHook.PRE_REBASE.getHookName()));
    given(filesHelper.write(any(), any())).will(returnsFirstArg());

    // When:
    applyGitHooksHelper.applyHooks(GIT_HOOKS_DIR, gitHooks);

    // Then:
    verify(filesHelper, never()).delete(any());
    verify(filesHelper).write(preCommitFile, leftoverHookString);
    verify(filesHelper).write(preRebaseFile, leftoverHookString);
    verify(filesHelper).setPosixFilePermissions(preCommitFile, "rwxr--r--");
    verify(filesHelper).setPosixFilePermissions(preRebaseFile, "rwxr--r--");
  }

  @Test
  void applyHooks_WhenRemovingHookButHookDoesNotExist_ShouldSkipDeleteAndMakeFileExecutable() {
    // Given:
    Map<GitHook, String> gitHooks = Collections.singletonMap(GitHook.PRE_COMMIT, "");

    Path preCommitFile = GIT_HOOKS_DIR.resolve(GitHook.PRE_COMMIT.getHookName());
    Path preRebaseFile = GIT_HOOKS_DIR.resolve(GitHook.PRE_REBASE.getHookName());

    given(filesHelper.exists(any())).willReturn(false);
    given(filesHelper.exists(preCommitFile)).willReturn(true);
    given(filesHelper.exists(preRebaseFile)).willReturn(true);
    given(filesHelper.toString(preCommitFile)).willReturn("echo \"Blah\"");
    given(filesHelper.toString(preRebaseFile)).willReturn("echo \"Blah\"");

    // When:
    applyGitHooksHelper.applyHooks(GIT_HOOKS_DIR, gitHooks);

    // Then:
    verify(filesHelper, never()).delete(any());
    verify(filesHelper, never()).write(any(), anyString());
    verify(filesHelper).setPosixFilePermissions(preCommitFile, "rwxr--r--");
    verify(filesHelper).setPosixFilePermissions(preRebaseFile, "rwxr--r--");
  }

  @Test
  void applyHooks_WhenRemovingHookButFileDoesNotExist_ShouldSkipDelete() {
    // Given:
    Map<GitHook, String> gitHooks = Collections.singletonMap(GitHook.PRE_COMMIT, "");

    given(filesHelper.exists(any())).willReturn(false);

    // When:
    applyGitHooksHelper.applyHooks(GIT_HOOKS_DIR, gitHooks);

    // Then:
    verify(filesHelper, never()).delete(any());
    verify(filesHelper, never()).write(any(), anyString());
  }

  @Test
  void findGitHooksDirectory_ShouldAppendHooksToGitCommonDir() {
    // Given:
    given(gitHelper.getCommonDirectory()).willReturn(GIT_COMMON_DIR);

    // When:
    Path gitHooksDir = applyGitHooksHelper.findGitHooksDirectory();

    // Then:
    assertThat(gitHooksDir).isEqualTo(GIT_COMMON_DIR.resolve("hooks"));
  }

  @Test
  void createGitHookDirectories_ShouldCreateDirectories() {
    // When:
    applyGitHooksHelper.createGitHookDirectories(GIT_HOOKS_DIR);

    // Then:
    verify(filesHelper).createDirectories(GIT_HOOKS_DIR.resolve("git-hooks"));
  }

  @Test
  void getGitHookTemplateData_ShouldReturnHookTemplateData() {
    // Given:
    Instant createdAt = Instant.now();
    ZoneId timeZone = ZoneId.of("America/Los_Angeles");
    String hookScript = "echo \"Hello, World!\"";

    given(clock.instant()).willReturn(createdAt);
    given(clock.getZone()).willReturn(timeZone);

    // When:
    GitHookTemplateData data = applyGitHooksHelper.getGitHookTemplateData(hookScript);

    // Then:
    GitHookTemplateData expectedData =
        ImmutableGitHookTemplateData.builder()
            .setCreatedAt(ZonedDateTime.ofInstant(createdAt, timeZone))
            .setHookScript(hookScript)
            .build();

    assertThat(data).isEqualTo(expectedData);
  }

  @Test
  void createFreeMarkerConfiguration_WhenDebugLoggingEnabled_ShouldUseDebugExceptionHandler() {
    // Given:
    given(gradleHelper.isDebugLoggingEnabled()).willReturn(true);

    // When:
    Configuration freeMarkerConfiguration = applyGitHooksHelper.createFreeMarkerConfiguration();

    // Then:
    assertThat(freeMarkerConfiguration.getTemplateExceptionHandler())
        .as("Validating template exception handler is TemplateExceptionHandler.DEBUG_HANDLER")
        .isEqualTo(TemplateExceptionHandler.DEBUG_HANDLER);
  }

  @Test
  void createFreeMarkerConfiguration_WhenDebugLoggingDisabled_ShouldUseRethrowExceptionHandler() {
    // Given:
    given(gradleHelper.isDebugLoggingEnabled()).willReturn(false);

    // When:
    Configuration freeMarkerConfiguration = applyGitHooksHelper.createFreeMarkerConfiguration();

    // Then:
    assertThat(freeMarkerConfiguration.getTemplateExceptionHandler())
        .as("Validating template exception handler is TemplateExceptionHandler.RETHROW_HANDLER")
        .isEqualTo(TemplateExceptionHandler.RETHROW_HANDLER);
  }

  @Test
  void cleanEmptyHookScriptsDirectory_WhenDirectoryIsEmpty_ShouldDeleteDirectory() {
    // Given:
    given(filesHelper.exists(GIT_HOOK_SCRIPTS_DIR)).willReturn(true);
    given(filesHelper.isDirectoryEmpty(GIT_HOOK_SCRIPTS_DIR)).willReturn(true);

    // When:
    applyGitHooksHelper.cleanEmptyHookScriptsDirectory(GIT_HOOKS_DIR);

    // Then:
    verify(filesHelper).delete(GIT_HOOK_SCRIPTS_DIR);
  }

  @Nested
  final class GitHookTemplateTest {
    private final FilesHelper filesHelper;

    GitHookTemplateTest(@Mock(name = "filesHelper") FilesHelper filesHelper) {
      this.filesHelper = filesHelper;
    }

    @Test
    void writeTemplate_WhenDataIsPresent_ShouldWriteHookWithData() {
      // Not strictly a unit test, because we use a real FreeMarker configuration. We want to
      // check that FreeMarker can actually find and write to the template.

      // Given:
      Configuration freeMarkerConfiguration = applyGitHooksHelper.createFreeMarkerConfiguration();

      GitHookTemplate gitHookTemplate = new GitHookTemplate(filesHelper, freeMarkerConfiguration);

      Path outputFile = Paths.get("/echo.sh");

      String hookScript = "echo \"Hello, World\"";
      GitHookTemplateData data =
          ImmutableGitHookTemplateData.builder()
              .setCreatedAt(ZonedDateTime.now())
              .setHookScript(hookScript)
              .build();

      StringWriter writer = new StringWriter();
      given(filesHelper.createFileOutputStreamWriter(outputFile)).willReturn(writer);

      // When:
      gitHookTemplate.writeTemplate(outputFile, data);

      // Then:
      assertThat(writer.toString())
          .as("Validate hook output from template and data")
          .contains(hookScript);
    }
  }

  @Nested
  final class GitHookTemplateDataTest {
    @Test
    void GitHookTemplateData_ShouldTransformZonedDateTimeToString() {
      // Given:
      ZonedDateTime createdAt =
          ZonedDateTime.of(
              LocalDate.of(1990, Month.MARCH, 4),
              LocalTime.of(15, 45),
              ZoneId.of("America/Los_Angeles"));

      // When:
      GitHookTemplateData data =
          ImmutableGitHookTemplateData.builder()
              .setCreatedAt(createdAt)
              .setHookScript("echo \"Hello, World!\"")
              .build();

      // Then:
      assertThat(data.getFormattedCreatedAt()).isEqualTo("Mar 4, 1990 03:45 PM PST");
    }
  }
}
