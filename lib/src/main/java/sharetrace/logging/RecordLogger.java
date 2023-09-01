package sharetrace.logging;

import java.util.Set;
import java.util.function.Supplier;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;

public record RecordLogger<T extends LogRecord>(
    Logger delegate, String key, Set<Class<? extends LogRecord>> loggable) {

  public <R extends T> void log(Class<R> type, Supplier<R> record) {
    if (delegate.isInfoEnabled() && loggable.contains(type))
      delegate.info(key, StructuredArguments.value(key, record.get()));
  }
}
