package org.sharetrace.util;

import java.util.function.Supplier;

public final class Preconditions {

  private Preconditions() {}

  public static void checkArgument(boolean condition, Supplier<String> messageSupplier) {
    if (!condition) {
      throw new IllegalArgumentException(messageSupplier.get());
    }
  }
}
