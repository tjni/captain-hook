package com.github.tjni.captainhook.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.github.tjni.captainhook.helpers.GitHelper.GitStatusLine;
import com.github.tjni.captainhook.helpers.StagingHelper.MergeStatus;
import com.github.tjni.captainhook.helpers.StagingHelper.Snapshot;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class StagingHelperTest {
  private static final Path GIT_DIR = Paths.get("/Workspace");
  private static final Path GIT_COMMON_DIR = GIT_DIR.resolve(".git");

  private final GitHelper gitHelper;
  private final FilesHelper filesHelper;
  private final StagingHelper stagingHelper;

  StagingHelperTest(
      @Mock(name = "gitHelper") GitHelper gitHelper,
      @Mock(name = "filesHelper") FilesHelper filesHelper,
      @Mock(name = "operatingSystemHelper") OperatingSystemHelper operatingSystemHelper) {
    this.gitHelper = gitHelper;
    this.filesHelper = filesHelper;
    stagingHelper = new StagingHelper(gitHelper, filesHelper, operatingSystemHelper);
  }

  @Test
  void saveSnapshot_ShouldSaveState() {
    // Given:
    given(gitHelper.getCommonDirectory()).willReturn(GIT_COMMON_DIR);

    List<Path> deletedFiles = Collections.singletonList(GIT_DIR.resolve("deleted.log"));
    given(gitHelper.lsFiles("--deleted")).willReturn(deletedFiles);

    String stashMessage = "message";
    StagingHelper spyStagingHelper = spy(stagingHelper);
    willReturn(true).given(spyStagingHelper).isGradleDirectoryIgnored();
    willReturn(stashMessage).given(spyStagingHelper).saveSnapshotStash();
    willReturn("stash{0}").given(spyStagingHelper).findStashName(stashMessage);

    List<Path> stagedFiles = Collections.singletonList(GIT_DIR.resolve("staged.log"));
    willReturn(stagedFiles).given(spyStagingHelper).getStagedFiles();

    // When:
    Snapshot snapshot = spyStagingHelper.saveSnapshot();

    // Then:
    assertThat(snapshot.getStagedFiles()).isEqualTo(stagedFiles);
    assertThat(snapshot.getStashMessage()).isEqualTo(stashMessage);
    assertThat(snapshot.getUnstagedPatchFile())
        .isEqualTo(GIT_COMMON_DIR.resolve(Snapshot.UNSTAGED_PATCH_FILE_NAME));
    assertThat(snapshot.getUntrackedPatchFile())
        .isEqualTo(GIT_COMMON_DIR.resolve(Snapshot.UNTRACKED_PATCH_FILE_NAME));

    InOrder inOrder = inOrder(spyStagingHelper, filesHelper, gitHelper);
    inOrder.verify(spyStagingHelper).isGradleDirectoryIgnored();
    inOrder.verify(spyStagingHelper).getStagedFiles();
    inOrder.verify(spyStagingHelper).saveSnapshotStash();
    inOrder.verify(filesHelper).delete(deletedFiles);
    inOrder.verify(gitHelper).git(eq("diff"), any());
    inOrder.verify(gitHelper).git(eq("show"), any());
  }

  @Test
  void isGradleDirectoryIgnored_ShouldDetectIgnoredGradleDirectory() {
    // Given:
    GitStatusLine statusLine = ImmutableGitStatusLine.of("!! .gradle/");

    given(gitHelper.status("--ignored"))
        .willReturn(ImmutableGitStatus.of(Collections.singletonList(statusLine)));

    // When:
    boolean isGradleDirectoryIgnored = stagingHelper.isGradleDirectoryIgnored();

    // Then:
    assertThat(isGradleDirectoryIgnored).isTrue();
  }

  @Test
  void saveSnapshotStash_ShouldMaintainMergeStatusAndSaveStash() {
    // Given:
    StagingHelper spyStagingHelper = spy(stagingHelper);

    MergeStatus mergeStatus =
        ImmutableMergeStatus.builder().setMergeHead("mock merge head file content").build();

    willReturn(mergeStatus).given(spyStagingHelper).saveMergeStatus();
    willDoNothing().given(spyStagingHelper).restoreMergeStatus(any());

    // When:
    String actualStashMessage = spyStagingHelper.saveSnapshotStash();

    // Then:
    assertThat(actualStashMessage).isEqualTo(StagingHelper.BACKUP_STASH_MESSAGE);

    InOrder inOrder = inOrder(spyStagingHelper, gitHelper);

    inOrder.verify(spyStagingHelper).saveMergeStatus();
    inOrder.verify(gitHelper).stash(eq("push"), any());
    inOrder.verify(spyStagingHelper).restoreMergeStatus(mergeStatus);
  }

  @Test
  void saveMergeStatus_ShouldSaveMergeStatus() {
    // Given:
    given(gitHelper.getCommonDirectory()).willReturn(GIT_COMMON_DIR);

    Path mergeHeadFile = GIT_COMMON_DIR.resolve("MERGE_HEAD");
    Path mergeModeFile = GIT_COMMON_DIR.resolve("MERGE_MODE");
    Path mergeMsgFile = GIT_COMMON_DIR.resolve("MERGE_MSG");

    given(filesHelper.exists(mergeHeadFile)).willReturn(true);
    given(filesHelper.exists(mergeModeFile)).willReturn(true);
    given(filesHelper.exists(mergeMsgFile)).willReturn(true);

    String mergeHeadContent = "mock merge head file content";
    String mergeModeContent = "mock merge mode file content";
    String mergeMsgContent = "mock merge msg file content";

    given(filesHelper.toString(mergeHeadFile)).willReturn(mergeHeadContent);
    given(filesHelper.toString(mergeModeFile)).willReturn(mergeModeContent);
    given(filesHelper.toString(mergeMsgFile)).willReturn(mergeMsgContent);

    // When:
    MergeStatus mergeStatus = stagingHelper.saveMergeStatus();

    // Then:
    assertThat(mergeStatus.getMergeHead()).contains(mergeHeadContent);
    assertThat(mergeStatus.getMergeMode()).contains(mergeModeContent);
    assertThat(mergeStatus.getMergeMsg()).contains(mergeMsgContent);
  }

  @Test
  void restoreMergeStatus_ShouldRestoreMergeStatus() {
    // Given:
    given(gitHelper.getCommonDirectory()).willReturn(GIT_COMMON_DIR);

    String mergeHeadContent = "mock merge head file content";
    String mergeModeContent = "mock merge mode file content";
    String mergeMsgContent = "mock merge msg file content";

    MergeStatus mergeStatus =
        ImmutableMergeStatus.builder()
            .setMergeHead(mergeHeadContent)
            .setMergeMode(mergeModeContent)
            .setMergeMsg(mergeMsgContent)
            .build();

    // When:
    stagingHelper.restoreMergeStatus(mergeStatus);

    // Then:
    Path mergeHeadFile = GIT_COMMON_DIR.resolve("MERGE_HEAD");
    Path mergeModeFile = GIT_COMMON_DIR.resolve("MERGE_MODE");
    Path mergeMsgFile = GIT_COMMON_DIR.resolve("MERGE_MSG");

    verify(filesHelper).write(mergeHeadFile, mergeHeadContent);
    verify(filesHelper).write(mergeModeFile, mergeModeContent);
    verify(filesHelper).write(mergeMsgFile, mergeMsgContent);
  }

  @Test
  void findStashName_WhenStashExists_ShouldReturnStash() {
    // Given:
    String stashList = StreamEx.of("message 1", "message 2").joining("\n");
    given(gitHelper.stash("list")).willReturn(stashList);

    // When:
    String stashName = stagingHelper.findStashName("message 2");

    // Then:
    assertThat(stashName).isEqualTo("stash@{1}");
  }

  @Test
  void findStashName_WhenStashDoesNotExist_ShouldThrowException() {
    // Given:
    given(gitHelper.stash("list")).willReturn("");

    // When:
    Throwable exception = catchThrowable(() -> stagingHelper.findStashName("message"));

    // Then:
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }
}
