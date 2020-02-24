package com.github.tjni.captainhook.dagger.modules;

import com.github.tjni.captainhook.dagger.annotations.ProjectScope;
import dagger.Module;
import dagger.Provides;
import org.gradle.api.Project;

@Module
public class ProjectModule {
  private final Project project;

  public ProjectModule(Project project) {
    this.project = project;
  }

  @Provides
  @ProjectScope
  public Project provideProject() {
    return project;
  }
}
