package sharetrace.util.logging;

import java.util.function.Supplier;

public interface RecordLogger<T extends LogRecord> {

  <R extends T> boolean log(Class<R> type, Supplier<R> record);
}