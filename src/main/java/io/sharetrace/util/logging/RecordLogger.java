package io.sharetrace.util.logging;

import java.util.function.Supplier;

@FunctionalInterface
public interface RecordLogger<T extends LogRecord> {

  <R extends T> boolean log(String key, Class<R> type, Supplier<R> record);
}
