package io.sharetrace.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;

public final class Checks {

  private static final String RANGE_MSG = "%s must be in the range %s; got %s";

  private Checks() {}

  public static <T extends Comparable<T>> void isAtLeast(T value, T atLeast, String name) {
    inRange(value, Range.atLeast(atLeast), name);
  }

  public static <T extends Comparable<T>> T inRange(T value, Range<T> range, String name) {
    Preconditions.checkArgument(range.contains(value), RANGE_MSG, name, range, value);
    return value;
  }

  public static <T extends Comparable<T>> void isGreaterThan(T value, T greaterThan, String name) {
    inRange(value, Range.greaterThan(greaterThan), name);
  }

  public static <T extends Comparable<T>> void inClosed(T value, T lower, T upper, String name) {
    inRange(value, Range.closed(lower, upper), name);
  }

  public static <T extends Comparable<T>> void inOpen(T value, T lower, T upper, String name) {
    inRange(value, Range.open(lower, upper), name);
  }

  public static <T extends Comparable<T>> void inClosedOpen(
      T value, T lower, T upper, String name) {
    inRange(value, Range.closedOpen(lower, upper), name);
  }
}
