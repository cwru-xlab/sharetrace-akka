package sharetrace.config;

import com.typesafe.config.Config;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.time.Instant;
import java.time.InstantSource;
import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.LoggerFactory;
import sharetrace.logging.LogRecord;
import sharetrace.logging.RecordLogger;
import sharetrace.model.Context;
import sharetrace.model.ContextBuilder;

public record ContextParser(Config contextConfig) implements ConfigParser<Context> {

  @Override
  public Context parse(Config config) {
    var timeSource = getTimeSource(config);
    var seed = getSeed(config);
    var logged = getLogged(config);
    return ContextBuilder.create()
        .config(contextConfig)
        .timeSource(timeSource)
        .seed(seed)
        .referenceTime(getReferenceTime(config, timeSource))
        .randomGenerator(getRandomGenerator(config, seed))
        .logged(logged)
        .propertyLogger(getPropertyLogger(logged))
        .eventLogger(getEventLogger(logged))
        .build();
  }

  private InstantSource getTimeSource(Config config) {
    var timezone = ZoneId.of(config.getString("timezone"));
    return InstantSource.system().withZone(timezone);
  }

  private long getSeed(Config config) {
    var seed = config.getString("seed");
    var random = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
    return seed.equals("any") ? random : Long.parseLong(seed);
  }

  private long getReferenceTime(Config config, InstantSource timeSource) {
    var string = config.getString("reference-time");
    return string.equals("now") ? timeSource.millis() : Instant.parse(string).toEpochMilli();
  }

  private RandomGenerator getRandomGenerator(Config config, long seed) {
    var className = config.getString("random-generator");
    return InstanceFactory.getInstance(RandomGenerator.class, className, seed);
  }

  private Set<Class<? extends LogRecord>> getLogged(Config config) {
    return config.getStringList("logged").stream()
        .map(className -> ClassFactory.getClass(LogRecord.class, className))
        .collect(ReferenceOpenHashSet.toSet());
  }

  private RecordLogger getEventLogger(Set<Class<? extends LogRecord>> logged) {
    return new RecordLogger(LoggerFactory.getLogger("EventLogger"), "event", logged);
  }

  private RecordLogger getPropertyLogger(Set<Class<? extends LogRecord>> logged) {
    return new RecordLogger(LoggerFactory.getLogger("PropertyLogger"), "property", logged);
  }
}
