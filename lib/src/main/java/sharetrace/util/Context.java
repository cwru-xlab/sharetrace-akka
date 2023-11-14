package sharetrace.util;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.typesafe.config.Config;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math3.random.RandomGenerator;
import sharetrace.Buildable;
import sharetrace.logging.LogRecord;
import sharetrace.logging.RecordLogger;
import sharetrace.model.Timestamp;

@Buildable
public record Context(
    @JsonIgnore Config config,
    TimeSource timeSource,
    Timestamp referenceTime,
    long seed,
    RandomGenerator randomGenerator,
    Set<Class<? extends LogRecord>> loggable,
    @JsonAnyGetter Map<String, String> mdc,
    @JsonIgnore RecordLogger eventsLogger,
    @JsonIgnore RecordLogger settingsLogger) {}
