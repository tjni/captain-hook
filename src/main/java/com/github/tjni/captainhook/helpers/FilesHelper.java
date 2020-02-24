package com.github.tjni.captainhook.helpers;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Helper for file system operations. */
@Singleton
public class FilesHelper {
  @Inject
  public FilesHelper() {}

  /**
   * Tests whether a path exists.
   *
   * @param path the path to test
   * @return {@code true} if the path exists; {@code false} if the path does not exist or its
   *     existence cannot be determined.
   * @see Files#exists(Path, java.nio.file.LinkOption...)
   */
  public boolean exists(Path path) {
    return Files.exists(path);
  }

  /**
   * Reads all characters from a file into a {@link String}.
   *
   * @param file the file to read from
   * @return a string containing all the characters from the file
   * @throws UncheckedIOException if an I/O error occurs
   */
  public String toString(Path file) {
    try {
      return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Writes a string to a file.
   *
   * @param file the path to the file
   * @param str the string to write
   * @return the path to the file
   * @throws UncheckedIOException if an I/O error occurs
   */
  public Path write(Path file, String str) {
    try {
      return Files.write(file, str.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Deletes a file.
   *
   * @param file the path to the file
   * @throws UncheckedIOException if an I/O error occurs
   * @see Files#delete(Path)
   */
  public void delete(Path file) {
    try {
      Files.delete(file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Deletes multiple files.
   *
   * @param files the path to the files
   * @throws UncheckedIOException if an I/O error occurs
   * @see Files#delete(Path)
   */
  public void delete(Iterable<Path> files) {
    files.forEach(this::delete);
  }

  /**
   * Deletes a file if it exists.
   *
   * @param file the path to the file
   * @return {@code true} if the file was deleted by this method; {@code false} if the file could
   *     not be deleted because it did not exist
   * @throws UncheckedIOException if an I/O error occurs
   * @see Files#deleteIfExists(Path)
   */
  public boolean deleteIfExists(Path file) {
    try {
      return Files.deleteIfExists(file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Checks if a file is empty.
   *
   * <p>This method performs a rudimentary check and will not work for files that contain a byte
   * order mark (BOM). Note that BOMs do not exist if a file is encoded in UTF-8.
   *
   * @param file the path to the file
   * @return {@code true} if the file is empty
   */
  public boolean isFileEmpty(Path file) {
    return file.toFile().length() == 0;
  }

  /**
   * Checks if the file is empty after its contents have been trimmed.
   *
   * @param file the path to the file
   * @return {@code true} if the file is empty after its contents have been trimmed
   */
  public boolean isTrimmedFileEmpty(Path file) {
    return isFileEmpty(file) || toString(file).trim().isEmpty();
  }

  /**
   * Tests whether a directory is empty.
   *
   * @param dir the path to the directory
   * @return whether the directory is empty
   * @throws UncheckedIOException if an I/O error occurs or if the path is not a directory
   */
  public boolean isDirectoryEmpty(Path dir) {
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
      return !dirStream.iterator().hasNext();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Creates a directory by creating all nonexistent parent directories first.
   *
   * @param dir the directory to create
   * @param attrs an optional list of file attributes to set atomically creating the directory
   * @return the directory
   * @throws UncheckedIOException if an I/O error occurs
   * @see Files#createDirectories(Path, FileAttribute[])
   */
  public Path createDirectories(Path dir, FileAttribute<?>... attrs) {
    try {
      return Files.createDirectories(dir, attrs);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Creates an {@link OutputStreamWriter} for writing to a file.
   *
   * @param file the file to be opened for writing
   * @throws UncheckedIOException if an I/O error occurs
   * @return an {@code OutputStreamWriter}
   */
  public Writer createFileOutputStreamWriter(Path file) {
    FileOutputStream fileOutputStream;
    try {
      fileOutputStream = new FileOutputStream(file.toFile());
    } catch (FileNotFoundException e) {
      throw new UncheckedIOException(e);
    }
    return new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
  }

  /**
   * Sets a path's POSIX permissions.
   *
   * @param path the path
   * @param permissions string representing a set of permissions
   * @return the path
   * @throws UncheckedIOException if an I/O error occurs
   * @see Files#setPosixFilePermissions(Path, java.util.Set)
   */
  public Path setPosixFilePermissions(Path path, String permissions) {
    try {
      return Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
