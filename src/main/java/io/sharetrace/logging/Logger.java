package io.sharetrace.logging;

import java.util.function.Supplier;

@FunctionalInterface
public interface Logger {

  <T extends Loggable> boolean log(String key, Class<T> type, Supplier<T> loggable);
}
