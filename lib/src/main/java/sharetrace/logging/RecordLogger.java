package sharetrace.logging;

import java.util.Set;
import java.util.function.Supplier;
import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import sharetrace.Buildable;
import sharetrace.model.factory.TimeFactory;

@Buildable
public record RecordLogger(
    Logger logger, String key, Set<Class<? extends LogRecord>> logged, TimeFactory timeFactory) {

  public <T extends LogRecord> void log(Class<T> type, Supplier<T> supplier) {
    if (logger.isInfoEnabled() && logged.contains(type)) {
      logger.info(key, currentTime(), record(supplier));
    }
  }

  private StructuredArgument currentTime() {
    return StructuredArguments.keyValue("t", timeFactory.getTime());
  }

  private StructuredArgument record(Supplier<?> supplier) {
    return StructuredArguments.value(key, supplier.get());
  }
}
