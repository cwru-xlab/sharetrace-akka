package org.sharetrace.util;

import java.util.function.Supplier;

/** A collection of utility methods for checking conditions and verifying assumptions. */
public final class Preconditions {

  private Preconditions() {}

  public static void checkArgument(boolean condition, Supplier<String> messageSupplier) {
    if (!condition) {
      throw new IllegalArgumentException(messageSupplier.get());
    }
  }
}
