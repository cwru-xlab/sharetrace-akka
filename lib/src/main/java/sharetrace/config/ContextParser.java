package sharetrace.config;

import com.typesafe.config.Config;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.LoggerFactory;
import sharetrace.logging.LogRecord;
import sharetrace.logging.RecordLogger;
import sharetrace.model.Context;
import sharetrace.model.ContextBuilder;
import sharetrace.model.factory.TimeFactory;

public record ContextParser(Config contextConfig) implements ConfigParser<Context> {

  @Override
  public Context parse(Config config) {
    var seed = getSeed(config);
    var logged = getLogged(config);
    var dataTimeFactory = getDataTimeFactory(config);
    return ContextBuilder.create()
        .config(contextConfig)
        .systemTimeFactory(getSystemTimeFactory())
        .dataTimeFactory(dataTimeFactory)
        .seed(seed)
        .referenceTime(dataTimeFactory.getTime())
        .randomGenerator(getRandomGenerator(config, seed))
        .eventLogger(getEventLogger(logged))
        .propertyLogger(getPropertyLogger(logged))
        .build();
  }

  private long getSeed(Config config) {
    var seed = config.getString("seed");
    var random = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
    return seed.equals("any") ? random : Integer.parseInt(seed);
  }

  private Set<Class<? extends LogRecord>> getLogged(Config config) {
    return config.getStringList("logged").stream()
        .map(className -> ClassFactory.getClass(LogRecord.class, className))
        .collect(ReferenceOpenHashSet.toSet());
  }

  private TimeFactory getDataTimeFactory(Config config) {
    var referenceTime = getReferenceTime(config);
    var clock = config.getString("data-clock");
    return switch (clock) {
      case "fixed" -> TimeFactory.from(InstantSource.fixed(referenceTime));
      case "system" -> getSystemTimeFactory();
      default -> throw new IllegalArgumentException(clock);
    };
  }

  private Instant getReferenceTime(Config config) {
    var string = config.getString("reference-time");
    return string.equals("now") ? Instant.now() : Instant.parse(string);
  }

  private TimeFactory getSystemTimeFactory() {
    return TimeFactory.from(InstantSource.system());
  }

  private RandomGenerator getRandomGenerator(Config config, long seed) {
    var className = config.getString("random-generator");
    return InstanceFactory.getInstance(RandomGenerator.class, className, seed);
  }

  private RecordLogger getEventLogger(Set<Class<? extends LogRecord>> logged) {
    return new RecordLogger(LoggerFactory.getLogger("EventLogger"), "e", logged);
  }

  private RecordLogger getPropertyLogger(Set<Class<? extends LogRecord>> logged) {
    return new RecordLogger(LoggerFactory.getLogger("PropertyLogger"), "p", logged);
  }
}
