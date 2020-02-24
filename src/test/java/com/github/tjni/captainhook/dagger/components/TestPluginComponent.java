package com.github.tjni.captainhook.dagger.components;

import static org.mockito.Mockito.mock;

import com.github.tjni.captainhook.helpers.ApplyGitHooksHelper;
import com.github.tjni.captainhook.helpers.FilesHelper;
import com.github.tjni.captainhook.helpers.GitHelper;
import com.github.tjni.captainhook.helpers.GradleHelper;
import com.github.tjni.captainhook.helpers.StagingHelper;
import java.time.Clock;

public class TestPluginComponent implements PluginComponent {
  private final FilesHelper filesHelper = mock(FilesHelper.class, "filesHelper");
  private final GitHelper gitHelper = mock(GitHelper.class, "gitHelper");
  private final GradleHelper gradleHelper = mock(GradleHelper.class, "gradleHelper");
  private final ApplyGitHooksHelper applyGitHooksHelper =
      mock(ApplyGitHooksHelper.class, "applyGitHooksHelper");
  private final StagingHelper stagingHelper = mock(StagingHelper.class, "stagingHelper");
  private final Clock clock = mock(Clock.class, "clock");

  @Override
  public FilesHelper getFilesHelper() {
    return filesHelper;
  }

  @Override
  public GitHelper getGitHelper() {
    return gitHelper;
  }

  @Override
  public GradleHelper getGradleHelper() {
    return gradleHelper;
  }

  @Override
  public ApplyGitHooksHelper getApplyGitHooksHelper() {
    return applyGitHooksHelper;
  }

  @Override
  public StagingHelper getStagingHelper() {
    return stagingHelper;
  }

  @Override
  public Clock getClock() {
    return clock;
  }
}
