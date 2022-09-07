package org.sharetrace.logging;

import org.sharetrace.util.TypedSupplier;

public interface Logger {

  default boolean log(String messageAndKey, Loggable loggable) {
    return log(messageAndKey, messageAndKey, TypedSupplier.of(loggable));
  }

  boolean log(String message, String key, TypedSupplier<? extends Loggable> loggable);

  default boolean log(String messageAndKey, TypedSupplier<? extends Loggable> loggable) {
    return log(messageAndKey, messageAndKey, loggable);
  }
}
