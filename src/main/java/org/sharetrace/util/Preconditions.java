package org.sharetrace.util;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/** A collection of utility methods for checking conditions and verifying assumptions. */
public final class Preconditions {

  private Preconditions() {}

  public static void checkArgument(boolean condition, Supplier<String> messageSupplier) {
    if (!condition) {
      throw new IllegalArgumentException(Objects.requireNonNull(messageSupplier).get());
    }
  }

  public static void checkInClosedRange(
      double value, double lowerBound, double upperBound, Supplier<String> messageSupplier) {
    checkArgument(value >= lowerBound && value <= upperBound, messageSupplier);
  }

  public static void checkIsNonNegative(Duration duration, Supplier<String> messageSupplier) {
    Objects.requireNonNull(duration);
    checkArgument(!duration.isNegative(), messageSupplier);
  }

  public static void checkIsAtLeast(
      double value, double lowerBound, Supplier<String> messageSupplier) {
    checkArgument(value >= lowerBound, messageSupplier);
  }

  public static void checkIsPositive(Duration duration, Supplier<String> messageSupplier) {
    Objects.requireNonNull(duration);
    checkArgument(!duration.isNegative() && !duration.isZero(), messageSupplier);
  }
}
