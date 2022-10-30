package io.sharetrace.logging;

import io.sharetrace.util.TypedSupplier;

@FunctionalInterface
public interface Logger {

  boolean log(String key, TypedSupplier<? extends Loggable> loggable);
}
