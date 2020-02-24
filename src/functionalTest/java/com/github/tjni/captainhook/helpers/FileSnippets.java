package com.github.tjni.captainhook.helpers;

import one.util.streamex.StreamEx;

public final class FileSnippets {

  public static final String APPLY_PLUGIN_SNIPPET =
      mergeLines(
          "plugins {                             ",
          "  id(\"com.github.tjni.captainhook\") ",
          "}                                     ");

  private static final String EMPTY_LINE = "";

  static String mergeLines(String... paddedLines) {
    return StreamEx.of(paddedLines)
        .append(EMPTY_LINE)
        .map(FileSnippets::stripEnd)
        .joining(System.lineSeparator());
  }

  private static String stripEnd(String str) {
    int end = str.length();
    while (end > 0 && str.charAt(end - 1) == ' ') {
      end--;
    }
    return str.substring(0, end);
  }

  private FileSnippets() {}
}
