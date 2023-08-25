package sharetrace.util;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.typesafe.config.Config;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sharetrace.Buildable;
import sharetrace.logging.LogRecord;
import sharetrace.logging.RecordLogger;
import sharetrace.logging.Settings;
import sharetrace.logging.event.Event;

@Buildable
public record Context(
    @JsonIgnore Config config,
    InstantSource timeSource,
    Instant referenceTime,
    long seed,
    RandomGenerator randomGenerator,
    Set<Class<? extends LogRecord>> loggable,
    @JsonAnyGetter Map<String, String> mdc) {

  private static final Logger EVENTS_LOGGER = LoggerFactory.getLogger("EventsLogger");
  private static final Logger SETTINGS_LOGGER = LoggerFactory.getLogger("SettingsLogger");

  public RecordLogger<Event> eventsLogger() {
    return new RecordLogger<>(EVENTS_LOGGER, "event", loggable);
  }

  public RecordLogger<Settings> settingsLogger() {
    return new RecordLogger<>(SETTINGS_LOGGER, "setting", loggable);
  }
}
