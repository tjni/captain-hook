package com.github.tjni.captainhook.helpers;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class OperatingSystemHelper {

  @Inject
  public OperatingSystemHelper() {}

  /**
   * Returns the maximum length of a command on the command line.
   *
   * @return the maximum length of a command on the command line
   */
  public int getMaxCommandLength() {
    String osName = System.getProperty("os.name");
    if (osName.startsWith("Mac")) {
      return 262144;
    } else if (osName.startsWith("Windows")) {
      return 8191;
    } else {
      return 131072;
    }
  }
}
