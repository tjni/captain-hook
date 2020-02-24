# Captain Hook

![build](https://github.com/tjni/captain-hook/workflows/build/badge.svg)

Gradle plugin for installing Git hooks.

## Usage

In the root project's build.gradle, apply the plugin:

```groovy
buildscript {
  repositories {
    mavenCentral()
  }

  dependencies {
    classpath 'com.github.tjni.captainhook:captain-hook:0.1.0'
  }
}

apply plugin: 'com.github.tjni.captainhook'
```

Later in your script, to add a pre-commit hook, write:

```groovy
captainHook {
  preCommit = 'echo "Hello, World!"'
}
```

By default, the plugin will add, update, or remove hooks whenever any Gradle task is run. This behavior can be disabled by specifying `autoApplyGitHooks = false` in the same block.

## Hooks

The hooks that are available are:

- `applypatchMsg`
- `preApplypatch`
- `postApplypatch`
- `preCommit`
- `preMergeCommit`
- `prepareCommitMsg`
- `commitMsg`
- `postCommit`
- `preRebase`
- `postCheckout`
- `postMerge`
- `prePush`
- `preReceive`
- `update`
- `postReceive`
- `postUpdate`
- `pushToCheckout`
- `preAutoGc`
- `postRewrite`
- `sendemailValidate`

## Staging

Captain Hook also can help you apply automatic code changes to files in the [staging area](https://git-scm.com/book/en/v2/Git-Basics-Recording-Changes-to-the-Repository) prior to your commit. This is a great way to improve code quality unobtrusively.

```groovy
captainHook {
  preCommit = './gradlew staging spotlessApply'
}
```

The `staging` modifier is a signal to Captain Hook to snapshot the state of your working directory before running subsequent tasks. If an error happens, it will do its best to restore your state. If no error occurs, but some staged files were modified, it will add them to your in-progress commit.

Captain Hook will leave untracked files alone, including changes that are made by the pre-commit hook. For example, the `spotlessApply` task from the [Spotless plugin](https://github.com/diffplug/spotless/tree/master/plugin-gradle) will run on every file and can lead to modifications to the untracked files.

To address this, the plugin will create a Gradle project property on the root project called <b>staging</b>, with type <i>List&lt;Path&gt;</i>, that contains the absolute paths to each file in the staging area. It's possible to create wrapper tasks that configure the underlying tasks to pay attention only to these files.

## Attribution

This would not exist if not for the great projects that came before this. The behavior of this plugin is transcribed from <b>[husky](https://github.com/typicode/husky)</b> and <b>[lint-staged](https://github.com/okonet/lint-staged)</b>, except with fewer features and adapted to Java &amp; Gradle.
