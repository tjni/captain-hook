package com.github.tjni.captainhook.dagger.components;

import com.github.tjni.captainhook.dagger.annotations.ProjectScope;
import com.github.tjni.captainhook.dagger.modules.ProjectModule;
import com.github.tjni.captainhook.dagger.modules.SingletonModule;
import com.github.tjni.captainhook.helpers.ApplyGitHooksHelper;
import com.github.tjni.captainhook.helpers.FilesHelper;
import com.github.tjni.captainhook.helpers.GitHelper;
import com.github.tjni.captainhook.helpers.GradleHelper;
import com.github.tjni.captainhook.helpers.StagingHelper;
import dagger.Component;
import java.time.Clock;
import javax.inject.Singleton;

@Component(modules = {ProjectModule.class, SingletonModule.class})
@ProjectScope
@Singleton
public interface PluginComponent {
  FilesHelper getFilesHelper();

  GitHelper getGitHelper();

  GradleHelper getGradleHelper();

  ApplyGitHooksHelper getApplyGitHooksHelper();

  StagingHelper getStagingHelper();

  Clock getClock();
}
