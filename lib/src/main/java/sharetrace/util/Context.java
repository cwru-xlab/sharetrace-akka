package sharetrace.util;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.typesafe.config.Config;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math3.random.RandomGenerator;
import sharetrace.Buildable;
import sharetrace.logging.LogRecord;
import sharetrace.logging.RecordLogger;
import sharetrace.logging.ToClassNameSerializer;
import sharetrace.logging.event.Event;
import sharetrace.logging.setting.Settings;

@Buildable
public record Context(
    Config config,
    InstantSource timeSource,
    Instant referenceTime,
    long seed,
    RandomGenerator randomGenerator,
    Set<Class<? extends LogRecord>> loggable,
    Map<String, String> mdc) {

  @Override
  @JsonIgnore
  public Config config() {
    return config;
  }

  @Override
  @JsonSerialize(using = ToStringSerializer.class)
  public InstantSource timeSource() {
    return timeSource;
  }

  @Override
  @JsonSerialize(using = ToClassNameSerializer.class)
  public RandomGenerator randomGenerator() {
    return randomGenerator;
  }

  @Override
  @JsonAnyGetter
  public Map<String, String> mdc() {
    return mdc;
  }

  public RecordLogger<Event> eventsLogger() {
    return new RecordLogger<>("EventsLogger", Event.key(), loggable);
  }

  public RecordLogger<Settings> settingsLogger() {
    return new RecordLogger<>("SettingsLogger", Settings.key(), loggable);
  }
}
