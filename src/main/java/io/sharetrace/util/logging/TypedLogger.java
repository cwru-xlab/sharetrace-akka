package io.sharetrace.util.logging;

import java.util.function.Supplier;

@FunctionalInterface
public interface TypedLogger<T extends Loggable> {

  <R extends T> boolean log(String key, Class<R> type, Supplier<R> loggable);
}
