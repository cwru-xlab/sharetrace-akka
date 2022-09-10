package org.sharetrace.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import java.util.Objects;

public final class Checks {

  private static final String RANGE_MSG = "%s must be in the range %s; got %s";
  private static final String IS_NOT_MSG = "%s must not be %s";

  private Checks() {}

  public static <T> void isNotNull(T value, String name) {
    Objects.requireNonNull(value, name);
  }

  public static <T extends Comparable<T>> void isAtLeast(T value, T atLeast, String name) {
    inRange(value, Range.atLeast(atLeast), name);
  }

  public static <T extends Comparable<T>> T inRange(T value, Range<T> range, String name) {
    checkArgument(range.contains(value), RANGE_MSG, name, range, value);
    return value;
  }

  public static void checkArgument(boolean condition, String messageTemplate, Object... args) {
    Preconditions.checkArgument(condition, messageTemplate, args);
  }

  public static <T extends Comparable<T>> void isGreaterThan(T value, T greaterThan, String name) {
    inRange(value, Range.greaterThan(greaterThan), name);
  }

  public static <T extends Comparable<T>> void inClosedRange(
      T value, T lower, T upper, String name) {
    inRange(value, Range.closed(lower, upper), name);
  }

  public static <T extends Comparable<T>> void inOpenRange(T value, T lower, T upper, String name) {
    inRange(value, Range.open(lower, upper), name);
  }

  public static <T extends Comparable<T>> void inClosedOpen(
      T value, T lower, T upper, String name) {
    inRange(value, Range.closedOpen(lower, upper), name);
  }

  public static <T> T isNot(T value, T not, String name) {
    checkArgument(!Objects.equals(value, not), IS_NOT_MSG, name, not);
    return value;
  }
}
