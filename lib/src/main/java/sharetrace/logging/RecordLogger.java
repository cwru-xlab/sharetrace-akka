package sharetrace.logging;

import java.util.Set;
import java.util.function.Supplier;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RecordLogger<T extends LogRecord> {

  private final Logger delegate;
  private final String key;
  private final Set<Class<? extends LogRecord>> loggable;

  public RecordLogger(String name, String key, Set<Class<? extends LogRecord>> loggable) {
    this.delegate = LoggerFactory.getLogger(name);
    this.key = key;
    this.loggable = loggable;
  }

  public <R extends T> void log(Class<R> type, Supplier<R> record) {
    if (delegate.isInfoEnabled() && loggable.contains(type)) {
      delegate.info(key, StructuredArguments.value(key, record.get()));
    }
  }
}
