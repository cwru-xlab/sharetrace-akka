package sharetrace.util;

import ch.qos.logback.core.spi.PropertyContainer;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.typesafe.config.Config;
import java.nio.file.Path;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.LoggerFactory;
import sharetrace.Buildable;
import sharetrace.logging.LogRecord;
import sharetrace.logging.RecordLogger;
import sharetrace.logging.ToClassNameSerializer;
import sharetrace.logging.event.EventRecord;
import sharetrace.logging.metric.MetricRecord;
import sharetrace.logging.setting.SettingsRecord;

@Buildable
public record Context(
    @JsonIgnore Config config,
    @JsonSerialize(using = ToStringSerializer.class) InstantSource timeSource,
    Instant referenceTime,
    long seed,
    @JsonSerialize(using = ToClassNameSerializer.class) RandomGenerator randomGenerator,
    Set<Class<? extends LogRecord>> loggable,
    @JsonAnyGetter Map<String, String> mdc) {

  public RecordLogger<EventRecord> eventsLogger() {
    return new RecordLogger<>("EventsLogger", "event", loggable);
  }

  public RecordLogger<MetricRecord> metricsLogger() {
    return new RecordLogger<>("MetricsLogger", "metric", loggable);
  }

  public RecordLogger<SettingsRecord> settingsLogger() {
    return new RecordLogger<>("SettingsLogger", "setting", loggable);
  }

  public Path logDirectory() {
    PropertyContainer properties = (PropertyContainer) LoggerFactory.getILoggerFactory();
    return Path.of(properties.getProperty("logDirectory"));
  }
}
