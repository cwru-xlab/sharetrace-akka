package io.sharetrace.util;

import com.google.common.collect.Range;

public final class Checks {

  private static final String RANGE_MSG = "%s must be in the range %s; got %s";

  private Checks() {}

  public static <T extends Comparable<T>> T inRange(T value, Range<T> range, String name) {
    isTrue(range.contains(value), RANGE_MSG, name, range, value);
    return value;
  }

  public static void isTrue(boolean condition, String template, Object... args) {
    if (!condition) {
      throw new IllegalArgumentException(String.format(template, args));
    }
  }
}
