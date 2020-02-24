package com.github.tjni.captainhook.helpers;

import com.github.tjni.captainhook.annotations.ImmutableStyle;
import com.github.tjni.captainhook.annotations.VisibleForTesting;
import com.github.tjni.captainhook.helpers.ExecHelper.ExecException;
import com.github.tjni.captainhook.helpers.GitHelper.GitStatusLine;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import one.util.streamex.StreamEx;
import org.gradle.api.GradleException;
import org.immutables.value.Value;

/**
 * Helper to safely apply modifications to files staged in Git.
 *
 * <p>Heavily inspired by <a href="https://github.com/okonet/lint-staged">lint-staged</a>.
 */
@Singleton
public class StagingHelper {
  @VisibleForTesting static final String BACKUP_STASH_MESSAGE = "Captain Hook backup";

  private final GitHelper gitHelper;
  private final FilesHelper filesHelper;
  private final OperatingSystemHelper operatingSystemHelper;

  @Inject
  public StagingHelper(
      GitHelper gitHelper, FilesHelper filesHelper, OperatingSystemHelper operatingSystemHelper) {
    this.gitHelper = gitHelper;
    this.filesHelper = filesHelper;
    this.operatingSystemHelper = operatingSystemHelper;
  }

  /**
   * Returns whether the Git staging area is empty.
   *
   * @return {@code true} if the Git staging area is empty, and {@code false} otherwise.
   */
  public boolean isStagingEmpty() {
    return getStagedFiles().isEmpty();
  }

  /**
   * Saves the changes in the working directory.
   *
   * @return a snapshot of the changes in the working directory
   */
  public Snapshot saveSnapshot() {
    if (!isGradleDirectoryIgnored()) {
      // If .gradle is not ignored, restoring the snapshot will break because we
      // will try to write to locked files underneath .gradle.
      throw new GradleException("Please add the .gradle directory to the .gitignore file.");
    }

    Path gitCommonDir = gitHelper.getCommonDirectory();

    List<Path> stagedFiles = getStagedFiles();
    List<Path> deletedFiles = gitHelper.lsFiles("--deleted");

    String stashMessage = saveSnapshotStash();

    // Because `git stash` restores the HEAD commit, it brings back uncommitted
    // deleted files. We need to clear them before creating our snapshot.
    filesHelper.delete(deletedFiles);

    String stashName = findStashName(stashMessage);

    Path unstagedPatchFile = gitCommonDir.resolve(Snapshot.UNSTAGED_PATCH_FILE_NAME);
    gitHelper.git(
        "diff",
        "--binary",
        "--unified=0",
        "--no-color",
        "--no-ext-diff",
        "--patch",
        "--output=" + unstagedPatchFile,
        stashName,
        "-R");

    Path untrackedPatchFile = gitCommonDir.resolve(Snapshot.UNTRACKED_PATCH_FILE_NAME);
    gitHelper.git(
        "show",
        "--binary",
        "--unified=0",
        "--no-color",
        "--no-ext-diff",
        "--patch",
        "--format=%b",
        "--output=" + untrackedPatchFile,
        stashName + "^3");

    return ImmutableSnapshot.builder()
        .addAllStagedFiles(stagedFiles)
        .setStashMessage(stashMessage)
        .setUnstagedPatchFile(unstagedPatchFile)
        .setUntrackedPatchFile(untrackedPatchFile)
        .build();
  }

  public void applyModifications(Snapshot snapshot) {
    stageModifications(snapshot.getStagedFiles());

    if (gitHelper.status().isEmpty()) {
      return;
    }

    mergeSnapshot(snapshot);
  }

  public void restoreSnapshot(Snapshot snapshot) {
    MergeStatus mergeStatus = saveMergeStatus();
    gitHelper.git("reset", "--hard", "HEAD");

    String stashName = findStashName(snapshot.getStashMessage());
    gitHelper.stash("apply", "--quiet", "--index", stashName);

    restoreMergeStatus(mergeStatus);
  }

  public void deleteSnapshot(Snapshot snapshot) {
    filesHelper.deleteIfExists(snapshot.getUnstagedPatchFile());
    filesHelper.deleteIfExists(snapshot.getUntrackedPatchFile());
    String stashName = findStashName(snapshot.getStashMessage());
    gitHelper.stash("drop", "--quiet", stashName);
  }

  @VisibleForTesting
  boolean isGradleDirectoryIgnored() {
    return gitHelper
        .status("--ignored")
        .findByFilePath(".gradle")
        .map(GitStatusLine::isIgnored)
        .orElse(false);
  }

  /**
   * Returns the absolute paths of the files staged for commit.
   *
   * @return the absolute paths of the files staged for commit
   */
  List<Path> getStagedFiles() {
    String output = gitHelper.git("diff", "--staged", "--diff-filter=ACMR", "--name-only");

    if (output.isEmpty()) {
      return Collections.emptyList();
    }

    Path topLevelDir = gitHelper.getTopLevelDirectory();
    return StreamEx.of(output.split("\n")).map(topLevelDir::resolve).toImmutableList();
  }

  void mergeSnapshot(Snapshot snapshot) {
    if (!mergeUnstagedPatch(snapshot.getUnstagedPatchFile(), false)) {
      mergeUnstagedPatch(snapshot.getUnstagedPatchFile(), true);
    }
    mergeUntrackedPatch(snapshot.getUntrackedPatchFile());
  }

  boolean mergeUnstagedPatch(Path unstagedPatchFile, boolean is3way) {
    if (filesHelper.isFileEmpty(unstagedPatchFile)) {
      return true;
    }

    List<String> args =
        StreamEx.of("-v", "--whitespace=nowarn", "--recount", "--unidiff-zero").toList();
    if (is3way) {
      args.add("--3way");
    }
    args.add(unstagedPatchFile.toString());

    try {
      gitHelper.git("apply", args.toArray(new String[0]));
      return true;
    } catch (ExecException e) {
      return false;
    }
  }

  void mergeUntrackedPatch(Path untrackedPatchFile) {
    if (filesHelper.isTrimmedFileEmpty(untrackedPatchFile)) {
      return;
    }

    gitHelper.git(
        "apply",
        "-v",
        "--whitespace=nowarn",
        "--recount",
        "--unidiff-zero",
        untrackedPatchFile.toString());
  }

  @VisibleForTesting
  String saveSnapshotStash() {
    // If we are in the middle of a merge, save the merge status, because we
    // will run `git stash`, and that clears it.
    MergeStatus mergeStatus = saveMergeStatus();

    gitHelper.stash(
        "push", "--include-untracked", "--keep-index", "--message=" + BACKUP_STASH_MESSAGE);

    restoreMergeStatus(mergeStatus);

    return BACKUP_STASH_MESSAGE;
  }

  @VisibleForTesting
  MergeStatus saveMergeStatus() {
    Path gitCommonDir = gitHelper.getCommonDirectory();

    ImmutableMergeStatus.Builder builder = ImmutableMergeStatus.builder();

    Path mergeHeadFile = gitCommonDir.resolve("MERGE_HEAD");
    if (filesHelper.exists(mergeHeadFile)) {
      builder.setMergeHead(filesHelper.toString(mergeHeadFile));
    }

    Path mergeModeFile = gitCommonDir.resolve("MERGE_MODE");
    if (filesHelper.exists(mergeModeFile)) {
      builder.setMergeMode(filesHelper.toString(mergeModeFile));
    }

    Path mergeMsgFile = gitCommonDir.resolve("MERGE_MSG");
    if (filesHelper.exists(mergeMsgFile)) {
      builder.setMergeMsg(filesHelper.toString(mergeMsgFile));
    }

    return builder.build();
  }

  @VisibleForTesting
  void restoreMergeStatus(MergeStatus mergeStatus) {
    Path gitCommonDir = gitHelper.getCommonDirectory();

    mergeStatus
        .getMergeHead()
        .ifPresent(mergeHead -> filesHelper.write(gitCommonDir.resolve("MERGE_HEAD"), mergeHead));

    mergeStatus
        .getMergeMode()
        .ifPresent(mergeMode -> filesHelper.write(gitCommonDir.resolve("MERGE_MODE"), mergeMode));

    mergeStatus
        .getMergeMsg()
        .ifPresent(mergeMsg -> filesHelper.write(gitCommonDir.resolve("MERGE_MSG"), mergeMsg));
  }

  @VisibleForTesting
  String findStashName(String stashMessage) {
    String[] stashList = gitHelper.stash("list").split("\n");

    long stashIndex =
        StreamEx.of(stashList)
            .indexOf(message -> message.contains(stashMessage))
            .orElseThrow(
                () ->
                    new RuntimeException(
                        String.format("Did not find a stash with message \"%s\".", stashMessage)));

    return "stash@{" + stashIndex + "}";
  }

  @VisibleForTesting
  void stageModifications(List<Path> previouslyStagedFiles) {
    // We only add modified files that were originally staged be committed.
    // Applying this filter is necessary because the Gradle tasks that ran
    // might not be optimized to only run on the subset of staged files.
    if (!gitHelper.lsFiles("--modified").isEmpty()) {
      int maxCommandLineLen = operatingSystemHelper.getMaxCommandLength();
      int approxPathLen = StreamEx.of(previouslyStagedFiles).joining(" ").length();
      int numChunks =
          Math.min(divideCeil(approxPathLen, maxCommandLineLen), previouslyStagedFiles.size());
      StreamEx.ofSubLists(previouslyStagedFiles, previouslyStagedFiles.size() / numChunks)
          .map(paths -> StreamEx.of(paths).map(Path::toString).toArray(String.class))
          .forEach(paths -> gitHelper.git("add", paths));
    }
  }

  private static int divideCeil(int dividend, int divisor) {
    // See https://stackoverflow.com/a/21830188.
    return (dividend + divisor - 1) / divisor;
  }

  @Value.Immutable
  @ImmutableStyle
  public interface Snapshot {
    String UNSTAGED_PATCH_FILE_NAME = "captain-hook_unstaged.patch";
    String UNTRACKED_PATCH_FILE_NAME = "captain-hook_untracked.patch";

    List<Path> getStagedFiles();

    String getStashMessage();

    Path getUnstagedPatchFile();

    Path getUntrackedPatchFile();
  }

  @Value.Immutable
  @ImmutableStyle
  interface MergeStatus {
    Optional<String> getMergeHead();

    Optional<String> getMergeMode();

    Optional<String> getMergeMsg();
  }
}
