package org.sharetrace.util;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/** A collection of utility methods for checking conditions and verifying assumptions. */
public final class Preconditions {

  private Preconditions() {}

  public static double checkInClosedRange(
      double value, double lowerBound, double upperBound, String name) {
    Supplier<String> message = () -> closedRangeMessage(name, lowerBound, upperBound, value);
    return checkInClosedRange(value, lowerBound, upperBound, message);
  }

  public static long checkInLowerInclusiveRange(
      long value, long lowerBound, long upperBound, String name) {
    Supplier<String> message = () -> lowerInclusiveMessage(name, lowerBound, upperBound, value);
    return checkInLowerInclusiveRange(value, lowerBound, upperBound, message);
  }

  public static double checkInClosedRange(
      double value, double lowerBound, double upperBound, Supplier<String> message) {
    checkArgument(value >= lowerBound && value <= upperBound, message);
    return value;
  }

  public static void checkArgument(boolean condition, Supplier<String> message) {
    if (!condition) {
      throw new IllegalArgumentException(Objects.requireNonNull(message).get());
    }
  }

  public static int checkIsAtLeast(int value, int lowerBound, Supplier<String> message) {
    checkArgument(value >= lowerBound, message);
    return value;
  }

  private static <T> String lowerInclusiveMessage(
      String name, T lowerBound, T upperBound, T value) {
    return "'"
        + name
        + "' must be at least "
        + lowerBound
        + " and "
        + upperBound
        + "less than; got "
        + value;
  }

  public static long checkInLowerInclusiveRange(
      long value, long lowerBound, long upperBound, Supplier<String> message) {
    checkArgument(value >= lowerBound && value < upperBound, message);
    return value;
  }

  public static <N extends Number> N checkIsAtLeast(N value, N lowerBound, String name) {
    return checkIsAtLeast(value, lowerBound, () -> atLeastMessage(name, lowerBound, value));
  }

  public static <N extends Number> N checkIsAtLeast(
      N value, N lowerBound, Supplier<String> message) {
    checkArgument(value.doubleValue() >= lowerBound.doubleValue(), message);
    return value;
  }

  private static <T> String atLeastMessage(String name, T lowerBound, T value) {
    return "'" + name + "' must be at least " + lowerBound + "; got " + value;
  }

  public static <N extends Number> N checkIsNonNegative(N value, String name) {
    return checkIsNonNegative(value, () -> nonNegativeMessage(name, value));
  }

  public static Duration checkIsNonNegative(Duration duration, String name) {
    return checkIsNonNegative(duration, () -> nonNegativeMessage(name, duration));
  }

  public static Duration checkIsNonNegative(Duration duration, Supplier<String> message) {
    checkArgument(!Objects.requireNonNull(duration).isNegative(), message);
    return duration;
  }

  private static <T> String nonNegativeMessage(String name, T value) {
    return "'" + name + "' must be non-negative; got " + value;
  }

  public static <N extends Number> N checkIsNonNegative(N value, Supplier<String> message) {
    checkArgument(Objects.requireNonNull(value).doubleValue() >= 0, message);
    return value;
  }

  public static <N extends Number> N checkIsPositive(N value, String name) {
    return checkIsPositive(value, () -> positiveMessage(name, value));
  }

  public static Duration checkIsPositive(Duration duration, String name) {
    return checkIsPositive(duration, () -> positiveMessage(name, duration));
  }

  public static Duration checkIsPositive(Duration duration, Supplier<String> message) {
    Objects.requireNonNull(duration);
    checkArgument(!duration.isNegative() && !duration.isZero(), message);
    return duration;
  }

  private static <T> String positiveMessage(String name, T value) {
    return "'" + name + "' must be positive; got " + value;
  }

  public static <N extends Number> N checkIsPositive(N value, Supplier<String> message) {
    Objects.requireNonNull(value);
    checkArgument(value.doubleValue() > 0, message);
    return value;
  }

  public static <N extends Number> N checkIsNonzero(N value, String name) {
    return checkIsNonzero(value, () -> nonzeroMessage(name, value));
  }

  public static <N extends Number> N checkIsNonzero(N value, Supplier<String> message) {
    Objects.requireNonNull(value);
    checkArgument(value.doubleValue() != 0, message);
    return value;
  }

  private static <T> String nonzeroMessage(String name, T value) {
    return "'" + name + "' must be nonzero; got " + value;
  }

  private static <T> String closedRangeMessage(String name, T lowerBound, T upperBound, T value) {
    return "'"
        + name
        + "' must be between "
        + lowerBound
        + " and "
        + upperBound
        + ", inclusive; got "
        + value;
  }
}
