package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.typesafe.config.Config;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.commons.math3.random.RandomGenerator;
import sharetrace.Buildable;
import sharetrace.logging.ExecutionProperties;
import sharetrace.logging.RecordLogger;
import sharetrace.logging.event.Event;
import sharetrace.model.factory.TimeFactory;

@Buildable
public record Context(
    long referenceTime,
    long seed,
    RandomGenerator randomGenerator,
    @JsonIgnore Config config,
    @JsonIgnore TimeFactory userTimeFactory,
    @JsonIgnore TimeFactory systemTimeFactory,
    @JsonIgnore Map<String, String> mdc,
    @JsonIgnore RecordLogger propertyLogger,
    @JsonIgnore RecordLogger eventLogger) {

  @JsonIgnore
  public long getUserTime() {
    return userTimeFactory.getTime();
  }

  @JsonIgnore
  public long getSystemTime() {
    return systemTimeFactory.getTime();
  }

  public <T extends Event> void logEvent(Class<T> type, Supplier<T> record) {
    eventLogger.log(type, record);
  }

  public void logProperties(ExecutionProperties properties) {
    propertyLogger.log(ExecutionProperties.class, () -> properties);
  }
}
