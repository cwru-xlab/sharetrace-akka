package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.typesafe.config.Config;
import java.util.Map;
import org.apache.commons.math3.random.RandomGenerator;
import sharetrace.Buildable;
import sharetrace.logging.RecordLogger;
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
    @JsonIgnore RecordLogger eventLogger) {}
