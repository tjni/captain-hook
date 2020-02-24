package com.github.tjni.captainhook;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

/** Configuration for {@link CaptainHookPlugin}. */
public class CaptainHookExtension {
  /**
   * The name of the extension.
   *
   * <p>In the build script, configuration will be inside of a block like:
   *
   * <pre>
   *   captainHook {
   *      // configuration
   *   }
   * </pre>
   */
  public static final String EXTENSION_NAME = "captainHook";

  private final Property<Boolean> autoApplyGitHooks;
  private final Property<String> applypatchMsg;
  private final Property<String> preApplypatch;
  private final Property<String> postApplypatch;
  private final Property<String> preCommit;
  private final Property<String> preMergeCommit;
  private final Property<String> prepareCommitMsg;
  private final Property<String> commitMsg;
  private final Property<String> postCommit;
  private final Property<String> preRebase;
  private final Property<String> postCheckout;
  private final Property<String> postMerge;
  private final Property<String> prePush;
  private final Property<String> preReceive;
  private final Property<String> update;
  private final Property<String> postReceive;
  private final Property<String> postUpdate;
  private final Property<String> pushToCheckout;
  private final Property<String> preAutoGc;
  private final Property<String> postRewrite;
  private final Property<String> sendemailValidate;

  public CaptainHookExtension(ObjectFactory objectFactory) {
    // noinspection UnstableApiUsage
    autoApplyGitHooks = objectFactory.property(Boolean.class).value(true);
    applypatchMsg = objectFactory.property(String.class);
    preApplypatch = objectFactory.property(String.class);
    postApplypatch = objectFactory.property(String.class);
    preCommit = objectFactory.property(String.class);
    preMergeCommit = objectFactory.property(String.class);
    prepareCommitMsg = objectFactory.property(String.class);
    commitMsg = objectFactory.property(String.class);
    postCommit = objectFactory.property(String.class);
    preRebase = objectFactory.property(String.class);
    postCheckout = objectFactory.property(String.class);
    postMerge = objectFactory.property(String.class);
    prePush = objectFactory.property(String.class);
    preReceive = objectFactory.property(String.class);
    update = objectFactory.property(String.class);
    postReceive = objectFactory.property(String.class);
    postUpdate = objectFactory.property(String.class);
    pushToCheckout = objectFactory.property(String.class);
    preAutoGc = objectFactory.property(String.class);
    postRewrite = objectFactory.property(String.class);
    sendemailValidate = objectFactory.property(String.class);
  }

  /**
   * Whether to apply configured Git hooks automatically.
   *
   * <p>When disabled, Git hooks configured will only be applied by executing the {@code
   * applyGitHooks} task.
   *
   * <p>This property defaults to {@code true}.
   *
   * @return whether to apply configured Git hooks automatically
   */
  public Property<Boolean> getAutoApplyGitHooks() {
    return autoApplyGitHooks;
  }

  /**
   * The Git applypatch-msg hook.
   *
   * @return the Git applypatch-msg hook
   * @see <a href="https://git-scm.com/docs/githooks#_applypatch_msg">applypatch-msg</a>
   */
  public Property<String> getApplypatchMsg() {
    return applypatchMsg;
  }

  /**
   * The Git pre-applypatch hook.
   *
   * @return the Git pre-applypatch hook
   * @see <a href="https://git-scm.com/docs/githooks#_pre_applypatch">pre-applypatch</a>
   */
  public Property<String> getPreApplypatch() {
    return preApplypatch;
  }

  /**
   * The Git post-applypatch hook.
   *
   * @return the Git post-applypatch hook
   * @see <a href="https://git-scm.com/docs/githooks#_post_applypatch">post-applypatch</a>
   */
  public Property<String> getPostApplypatch() {
    return postApplypatch;
  }

  /**
   * The Git pre-commit hook.
   *
   * @return the Git pre-commit hook
   * @see <a href="https://git-scm.com/docs/githooks#_pre_commit">pre-commit</a>
   */
  public Property<String> getPreCommit() {
    return preCommit;
  }

  /**
   * The Git pre-merge-commit hook.
   *
   * @return the Git pre-merge-commit hook
   * @see <a href="https://git-scm.com/docs/githooks#_pre_merge_commit">pre-merge-commit</a>
   */
  public Property<String> getPreMergeCommit() {
    return preMergeCommit;
  }

  /**
   * The Git prepare-commit-msg hook.
   *
   * @return the Git prepare-commit-msg hook
   * @see <a href="https://git-scm.com/docs/githooks#_prepare_commit_msg">prepare-commit-msg</a>
   */
  public Property<String> getPrepareCommitMsg() {
    return prepareCommitMsg;
  }

  /**
   * The Git commit-msg hook.
   *
   * @return the Git commit-msg hook
   * @see <a href="https://git-scm.com/docs/githooks#_commit_msg">commit-msg</a>
   */
  public Property<String> getCommitMsg() {
    return commitMsg;
  }

  /**
   * The Git post-commit hook.
   *
   * @return the Git post-commit hook
   * @see <a href="https://git-scm.com/docs/githooks#_post_commit">post-commit</a>
   */
  public Property<String> getPostCommit() {
    return postCommit;
  }

  /**
   * The Git pre-rebase hook.
   *
   * @return the Git pre-rebase hook
   * @see <a href="https://git-scm.com/docs/githooks#_pre_rebase">pre-rebase</a>
   */
  public Property<String> getPreRebase() {
    return preRebase;
  }

  /**
   * The Git post-checkout hook.
   *
   * @return the Git post-checkout hook
   * @see <a href="https://git-scm.com/docs/githooks#_post_checkout">post-checkout</a>
   */
  public Property<String> getPostCheckout() {
    return postCheckout;
  }

  /**
   * The Git post-merge hook.
   *
   * @return the Git post-merge hook
   * @see <a href="https://git-scm.com/docs/githooks#_post_merge">post-merge</a>
   */
  public Property<String> getPostMerge() {
    return postMerge;
  }

  /**
   * The Git pre-push hook.
   *
   * @return the Git pre-push hook
   * @see <a href="https://git-scm.com/docs/githooks#_pre_push">pre-push</a>
   */
  public Property<String> getPrePush() {
    return prePush;
  }

  /**
   * The Git pre-receive hook.
   *
   * @return the Git pre-receive hook
   * @see <a href="https://git-scm.com/docs/githooks#_pre_receive">pre-receive</a>
   */
  public Property<String> getPreReceive() {
    return preReceive;
  }

  /**
   * The Git update hook.
   *
   * @return the Git update hook
   * @see <a href="https://git-scm.com/docs/githooks#_update">update</a>
   */
  public Property<String> getUpdate() {
    return update;
  }

  /**
   * The Git post-receive hook.
   *
   * @return the Git post-receive hook
   * @see <a href="https://git-scm.com/docs/githooks#_post_receive">post-receive</a>
   */
  public Property<String> getPostReceive() {
    return postReceive;
  }

  /**
   * The Git post-update hook.
   *
   * @return the Git post-update hook
   * @see <a href="https://git-scm.com/docs/githooks#_post_update">post-update</a>
   */
  public Property<String> getPostUpdate() {
    return postUpdate;
  }

  /**
   * The Git push-to-checkout hook.
   *
   * @return the Git push-to-checkout hook
   * @see <a href="https://git-scm.com/docs/githooks#_push_to_checkout">push-to-checkout</a>
   */
  public Property<String> getPushToCheckout() {
    return pushToCheckout;
  }

  /**
   * The Git pre-auto-gc hook.
   *
   * @return the Git pre-auto-gc hook
   * @see <a href="https://git-scm.com/docs/githooks#_pre_auto_gc">pre-auto-gc</a>
   */
  public Property<String> getPreAutoGc() {
    return preAutoGc;
  }

  /**
   * The Git post-rewrite hook.
   *
   * @return the Git post-rewrite hook
   * @see <a href="https://git-scm.com/docs/githooks#_post_rewrite">post-rewrite</a>
   */
  public Property<String> getPostRewrite() {
    return postRewrite;
  }

  /**
   * The Git sendemail-validate hook.
   *
   * @return the Git sendemail-validate hook
   * @see <a href="https://git-scm.com/docs/githooks#_sendemail_validate">sendemail-validate</a>
   */
  public Property<String> getSendemailValidate() {
    return sendemailValidate;
  }
}
