package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.typesafe.config.Config;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.math3.random.RandomGenerator;
import sharetrace.Buildable;
import sharetrace.logging.LogRecord;
import sharetrace.logging.RecordLogger;
import sharetrace.model.factory.TimeFactory;

@Buildable
public record Context(
    @JsonIgnore Config config,
    @JsonIgnore TimeFactory timeFactory,
    long referenceTime,
    long seed,
    RandomGenerator randomGenerator,
    Set<Class<? extends LogRecord>> logged,
    Map<String, String> tags,
    @JsonIgnore Map<String, String> mdc,
    @JsonIgnore RecordLogger propertyLogger,
    @JsonIgnore RecordLogger eventLogger)
    implements TimeFactory {

  @Override
  public long getTime() {
    return timeFactory.getTime();
  }

  public <T extends LogRecord> void logEvent(Class<T> type, Supplier<T> record) {
    eventLogger.log(type, record);
  }

  public <T extends LogRecord> void logProperty(Class<T> type, Supplier<T> record) {
    propertyLogger.log(type, record);
  }
}
