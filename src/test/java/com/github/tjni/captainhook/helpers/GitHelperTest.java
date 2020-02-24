package com.github.tjni.captainhook.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.github.tjni.captainhook.helpers.GitHelper.GitStatusLine;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class GitHelperTest {
  private static final Path GIT_TOP_LEVEL_DIR = Paths.get("/Workspace");

  private final ExecHelper execHelper;
  private final GitHelper gitHelper;

  GitHelperTest(@Mock(name = "execHelper") ExecHelper execHelper) {
    this.execHelper = execHelper;
    gitHelper = new GitHelper(execHelper);
  }

  @Test
  void getCommonDirectory_ShouldCombineTopLevelWithCommonDirectory() {
    // Given:
    given(execHelper.exec("git", "rev-parse", "--show-toplevel"))
        .willReturn(GIT_TOP_LEVEL_DIR.toString());
    given(execHelper.exec("git", "rev-parse", "--git-common-dir")).willReturn(".git");

    // When:
    Path gitCommonDir = gitHelper.getCommonDirectory();

    // Then:
    assertThat(gitCommonDir).isEqualTo(GIT_TOP_LEVEL_DIR.resolve(".git"));
  }

  @Test
  void getCommonDirectory_ShouldUseRepositoryCache() {
    // Given:
    given(execHelper.exec("git", "rev-parse", "--show-toplevel"))
        .willReturn(GIT_TOP_LEVEL_DIR.toString());
    given(execHelper.exec("git", "rev-parse", "--git-common-dir")).willReturn(".git");

    // When:
    Path gitCommonDir1 = gitHelper.getCommonDirectory();
    Path gitCommonDir2 = gitHelper.getCommonDirectory();

    // Then:
    assertThat(gitCommonDir1).isEqualTo(GIT_TOP_LEVEL_DIR.resolve(".git"));
    assertThat(gitCommonDir2).isEqualTo(GIT_TOP_LEVEL_DIR.resolve(".git"));

    verify(execHelper).exec("git", "rev-parse", "--show-toplevel");
    verify(execHelper).exec("git", "rev-parse", "--git-common-dir");
  }

  @Test
  void getTopLevelDirectory_ShouldUseRepositoryCache() {
    // Given:
    given(execHelper.exec("git", "rev-parse", "--show-toplevel"))
        .willReturn(GIT_TOP_LEVEL_DIR.toString());

    // When:
    Path gitTopLevelDir1 = gitHelper.getTopLevelDirectory();
    Path gitTopLevelDir2 = gitHelper.getTopLevelDirectory();

    // Then:
    assertThat(gitTopLevelDir1).isEqualTo(GIT_TOP_LEVEL_DIR);
    assertThat(gitTopLevelDir2).isEqualTo(GIT_TOP_LEVEL_DIR);

    verify(execHelper).exec("git", "rev-parse", "--show-toplevel");
  }

  @Test
  void getStatus_ShouldParseIntoGitStatusLines() {
    // Given:
    List<String> statusOutput = Arrays.asList(" M file1", "AM file2", "?? file3");

    given(execHelper.exec("git", "status", "--porcelain"))
        .willReturn(String.join("\n", statusOutput));

    // When:
    List<GitStatusLine> statusLines = gitHelper.status().getStatusLines();

    // Then:
    assertThat(statusLines).hasSize(3);

    assertThat(statusLines.get(0).getRelativePath()).isEqualTo(Paths.get("file1"));
    assertThat(statusLines.get(1).getRelativePath()).isEqualTo(Paths.get("file2"));
    assertThat(statusLines.get(2).getRelativePath()).isEqualTo(Paths.get("file3"));
  }

  @Test
  void getLsFiles_ShouldReturnAbsolutePaths() {
    // TODO
  }
}
