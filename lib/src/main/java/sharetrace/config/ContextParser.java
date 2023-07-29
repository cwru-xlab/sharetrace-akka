package sharetrace.config;

import com.typesafe.config.Config;
import java.time.Instant;
import java.time.InstantSource;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.math3.random.RandomGenerator;
import sharetrace.logging.LogRecord;
import sharetrace.model.factory.RandomGeneratorFactory;
import sharetrace.util.Context;
import sharetrace.util.ContextBuilder;
import sharetrace.util.IdFactory;

public record ContextParser(Config contextConfig) implements ConfigParser<Context> {

  @Override
  public Context parse(Config config) {
    var timeSource = timeSource(config);
    var seed = seed(config);
    return ContextBuilder.create()
        .config(contextConfig)
        .timeSource(timeSource)
        .seed(seed)
        .referenceTime(referenceTime(config, timeSource))
        .randomGenerator(randomGenerator(config, seed))
        .loggable(loggable(config))
        .build();
  }

  private InstantSource timeSource(Config config) {
    var timezone = ZoneId.of(config.getString("timezone"));
    return InstantSource.system().withZone(timezone);
  }

  private long seed(Config config) {
    var seed = config.getString("seed");
    return seed.equals("any") ? IdFactory.newLong() : Long.parseLong(seed);
  }

  private Instant referenceTime(Config config, InstantSource timeSource) {
    var referenceTime = config.getString("reference-time");
    return referenceTime.equals("now") ? timeSource.instant() : Instant.parse(referenceTime);
  }

  private RandomGenerator randomGenerator(Config config, long seed) {
    var factory = new InstanceParser<>("random-generator-factory").parse(config);
    return ((RandomGeneratorFactory) factory).getRandomGenerator(seed);
  }

  private Set<Class<? extends LogRecord>> loggable(Config config) {
    var classFactory = new ClassFactory();
    return config.getStringList("loggable").stream()
        .<Class<? extends LogRecord>>map(classFactory::getClass)
        .collect(Collectors.toUnmodifiableSet());
  }
}
