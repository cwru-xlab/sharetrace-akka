package io.sharetrace.util;

import com.google.common.collect.Range;

public final class Checks {

  private Checks() {}

  public static <T extends Comparable<T>> T checkRange(T value, Range<T> range, String name) {
    if (!range.contains(value))
      throw new IllegalArgumentException(
          String.format("%s must be in the range %s; got %s", name, range, value));
    return value;
  }
}
