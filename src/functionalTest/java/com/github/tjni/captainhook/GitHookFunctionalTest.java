package com.github.tjni.captainhook;

import static com.github.tjni.captainhook.helpers.FileSnippets.APPLY_PLUGIN_SNIPPET;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tjni.captainhook.helpers.ExecHelper.ExecResult;
import com.github.tjni.captainhook.helpers.GitRepository;
import java.nio.file.Path;
import java.util.Objects;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GitHookFunctionalTest {
  @Test
  void applyGitHooks_ShouldAddPreCommitHook(@TempDir Path tempDir) {
    // Given:
    GitRepository repository = new GitRepository(tempDir);

    repository.commitBuild(
        APPLY_PLUGIN_SNIPPET,
        "                                         ",
        "captainHook {                            ",
        "  autoApplyGitHooks.set(false)           ",
        "  preCommit.set(\"echo 'Hello, World!'\")",
        "}                                        ");

    // When:
    BuildResult buildResult =
        GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withArguments("applyGitHooks", "-s")
            .withPluginClasspath()
            .build();

    ExecResult commitResult =
        repository
            .getExecHelper()
            .rawExec("git", "commit", "--allow-empty", "--message=new commit");

    // Then:
    assertThat(Objects.requireNonNull(buildResult.task(":applyGitHooks")).getOutcome())
        .isEqualTo(TaskOutcome.SUCCESS);

    // Unexpectedly, it appears that the pre-commit output is captured in standard error.
    assertThat(commitResult.getStderr()).contains("Hello, World!");
  }

  @Test
  void autoApplyGitHooks_ShouldAddPreCommitHook(@TempDir Path tempDir) {
    // Given:
    GitRepository repository = new GitRepository(tempDir);

    repository.commitBuild(
        APPLY_PLUGIN_SNIPPET,
        "                                         ",
        "captainHook {                            ",
        "  preCommit.set(\"echo 'Hello, World!'\")",
        "}                                        ");

    // When:
    GradleRunner.create()
        .withProjectDir(tempDir.toFile())
        .withArguments("tasks", "-s")
        .withPluginClasspath()
        .build();

    ExecResult commitResult =
        repository
            .getExecHelper()
            .rawExec("git", "commit", "--allow-empty", "--message=new commit");

    // Then:
    // Unexpectedly, it appears that the pre-commit output is captured in standard error.
    assertThat(commitResult.getStderr()).contains("Hello, World!");
  }
}
