package com.github.tjni.captainhook.tasks;

import static org.mockito.Mockito.verify;

import com.github.tjni.captainhook.dagger.components.PluginComponent;
import com.github.tjni.captainhook.dagger.components.TestPluginComponent;
import com.github.tjni.captainhook.helpers.ApplyGitHooksHelper;
import com.github.tjni.captainhook.helpers.GitHook;
import java.util.Collections;
import java.util.Map;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

final class ApplyGitHooksTaskTest {
  private final ApplyGitHooksHelper applyGitHooksHelper;
  private final ApplyGitHooksTask applyGitHooksTask;

  ApplyGitHooksTaskTest() {
    PluginComponent component = new TestPluginComponent();
    applyGitHooksHelper = component.getApplyGitHooksHelper();
    applyGitHooksTask =
        ProjectBuilder.builder()
            .build()
            .getTasks()
            .create("applyGitHooks", ApplyGitHooksTask.class, component);
  }

  @Test
  void apply_ShouldDelegateToApplyGitHooksHelper() {
    // Given:
    String preCommitScript = "echo \"pre-commit\"";

    // noinspection UnstableApiUsage
    applyGitHooksTask.getGitHooks().put(GitHook.PRE_COMMIT, preCommitScript);

    // When:
    applyGitHooksTask.apply();

    // Then:
    Map<GitHook, String> expectedGitHooks =
        Collections.singletonMap(GitHook.PRE_COMMIT, preCommitScript);

    verify(applyGitHooksHelper).apply(expectedGitHooks);
  }
}
