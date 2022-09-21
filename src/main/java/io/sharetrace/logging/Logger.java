package io.sharetrace.logging;

import io.sharetrace.util.TypedSupplier;

public interface Logger {

  default boolean log(String msgAndKey, Loggable loggable) {
    return log(msgAndKey, msgAndKey, TypedSupplier.of(loggable));
  }

  boolean log(String msg, String key, TypedSupplier<? extends Loggable> supplier);

  default boolean log(String msgAndKey, TypedSupplier<? extends Loggable> supplier) {
    return log(msgAndKey, msgAndKey, supplier);
  }
}
