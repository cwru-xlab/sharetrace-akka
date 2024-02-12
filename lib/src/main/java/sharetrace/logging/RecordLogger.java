package sharetrace.logging;

import java.util.Set;
import java.util.function.Supplier;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;

public record RecordLogger(Logger delegate, String key, Set<Class<? extends LogRecord>> logged) {

  public <T extends LogRecord> void log(Class<T> type, Supplier<T> record) {
    if (delegate.isInfoEnabled() && logged.contains(type)) {
      delegate.info(key, StructuredArguments.value(key, record.get()));
    }
  }
}
