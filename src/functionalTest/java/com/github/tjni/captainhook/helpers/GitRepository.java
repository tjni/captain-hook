package com.github.tjni.captainhook.helpers;

import static org.assertj.core.api.Assumptions.assumeThatCode;

import com.github.tjni.captainhook.helpers.GitHelper.GitStatus;
import java.nio.file.Path;
import java.util.List;
import one.util.streamex.IntStreamEx;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;

public class GitRepository {
  private final FilesHelper filesHelper;
  private final ExecHelper execHelper;
  private final GitHelper gitHelper;
  private final Path directory;

  public GitRepository(Path directory) {
    this.directory = directory;
    filesHelper = new FilesHelper();

    Project project = ProjectBuilder.builder().withProjectDir(directory.toFile()).build();
    execHelper = new ExecHelper(project);
    gitHelper = new GitHelper(execHelper);

    init();
  }

  public String git(String command, String... options) {
    return gitHelper.git(command, options);
  }

  public GitStatus status() {
    return gitHelper.status();
  }

  public void commitBuild(String... lines) {
    writeFile("settings.gradle.kts");
    writeFile("build.gradle.kts", lines);
    writeFile(".gitignore", ".gradle");
    git("add", "settings.gradle.kts", "build.gradle.kts", ".gitignore");
    git("commit", "--message=build files");
  }

  public void commitEmptyFiles(String filePathFormat, int numFiles) {
    List<String> filePaths =
        IntStreamEx.rangeClosed(1, numFiles)
            .mapToObj(i -> String.format(filePathFormat, i))
            .toList();

    filePaths.forEach(this::writeFile);

    git("add", filePaths.toArray(new String[0]));
    git("commit", "--message=empty files");
  }

  public ExecHelper getExecHelper() {
    return execHelper;
  }

  /**
   * Assumes that a file can be written as setup for a test.
   *
   * @param filePath path to the file to write, relative to the repository root
   * @param lines lines to write into the file
   */
  public void writeFile(String filePath, String... lines) {
    assumeThatCode(() -> writeLines(filePath, lines))
        .as("Writing %s", filePath)
        .doesNotThrowAnyException();
  }

  public void deleteFile(String filePath) {
    assumeThatCode(() -> filesHelper.delete(directory.resolve(filePath)))
        .as("Deleting %s", filePath)
        .doesNotThrowAnyException();
  }

  private void init() {
    assumeThatCode(() -> gitHelper.git("--version"))
        .as("Running \"git --version\"")
        .doesNotThrowAnyException();

    assumeThatCode(() -> gitHelper.git("init"))
        .as("Running \"git init\"")
        .doesNotThrowAnyException();
  }

  private void writeLines(String filePath, String... paddedLines) {
    filesHelper.write(directory.resolve(filePath), FileSnippets.mergeLines(paddedLines));
  }
}
