package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.typesafe.config.Config;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.commons.math3.random.RandomGenerator;
import sharetrace.Buildable;
import sharetrace.logging.LogRecord;
import sharetrace.logging.RecordLogger;
import sharetrace.model.factory.TimeFactory;

@Buildable
public record Context(
    long referenceTime,
    long seed,
    RandomGenerator randomGenerator,
    @JsonIgnore Config config,
    @JsonIgnore TimeFactory dataTimeFactory,
    @JsonIgnore TimeFactory systemTimeFactory,
    @JsonIgnore Map<String, String> mdc,
    @JsonIgnore RecordLogger propertyLogger,
    @JsonIgnore RecordLogger eventLogger) {

  public long getDataTime() {
    return dataTimeFactory.getTime();
  }

  public long getSystemTime() {
    return systemTimeFactory.getTime();
  }

  public <T extends LogRecord> void logEvent(Class<T> type, Supplier<T> record) {
    eventLogger.log(type, record);
  }

  public <T extends LogRecord> void logProperty(Class<T> type, Supplier<T> record) {
    propertyLogger.log(type, record);
  }
}
