package org.sharetrace.util;

public final class Preconditions {

  private Preconditions() {
  }

  public static void checkArgument(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }
}
