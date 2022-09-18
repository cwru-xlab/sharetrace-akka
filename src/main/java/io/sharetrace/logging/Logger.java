package io.sharetrace.logging;

import io.sharetrace.util.TypedSupplier;

public interface Logger {

  default boolean log(String messageAndKey, Loggable loggable) {
    return log(messageAndKey, messageAndKey, TypedSupplier.of(loggable));
  }

  boolean log(String message, String key, TypedSupplier<? extends Loggable> supplier);

  default boolean log(String messageAndKey, TypedSupplier<? extends Loggable> supplier) {
    return log(messageAndKey, messageAndKey, supplier);
  }
}
