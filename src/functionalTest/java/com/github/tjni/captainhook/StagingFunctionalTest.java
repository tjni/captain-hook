package com.github.tjni.captainhook;

import static com.github.tjni.captainhook.helpers.FileSnippets.APPLY_PLUGIN_SNIPPET;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tjni.captainhook.helpers.GitHelper.GitStatus;
import com.github.tjni.captainhook.helpers.GitHelper.GitStatusLine;
import com.github.tjni.captainhook.helpers.GitRepository;
import java.nio.file.Path;
import java.util.Objects;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class StagingFunctionalTest {
  @Test
  void staging_WhenStagingHasChanges_ShouldRunTasksInOrder(@TempDir Path tempDir) {
    // Given:
    GitRepository repository = new GitRepository(tempDir);

    repository.commitBuild(
        APPLY_PLUGIN_SNIPPET,
        "                                         ",
        "tasks.register(\"greeting\") {           ",
        "  doLast {                               ",
        "    println(\"Hello, World!\")           ",
        "  }                                      ",
        "}                                        ",
        "tasks.register(\"goodbye\") {            ",
        "  doLast {                               ",
        "    println(\"Goodbye, World!\")         ",
        "  }                                      ",
        "}                                        ");

    repository.writeFile("file1.txt");
    repository.git("add", ".");

    // When:
    BuildResult buildResult =
        GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withArguments("staging", "greeting", "goodbye", "-s")
            .withPluginClasspath()
            .build();

    // Then:
    assertThat(Objects.requireNonNull(buildResult.task(":greeting")).getOutcome())
        .isEqualTo(TaskOutcome.SUCCESS);

    assertThat(Objects.requireNonNull(buildResult.task(":goodbye")).getOutcome())
        .isEqualTo(TaskOutcome.SUCCESS);

    assertThat(Objects.requireNonNull(buildResult.task(":staging")).getOutcome())
        .isEqualTo(TaskOutcome.UP_TO_DATE);

    assertThat(buildResult.getTasks())
        .extracting(BuildTask::getPath)
        .containsSubsequence(":staging", ":greeting", ":goodbye");
  }

  @Test
  void staging_WhenStagingIsEmpty_ShouldSkipAllTasks(@TempDir Path tempDir) {
    // Given:
    GitRepository repository = new GitRepository(tempDir);

    repository.commitBuild(
        APPLY_PLUGIN_SNIPPET,
        "                                         ",
        "tasks.register(\"greeting\") {           ",
        "  doLast {                               ",
        "    println(\"Hello, World!\")           ",
        "  }                                      ",
        "}                                        ",
        "tasks.register(\"goodbye\") {            ",
        "  doLast {                               ",
        "    println(\"Goodbye, World!\")         ",
        "  }                                      ",
        "}                                        ");

    // When:
    BuildResult buildResult =
        GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withArguments("staging", "greeting", "goodbye", "-s")
            .withPluginClasspath()
            .build();

    // Then:
    assertThat(Objects.requireNonNull(buildResult.task(":greeting")).getOutcome())
        .isEqualTo(TaskOutcome.SKIPPED);

    assertThat(Objects.requireNonNull(buildResult.task(":goodbye")).getOutcome())
        .isEqualTo(TaskOutcome.SKIPPED);

    assertThat(Objects.requireNonNull(buildResult.task(":staging")).getOutcome())
        .isEqualTo(TaskOutcome.SKIPPED);

    assertThat(buildResult.getTasks())
        .extracting(BuildTask::getPath)
        .containsSubsequence(":staging", ":greeting", ":goodbye");
  }

  @Test
  void staging_WhenStagingHasChanges_ShouldPreserveDeletions(@TempDir Path tempDir) {
    // Given:
    GitRepository repository = new GitRepository(tempDir);

    repository.commitBuild(APPLY_PLUGIN_SNIPPET);
    repository.commitEmptyFiles("file%d.txt", 2);

    repository.git("rm", "file1.txt");
    repository.deleteFile("file2.txt");

    // Modifying a file ensures that the staging logic will run. If only
    // deletions are staged, the staging logic will be skipped.
    repository.writeFile("file3.txt", "modified");
    repository.git("add", "file3.txt");

    // When:
    GradleRunner.create()
        .withProjectDir(tempDir.toFile())
        .withArguments("staging", "-s")
        .withPluginClasspath()
        .build();

    // Then:
    GitStatus status = repository.status();

    assertThat(status.findByFilePath("file1.txt"))
        .map(GitStatusLine::getIndexStatus)
        .as("file1.txt index status")
        .hasValue('D');

    assertThat(status.findByFilePath("file2.txt"))
        .map(GitStatusLine::getWorkingTreeStatus)
        .as("file1.txt working tree status")
        .hasValue('D');
  }

  @Test
  void staging_WhenStagingTaskFails_ShouldRestoreState(@TempDir Path tempDir) {
    // Given:
    GitRepository repository = new GitRepository(tempDir);

    repository.commitBuild(
        APPLY_PLUGIN_SNIPPET,
        "                                         ",
        "tasks.register(\"fail\") {               ",
        "  doLast {                               ",
        "    throw RuntimeException()             ",
        "  }                                      ",
        "}                                        ");

    repository.commitEmptyFiles("file%d.txt", 4);

    repository.writeFile("file1.txt", "modified");
    repository.git("add", "file1.txt");
    repository.writeFile("file2.txt", "modified");
    repository.git("rm", "file3.txt");
    repository.deleteFile("file4.txt");
    repository.writeFile("file5.txt", "not a rename of file3.txt");
    repository.git("add", "file5.txt");
    repository.writeFile("file6.txt");

    // When:
    BuildResult buildResult =
        GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withArguments("staging", "fail", "-s")
            .withPluginClasspath()
            .buildAndFail();

    // Then:
    assertThat(Objects.requireNonNull(buildResult.task(":fail")).getOutcome())
        .isEqualTo(TaskOutcome.FAILED);

    GitStatus status = repository.status();

    assertThat(status.findByFilePath("file1.txt"))
        .map(GitStatusLine::getIndexStatus)
        .as("file1.txt index status")
        .hasValue('M');

    assertThat(status.findByFilePath("file2.txt"))
        .map(GitStatusLine::getWorkingTreeStatus)
        .as("file2.txt working tree status")
        .hasValue('M');

    assertThat(status.findByFilePath("file3.txt"))
        .map(GitStatusLine::getIndexStatus)
        .as("file3.txt index status")
        .hasValue('D');

    assertThat(status.findByFilePath("file4.txt"))
        .map(GitStatusLine::getWorkingTreeStatus)
        .as("file4.txt working tree status")
        .hasValue('D');

    assertThat(status.findByFilePath("file5.txt"))
        .map(GitStatusLine::getIndexStatus)
        .as("file5.txt index status")
        .hasValue('A');

    assertThat(status.findByFilePath("file6.txt"))
        .map(GitStatusLine::getWorkingTreeStatus)
        .as("file6.txt working tree status")
        .hasValue('?');

    assertSnapshotDeleted(repository);
  }

  @Test
  void staging_WhenStagingTaskModifiesFiles_ShouldOnlyAddModifiedStagedFiles(
      @TempDir Path tempDir) {
    // Given:
    GitRepository repository = new GitRepository(tempDir);

    repository.commitBuild(
        APPLY_PLUGIN_SNIPPET,
        "                                                ",
        "tasks.register(\"modify\") {                    ",
        "  doLast {                                      ",
        "    file(\"file1.txt\").writeText(\"final\")    ",
        "    file(\"file2.txt\").writeText(\"final\")    ",
        "  }                                             ",
        "}                                               ");

    repository.commitEmptyFiles("file%d.txt", 3);

    repository.writeFile("file1.txt", "intermediate");
    repository.git("add", "file1.txt");
    repository.writeFile("file2.txt", "intermediate");
    repository.writeFile("file3.txt", "final");

    // When:
    BuildResult buildResult =
        GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withArguments("staging", "modify", "-s")
            .withPluginClasspath()
            .build();

    // Then:
    assertThat(Objects.requireNonNull(buildResult.task(":modify")).getOutcome())
        .isEqualTo(TaskOutcome.SUCCESS);

    GitStatus status = repository.status();

    assertThat(status.findByFilePath("file1.txt"))
        .map(GitStatusLine::getIndexStatus)
        .as("file1.txt index status")
        .hasValue('M');

    assertThat(status.findByFilePath("file1.txt"))
        .map(GitStatusLine::getWorkingTreeStatus)
        .as("file1.txt working tree status")
        .hasValue(' ');

    assertThat(status.findByFilePath("file2.txt"))
        .map(GitStatusLine::getIndexStatus)
        .as("file2.txt index status")
        .hasValue(' ');

    assertThat(status.findByFilePath("file2.txt"))
        .map(GitStatusLine::getWorkingTreeStatus)
        .as("file2.txt working tree status")
        .hasValue('M');

    assertThat(status.findByFilePath("file3.txt"))
        .map(GitStatusLine::getIndexStatus)
        .as("file3.txt index status")
        .hasValue(' ');

    assertThat(status.findByFilePath("file3.txt"))
        .map(GitStatusLine::getWorkingTreeStatus)
        .as("file3.txt working tree status")
        .hasValue('M');

    assertSnapshotDeleted(repository);
  }

  private static void assertSnapshotDeleted(GitRepository repository) {
    assertThat(repository.git("stash", "list")).as("git stash list").isEmpty();
  }
}
