package sharetrace.logging;

import java.util.Set;
import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import sharetrace.Buildable;
import sharetrace.model.factory.TimeFactory;

@Buildable
public record RecordLogger(
    Logger logger, String key, Set<Class<? extends LogRecord>> logged, TimeFactory timeFactory) {

  public void log(LogRecord record) {
    if (logger.isInfoEnabled() && logged.contains(record.getClass())) {
      logger.info(key, timeField(), recordField(record));
    }
  }

  private StructuredArgument timeField() {
    return StructuredArguments.keyValue("t", timeFactory.getTime());
  }

  private StructuredArgument recordField(LogRecord record) {
    return StructuredArguments.value(key, record);
  }
}
