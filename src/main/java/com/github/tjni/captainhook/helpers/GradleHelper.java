package com.github.tjni.captainhook.helpers;

import com.github.tjni.captainhook.CaptainHookExtension;
import com.github.tjni.captainhook.dagger.annotations.ProjectScope;
import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.logging.LogLevel;

/** Helper for Gradle operations. */
@ProjectScope
public class GradleHelper {
  private final Project project;
  private final CaptainHookExtension extension;

  @Inject
  public GradleHelper(Project project) {
    this.project = project;
    extension = project.getExtensions().getByType(CaptainHookExtension.class);
  }

  /**
   * Returns the Gradle project.
   *
   * @return the Gradle project
   */
  public Project getProject() {
    return project;
  }

  /**
   * Returns the extension used to configure Captain Hook.
   *
   * @return the extension used to configure Captain Hook
   */
  public CaptainHookExtension getExtension() {
    return extension;
  }

  /**
   * Returns whether debug logging is enabled in the current Gradle execution.
   *
   * @return whether debug logging is enabled in the current Gradle execution
   */
  public boolean isDebugLoggingEnabled() {
    return project.getGradle().getStartParameter().getLogLevel() == LogLevel.DEBUG;
  }
}
