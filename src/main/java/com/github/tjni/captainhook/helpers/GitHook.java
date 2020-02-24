package com.github.tjni.captainhook.helpers;

/**
 * The available Git hooks.
 *
 * @see <a href="https://git-scm.com/docs/githooks">Git hooks documentation</a>
 */
public enum GitHook {
  APPLYPATCH_MSG("applypatch-msg"),
  PRE_APPLYPATCH("pre-applypatch"),
  POST_APPLYPATCH("post-applypatch"),
  PRE_COMMIT("pre-commit"),
  PRE_MERGE_COMMIT("pre-merge-commit"),
  PREPARE_COMMIT_MSG("prepare-commit-msg"),
  COMMIT_MSG("commit-msg"),
  POST_COMMIT("post-commit"),
  PRE_REBASE("pre-rebase"),
  POST_CHECKOUT("post-checkout"),
  POST_MERGE("post-merge"),
  PRE_PUSH("pre-push"),
  PRE_RECEIVE("pre-receive"),
  UPDATE("update"),
  POST_RECEIVE("post-receive"),
  POST_UPDATE("post-update"),
  PUSH_TO_CHECKOUT("push-to-checkout"),
  PRE_AUTO_GC("pre-auto-gc"),
  POST_REWRITE("post-rewrite"),
  SENDEMAIL_VALIDATE("sendemail-validate");

  private final String _hookName;

  GitHook(String hookName) {
    _hookName = hookName;
  }

  /**
   * Returns the name of the Git hook as documented in Git.
   *
   * @return the name of the Git hook
   * @see <a href="https://git-scm.com/docs/githooks">githooks documentation</a>
   */
  public String getHookName() {
    return _hookName;
  }
}
