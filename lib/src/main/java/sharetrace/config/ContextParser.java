package sharetrace.config;

import com.typesafe.config.Config;
import java.time.Instant;
import java.time.InstantSource;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.apache.commons.math3.random.RandomGenerator;
import sharetrace.logging.LogRecord;
import sharetrace.util.Context;
import sharetrace.util.ContextBuilder;

public record ContextParser(Config contextConfig) implements ConfigParser<Context> {

  @Override
  public Context parse(Config config) {
    var timeSource = getTimeSource(config);
    var seed = getSeed(config);
    return ContextBuilder.create()
        .mdc(getMdc(config))
        .config(contextConfig)
        .timeSource(timeSource)
        .seed(seed)
        .referenceTime(getReferenceTime(config, timeSource))
        .randomGenerator(getRandomGenerator(config, seed))
        .loggable(getLoggable(config))
        .build();
  }

  private Map<String, String> getMdc(Config config) {
    var map = config.getObject("mdc").unwrapped();
    var mdc = new HashMap<String, String>(map.size());
    map.forEach((k, v) -> mdc.put(k, (String) v));
    return mdc;
  }

  private InstantSource getTimeSource(Config config) {
    var timezone = ZoneId.of(config.getString("timezone"));
    return InstantSource.system().withZone(timezone);
  }

  private long getSeed(Config config) {
    var seed = config.getString("seed");
    var random = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
    return seed.equals("any") ? random : Long.parseLong(seed);
  }

  private Instant getReferenceTime(Config config, InstantSource timeSource) {
    var referenceTime = config.getString("reference-time");
    return referenceTime.equals("now") ? timeSource.instant() : Instant.parse(referenceTime);
  }

  private RandomGenerator getRandomGenerator(Config config, long seed) {
    return InstanceFactory.getInstance(config.getString("random-generator"), seed);
  }

  private Set<Class<? extends LogRecord>> getLoggable(Config config) {
    return config.getStringList("loggable").stream()
        .<Class<? extends LogRecord>>map(ClassFactory::getClass)
        .peek(this::ensureIsLogRecord)
        .collect(Collectors.toSet());
  }

  @SuppressWarnings("SameParameterValue")
  private void ensureIsLogRecord(Class<?> cls) {
    var expected = LogRecord.class;
    if (!expected.isAssignableFrom(cls)) {
      throw new IllegalArgumentException(
          "%s must be of type %s".formatted(cls.getSimpleName(), expected.getSimpleName()));
    }
  }
}
