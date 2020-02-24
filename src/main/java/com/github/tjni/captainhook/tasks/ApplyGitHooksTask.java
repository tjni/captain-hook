package com.github.tjni.captainhook.tasks;

import com.github.tjni.captainhook.dagger.components.PluginComponent;
import com.github.tjni.captainhook.helpers.*;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

/**
 * Adds and removes Git hooks based on configuration.
 *
 * <p>Please see the documentation of {@link ApplyGitHooksHelper} to see in more detail what this
 * entails.
 *
 * @see ApplyGitHooksHelper
 */
public class ApplyGitHooksTask extends DefaultTask {
  private final ApplyGitHooksHelper applyGitHooksHelper;

  @SuppressWarnings("UnstableApiUsage")
  private final MapProperty<GitHook, String> gitHooks;

  @Inject
  public ApplyGitHooksTask(PluginComponent component) {
    applyGitHooksHelper = component.getApplyGitHooksHelper();

    // noinspection UnstableApiUsage
    gitHooks = getProject().getObjects().mapProperty(GitHook.class, String.class).empty();
  }

  @TaskAction
  public void apply() {
    applyGitHooksHelper.apply(gitHooks.get());
  }

  @Input
  @SuppressWarnings("UnstableApiUsage")
  public MapProperty<GitHook, String> getGitHooks() {
    return gitHooks;
  }
}
