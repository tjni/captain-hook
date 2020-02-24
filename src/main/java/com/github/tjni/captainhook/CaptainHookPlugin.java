package com.github.tjni.captainhook;

import com.github.tjni.captainhook.annotations.VisibleForTesting;
import com.github.tjni.captainhook.dagger.components.DaggerPluginComponent;
import com.github.tjni.captainhook.dagger.components.PluginComponent;
import com.github.tjni.captainhook.dagger.modules.ProjectModule;
import com.github.tjni.captainhook.helpers.ApplyPluginHelper;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CaptainHookPlugin implements Plugin<Project> {
  @VisibleForTesting static final String PLUGIN_ID = "com.github.tjni.captain-hook";

  @Override
  public void apply(Project project) {
    if (project != project.getRootProject()) {
      throw new GradleException(String.format("Please apply '%s' to the root project.", PLUGIN_ID));
    }

    project.getExtensions().create(CaptainHookExtension.EXTENSION_NAME, CaptainHookExtension.class);

    PluginComponent component =
        DaggerPluginComponent.builder().projectModule(new ProjectModule(project)).build();

    apply(new ApplyPluginHelper(component));
  }

  @VisibleForTesting
  void apply(ApplyPluginHelper applyPluginHelper) {
    applyPluginHelper.createApplyGitHooksTask();
    applyPluginHelper.configureStaging();
    applyPluginHelper.maybeAutoApplyGitHooks();
  }
}
