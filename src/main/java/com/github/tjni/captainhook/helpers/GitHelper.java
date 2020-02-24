package com.github.tjni.captainhook.helpers;

import com.github.tjni.captainhook.annotations.ImmutableStyle;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import one.util.streamex.StreamEx;
import org.immutables.value.Value;

/**
 * Helper for running common Git commands.
 *
 * <p>This implementation caches responses from Git that do not change for a single repository.
 */
@Singleton
public class GitHelper {
  private final RepositoryCache repositoryCache =
      ImmutableRepositoryCache.builder()
          .setCommonDirectorySupplier(this::getCommonDirectoryFromGit)
          .setTopLevelDirectorySupplier(this::getTopLevelDirectoryFromGit)
          .build();

  private final ExecHelper execHelper;

  @Inject
  public GitHelper(ExecHelper execHelper) {
    this.execHelper = execHelper;
  }

  public String git(String command, String... options) {
    String[] args = StreamEx.of(command).append(options).toArray(String.class);
    return execHelper.exec("git", args);
  }

  public String stash(String subCommand, String... options) {
    String[] args = StreamEx.of(subCommand).append(options).toArray(String.class);
    return git("stash", args);
  }

  /**
   * Returns the absolute path to the .git directory for the repository.
   *
   * @return the absolute path to the .git directory for the repository
   * @see <a
   *     href="https://git-scm.com/docs/git-rev-parse#Documentation/git-rev-parse.txt---show-toplevel">--show-toplevel</a>
   * @see <a
   *     href="https://git-scm.com/docs/git-rev-parse#Documentation/git-rev-parse.txt---git-common-dir">--git-common-dir</a>
   */
  public Path getCommonDirectory() {
    return repositoryCache.getCommonDirectory();
  }

  private Path getCommonDirectoryFromGit() {
    String relativeGitCommonDir = git("rev-parse", "--git-common-dir");
    return getTopLevelDirectory().resolve(relativeGitCommonDir);
  }

  /**
   * Returns the absolute path to the top-level directory for the repository.
   *
   * @return the absolute path to the top-level directory for the repository
   * @see <a
   *     href="https://git-scm.com/docs/git-rev-parse#Documentation/git-rev-parse.txt---show-toplevel">--show-toplevel</a>
   */
  public Path getTopLevelDirectory() {
    return repositoryCache.getTopLevelDirectory();
  }

  private Path getTopLevelDirectoryFromGit() {
    return Paths.get(git("rev-parse", "--show-toplevel"));
  }

  /**
   * Returns the output of {@code git status --porcelain}.
   *
   * @param options options to {@code git status}
   * @return the output of {@code git status --porcelain}
   */
  GitStatus status(String... options) {
    String[] args = StreamEx.of("--porcelain").append(options).toArray(String.class);
    String output = git("status", args);
    List<GitStatusLine> statusLines;
    if (output.isEmpty()) {
      statusLines = Collections.emptyList();
    } else {
      statusLines =
          StreamEx.of(output.split("\n")).<GitStatusLine>map(ImmutableGitStatusLine::of).toList();
    }
    return ImmutableGitStatus.of(statusLines);
  }

  /**
   * Returns absolute paths of files from {@code git ls-files}.
   *
   * @param options options to {@code git ls-files}
   * @return absolute paths of files from {@code git ls-files}
   */
  List<Path> lsFiles(String... options) {
    String output = git("ls-files", options);
    if (output.isEmpty()) {
      return Collections.emptyList();
    }
    return StreamEx.of(output.split("\n")).map(getTopLevelDirectory()::resolve).toImmutableList();
  }

  @Value.Immutable
  @ImmutableStyle
  abstract static class RepositoryCache {

    abstract Supplier<Path> commonDirectorySupplier();

    abstract Supplier<Path> topLevelDirectorySupplier();

    @Value.Lazy
    Path getCommonDirectory() {
      return commonDirectorySupplier().get();
    }

    @Value.Lazy
    Path getTopLevelDirectory() {
      return topLevelDirectorySupplier().get();
    }
  }

  @Value.Immutable(builder = false)
  @ImmutableStyle
  public abstract static class GitStatus {
    @Value.Parameter
    abstract List<GitStatusLine> getStatusLines();

    public Optional<GitStatusLine> findByFilePath(String filePath) {
      return StreamEx.of(getStatusLines())
          .findFirst(statusLine -> statusLine.getRelativePath().toString().equals(filePath));
    }

    public boolean isEmpty() {
      return getStatusLines().isEmpty();
    }
  }

  @Value.Immutable(builder = false)
  @ImmutableStyle
  public abstract static class GitStatusLine {
    @Value.Auxiliary
    @Value.Parameter
    abstract String getGitStatusShortOutputLine();

    @Value.Derived
    public char getIndexStatus() {
      return getGitStatusShortOutputLine().charAt(0);
    }

    @Value.Derived
    public char getWorkingTreeStatus() {
      return getGitStatusShortOutputLine().charAt(1);
    }

    @Value.Derived
    public Path getRelativePath() {
      return Paths.get(getGitStatusShortOutputLine().substring(3));
    }

    public boolean isIgnored() {
      return getIndexStatus() == '!' && getWorkingTreeStatus() == '!';
    }
  }
}
