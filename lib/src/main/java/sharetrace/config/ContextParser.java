package sharetrace.config;

import com.typesafe.config.Config;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.LoggerFactory;
import sharetrace.logging.LogRecord;
import sharetrace.logging.RecordLogger;
import sharetrace.logging.RecordLoggerBuilder;
import sharetrace.model.Context;
import sharetrace.model.ContextBuilder;
import sharetrace.model.factory.TimeFactory;

public record ContextParser(Config contextConfig) implements ConfigParser<Context> {

  @Override
  public Context parse(Config config) {
    var seed = getSeed(config);
    var referenceTime = getReferenceTime(config);
    return ContextBuilder.create()
        .config(contextConfig)
        .seed(seed)
        .randomGenerator(getRandomGenerator(config, seed))
        .eventLogger(getEventLogger(config))
        .propertyLogger(getPropertyLogger(config))
        .systemTimeFactory(getSystemTimeFactory())
        .userTimeFactory(getUserTimeFactory(config, referenceTime))
        .referenceTime(TimeFactory.fixed(referenceTime).getTime())
        .build();
  }

  private long getSeed(Config config) {
    var seed = config.getString("seed");
    var random = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
    return seed.equals("any") ? random : Integer.parseInt(seed);
  }

  private RandomGenerator getRandomGenerator(Config config, long seed) {
    var className = config.getString("random-generator");
    var generator = ClassFactory.getInstance(RandomGenerator.class, className);
    generator.setSeed(seed);
    return generator;
  }

  private Instant getReferenceTime(Config config) {
    var string = config.getString("reference-time");
    return string.equals("now") ? Instant.now() : Instant.parse(string);
  }

  private TimeFactory getUserTimeFactory(Config config, Instant referenceTime) {
    var type = config.getString("user-time");
    return switch (type) {
      case "fixed" -> TimeFactory.fixed(referenceTime);
      case "system" -> getSystemTimeFactory();
      default -> throw new IllegalArgumentException(type);
    };
  }

  private TimeFactory getSystemTimeFactory() {
    return TimeFactory.system();
  }

  private RecordLogger getEventLogger(Config config) {
    return RecordLoggerBuilder.create()
        .logger(LoggerFactory.getLogger("EventLogger"))
        .key("e")
        .logged(getLogged(config))
        .timeFactory(getSystemTimeFactory())
        .build();
  }

  private RecordLogger getPropertyLogger(Config config) {
    return RecordLoggerBuilder.create()
        .logger(LoggerFactory.getLogger("PropertyLogger"))
        .key("p")
        .logged(getLogged(config))
        .timeFactory(getSystemTimeFactory())
        .build();
  }

  private Set<Class<? extends LogRecord>> getLogged(Config config) {
    return config.getStringList("logged").stream()
        .map(className -> ClassFactory.getClass(LogRecord.class, className))
        .collect(ReferenceOpenHashSet.toSet());
  }
}
