package io.sharetrace.util;

import com.google.common.collect.Range;

public final class Checks {

  private Checks() {}

  public static <T extends Comparable<T>> T checkRange(T value, Range<T> range, String name) {
    if (!range.contains(value)) {
      throw new IllegalArgumentException(
          String.format("%s must be in the range %s; got %s", name, range, value));
    }
    return value;
  }

  public static void checkState(boolean condition, String template, Object arg) {
    if (!condition) {
      throw new IllegalStateException(String.format(template, arg));
    }
  }

  public static void checkState(boolean condition, String template, Object arg1, Object arg2) {
    if (!condition) {
      throw new IllegalStateException(String.format(template, arg1, arg2));
    }
  }
}
