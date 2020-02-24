package com.github.tjni.captainhook.helpers;

import com.github.tjni.captainhook.annotations.ImmutableStyle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import one.util.streamex.StreamEx;
import org.gradle.api.Project;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

/** A helper for executing commands. */
@Singleton
public class ExecHelper {
  private final Path workingDir;

  @Inject
  public ExecHelper(Project project) {
    workingDir = project.getRootDir().toPath();
  }

  public String exec(String executable, String... args) {
    ExecResult result = rawExec(executable, args);

    if (result.getExitCode() != 0) {
      throw new ExecException(
          String.join(" ", getCommand(executable, args)), result.getExitCode(), result.getStderr());
    }

    return result.getStdout();
  }

  public ExecResult rawExec(String executable, String... args) {
    return rawExec(ImmutableExecRequest.builder().setExecutable(executable).setArgs(args).build());
  }

  private ExecResult rawExec(ExecRequest request) {
    List<String> command = getCommand(request.getExecutable(), request.getArgs());
    ProcessBuilder processBuilder = new ProcessBuilder(command).directory(workingDir.toFile());

    Process process;
    try {
      process = processBuilder.start();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    writeStdin(process, request.getStdin());

    String stdout = readStream(process.getInputStream()).trim();
    String stderr = readStream(process.getErrorStream()).trim();

    int exitCode;
    try {
      exitCode = process.waitFor();
    } catch (InterruptedException e) {
      throw new RuntimeException("The thread waiting for the process was interrupted.", e);
    }

    return ImmutableExecResult.builder()
        .setExitCode(exitCode)
        .setStdout(stdout)
        .setStderr(stderr)
        .build();
  }

  private static void writeStdin(Process process, String stdin) {
    if (!stdin.isEmpty()) {
      OutputStream stream = process.getOutputStream();
      try (Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
        writer.write(stdin);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private static String readStream(InputStream stream) {
    ByteArrayOutputStream result = new ByteArrayOutputStream();

    try {
      byte[] buffer = new byte[1024];
      int length;
      while ((length = stream.read(buffer)) != -1) {
        result.write(buffer, 0, length);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    try {
      return result.toString(StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static List<String> getCommand(String executable, String... args) {
    return StreamEx.of(executable).append(args).toList();
  }

  @Immutable
  @ImmutableStyle
  public interface ExecRequest {
    String getExecutable();

    String[] getArgs();

    @Default
    default String getStdin() {
      return "";
    }
  }

  @Immutable
  @ImmutableStyle
  public interface ExecResult {
    int getExitCode();

    String getStdout();

    String getStderr();
  }

  public static class ExecException extends RuntimeException {
    private final String error;

    public ExecException(String command, int exitCode, String error) {
      super(
          String.format("Command %s exited with code %d and error %s.", command, exitCode, error));
      this.error = error;
    }

    public String getError() {
      return error;
    }
  }
}
