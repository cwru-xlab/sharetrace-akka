package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.typesafe.config.Config;
import java.time.InstantSource;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math3.random.RandomGenerator;
import sharetrace.Buildable;
import sharetrace.logging.LogRecord;
import sharetrace.logging.RecordLogger;

@Buildable
public record Context(
    @JsonIgnore Config config,
    InstantSource timeSource,
    long referenceTime,
    long seed,
    RandomGenerator randomGenerator,
    Set<Class<? extends LogRecord>> logged,
    Map<String, String> tags,
    @JsonIgnore Map<String, String> mdc,
    @JsonIgnore RecordLogger propertyLogger,
    @JsonIgnore RecordLogger eventLogger) {}
