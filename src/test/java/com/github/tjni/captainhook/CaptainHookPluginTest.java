package com.github.tjni.captainhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.github.tjni.captainhook.helpers.ApplyPluginHelper;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class CaptainHookPluginTest {
  private final ApplyPluginHelper applyPluginHelper;
  private final Project project;
  private final CaptainHookPlugin captainHookPlugin;

  CaptainHookPluginTest(
      @Mock(name = "applyPluginHelper") ApplyPluginHelper applyPluginHelper,
      @Mock(name = "project") Project project) {
    this.applyPluginHelper = applyPluginHelper;
    this.project = project;
    captainHookPlugin = new CaptainHookPlugin();
  }

  @Test
  void apply_WhenAppliedToRootProject_ShouldCreateExtension() {
    // Given:
    ExtensionContainer extensions = mock(ExtensionContainer.class, "extensions");

    given(project.getRootProject()).willReturn(project);
    given(project.getExtensions()).willReturn(extensions);

    CaptainHookPlugin spyCaptainHookPlugin = spy(captainHookPlugin);
    willDoNothing().given(spyCaptainHookPlugin).apply(any(ApplyPluginHelper.class));

    // When:
    spyCaptainHookPlugin.apply(project);

    // Then:
    InOrder inOrder = inOrder(extensions, spyCaptainHookPlugin);
    inOrder
        .verify(extensions)
        .create(CaptainHookExtension.EXTENSION_NAME, CaptainHookExtension.class);
    inOrder.verify(spyCaptainHookPlugin).apply(any(ApplyPluginHelper.class));
  }

  @Test
  void apply_WhenAppliedToChildProject_ShouldThrowGradleException() {
    // Given:
    Project rootProject = mock(Project.class, "rootProject");

    given(project.getRootProject()).willReturn(rootProject);

    // When:
    Throwable exception = catchThrowable(() -> captainHookPlugin.apply(project));

    // Then:
    assertThat(exception)
        .isInstanceOf(GradleException.class)
        .hasMessageContainingAll(CaptainHookPlugin.PLUGIN_ID, "root project");
  }

  @Test
  void apply_ShouldDelegateToHelper() {
    // When:
    captainHookPlugin.apply(applyPluginHelper);

    // Then:
    verify(applyPluginHelper).createApplyGitHooksTask();
    verify(applyPluginHelper).configureStaging();
    verify(applyPluginHelper).maybeAutoApplyGitHooks();
  }
}
