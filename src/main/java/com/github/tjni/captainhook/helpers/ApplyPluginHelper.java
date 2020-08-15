package com.github.tjni.captainhook.helpers;

import com.github.tjni.captainhook.CaptainHookExtension;
import com.github.tjni.captainhook.dagger.components.PluginComponent;
import com.github.tjni.captainhook.helpers.StagingHelper.Snapshot;
import com.github.tjni.captainhook.tasks.ApplyGitHooksTask;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import one.util.streamex.EntryStream;
import org.gradle.BuildResult;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;

public class ApplyPluginHelper {
  private static final Logger LOG = Logging.getLogger(ApplyPluginHelper.class);
  private static final String STAGING_TASK_NAME = "staging";

  private final PluginComponent component;
  private final Project project;
  private final TaskContainer tasks;
  private final CaptainHookExtension extension;

  public ApplyPluginHelper(PluginComponent component) {
    this.component = component;
    this.project = component.getGradleHelper().getProject();
    this.tasks = component.getGradleHelper().getProject().getTasks();
    this.extension = component.getGradleHelper().getExtension();
  }

  public void createApplyGitHooksTask() {
    tasks
        .register("applyGitHooks", ApplyGitHooksTask.class, component)
        .configure(this::configureApplyGitHooksTask);
  }

  public void configureStaging() {
    List<String> startTaskNames = project.getGradle().getStartParameter().getTaskNames();
    if (!startTaskNames.isEmpty() && startTaskNames.get(0).equals(STAGING_TASK_NAME)) {
      createStagingRootTask();
      if (component.getStagingHelper().isStagingEmpty()) {
        LOG.warn("Not running any tasks because the staging area is empty.");
        project.getGradle().getStartParameter().setExcludedTaskNames(startTaskNames);
      } else {
        Snapshot snapshot = component.getStagingHelper().saveSnapshot();
        project.getExtensions().getExtraProperties().set("staging", snapshot.getStagedFiles());
        project
            .getGradle()
            .buildFinished(buildResult -> handleStagingBuildFinished(buildResult, snapshot));
      }
    }
  }

  public void maybeAutoApplyGitHooks() {
    project.afterEvaluate(p -> doMaybeAutoApplyGitHooks());
  }

  private void doMaybeAutoApplyGitHooks() {
    if (extension.getAutoApplyGitHooks().get()) {
      Map<GitHook, String> gitHooks =
          EntryStream.of(getGitHooks()).mapValues(Provider::get).toMap();
      component.getApplyGitHooksHelper().apply(gitHooks);
    }
  }

  private Map<GitHook, Provider<String>> getGitHooks() {
    Map<GitHook, Provider<String>> gitHooks = new HashMap<>();
    if (extension.getApplypatchMsg().isPresent()) {
      gitHooks.put(GitHook.APPLYPATCH_MSG, extension.getApplypatchMsg());
    }
    if (extension.getPreApplypatch().isPresent()) {
      gitHooks.put(GitHook.PRE_APPLYPATCH, extension.getPreApplypatch());
    }
    if (extension.getPostApplypatch().isPresent()) {
      gitHooks.put(GitHook.POST_APPLYPATCH, extension.getPostApplypatch());
    }
    if (extension.getPreCommit().isPresent()) {
      gitHooks.put(GitHook.PRE_COMMIT, extension.getPreCommit());
    }
    if (extension.getPreMergeCommit().isPresent()) {
      gitHooks.put(GitHook.PRE_MERGE_COMMIT, extension.getPreMergeCommit());
    }
    if (extension.getPrepareCommitMsg().isPresent()) {
      gitHooks.put(GitHook.PREPARE_COMMIT_MSG, extension.getPrepareCommitMsg());
    }
    if (extension.getCommitMsg().isPresent()) {
      gitHooks.put(GitHook.COMMIT_MSG, extension.getCommitMsg());
    }
    if (extension.getPostCommit().isPresent()) {
      gitHooks.put(GitHook.POST_COMMIT, extension.getPostCommit());
    }
    if (extension.getPreRebase().isPresent()) {
      gitHooks.put(GitHook.PRE_REBASE, extension.getPreRebase());
    }
    if (extension.getPostCheckout().isPresent()) {
      gitHooks.put(GitHook.POST_CHECKOUT, extension.getPostCheckout());
    }
    if (extension.getPostMerge().isPresent()) {
      gitHooks.put(GitHook.POST_MERGE, extension.getPostMerge());
    }
    if (extension.getPrePush().isPresent()) {
      gitHooks.put(GitHook.PRE_PUSH, extension.getPrePush());
    }
    if (extension.getPreReceive().isPresent()) {
      gitHooks.put(GitHook.PRE_RECEIVE, extension.getPreReceive());
    }
    if (extension.getUpdate().isPresent()) {
      gitHooks.put(GitHook.UPDATE, extension.getUpdate());
    }
    if (extension.getPostReceive().isPresent()) {
      gitHooks.put(GitHook.POST_RECEIVE, extension.getPostReceive());
    }
    if (extension.getPostUpdate().isPresent()) {
      gitHooks.put(GitHook.POST_UPDATE, extension.getPostUpdate());
    }
    if (extension.getPushToCheckout().isPresent()) {
      gitHooks.put(GitHook.PUSH_TO_CHECKOUT, extension.getPushToCheckout());
    }
    if (extension.getPreAutoGc().isPresent()) {
      gitHooks.put(GitHook.PRE_AUTO_GC, extension.getPreAutoGc());
    }
    if (extension.getPostRewrite().isPresent()) {
      gitHooks.put(GitHook.POST_REWRITE, extension.getPostRewrite());
    }
    if (extension.getSendemailValidate().isPresent()) {
      gitHooks.put(GitHook.SENDEMAIL_VALIDATE, extension.getSendemailValidate());
    }
    return gitHooks;
  }

  private void configureApplyGitHooksTask(ApplyGitHooksTask task) {
    // noinspection UnstableApiUsage
    getGitHooks().forEach(task.getGitHooks()::put);
  }

  private void createStagingRootTask() {
    tasks.register(STAGING_TASK_NAME);
  }

  private void handleStagingBuildFinished(BuildResult buildResult, Snapshot snapshot) {
    if (buildResult.getFailure() == null) {
      try {
        component.getStagingHelper().applyModifications(snapshot);
      } catch (Exception e) {
        component.getStagingHelper().restoreSnapshot(snapshot);
        throw e;
      }
    } else {
      component.getStagingHelper().restoreSnapshot(snapshot);
    }

    component.getStagingHelper().deleteSnapshot(snapshot);
  }
}
