package org.sharetrace.util;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/** A collection of utility methods for checking conditions and verifying assumptions. */
public final class Preconditions {

  private Preconditions() {}

  public static void checkArgument(boolean condition, Supplier<String> message) {
    if (!condition) {
      throw new IllegalArgumentException(Objects.requireNonNull(message).get());
    }
  }

  public static void checkInClosedRange(
      double value, double lowerBound, double upperBound, String name) {
    Supplier<String> message = () -> closedRangeMessage(name, lowerBound, upperBound, value);
    checkInClosedRange(value, lowerBound, upperBound, message);
  }

  public static void checkInClosedRange(
      double value, double lowerBound, double upperBound, Supplier<String> message) {
    checkArgument(value >= lowerBound && value <= upperBound, message);
  }

  public static void checkIsNonNegative(Duration duration, String name) {
    checkIsNonNegative(duration, () -> nonNegativeMessage(name, duration));
  }

  public static void checkIsNonNegative(Duration duration, Supplier<String> message) {
    checkArgument(!Objects.requireNonNull(duration).isNegative(), message);
  }

  public static void checkIsAtLeast(double value, double lowerBound, String name) {
    checkIsAtLeast(value, lowerBound, () -> atLeastMessage(name, lowerBound, value));
  }

  public static void checkIsAtLeast(double value, double lowerBound, Supplier<String> message) {
    checkArgument(value >= lowerBound, message);
  }

  public static void checkIsPositive(Duration duration, String name) {
    checkIsPositive(duration, () -> positiveMessage(name, duration));
  }

  public static void checkIsPositive(Duration duration, Supplier<String> message) {
    Objects.requireNonNull(duration);
    checkArgument(!duration.isNegative() && !duration.isZero(), message);
  }

  private static <T> String closedRangeMessage(String name, T loweBound, T upperBound, T value) {
    return "'"
        + name
        + "' must be between "
        + loweBound
        + " and "
        + upperBound
        + ", inclusive; got "
        + value;
  }

  private static <T> String nonNegativeMessage(String name, T value) {
    return "'" + name + "' must be non-negative; got " + value;
  }

  private static <T> String atLeastMessage(String name, T lowerBound, T value) {
    return "'" + name + "' must be at least " + lowerBound + "; got " + value;
  }

  private static <T> String positiveMessage(String name, T value) {
    return "'" + name + "' must be positive; got " + value;
  }
}
