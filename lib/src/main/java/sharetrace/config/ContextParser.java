package sharetrace.config;

import com.typesafe.config.Config;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
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
import sharetrace.model.factory.TimeFactory;

public record ContextParser(Config contextConfig) implements ConfigParser<Context> {

  @Override
  public Context parse(Config config) {
    var timeFactory = getTimeFactory(config);
    var seed = getSeed(config);
    var logged = getLogged(config);
    return ContextBuilder.create()
        .config(contextConfig)
        .timeFactory(timeFactory)
        .seed(seed)
        .referenceTime(getReferenceTime(config, timeFactory))
        .randomGenerator(getRandomGenerator(config, seed))
        .logged(logged)
        .propertyLogger(getPropertyLogger(logged))
        .eventLogger(getEventLogger(logged))
        .build();
  }

  private TimeFactory getTimeFactory(Config config) {
    var timezone = ZoneId.of(config.getString("timezone"));
    return TimeFactory.from(InstantSource.system().withZone(timezone));
  }

  private long getSeed(Config config) {
    var seed = config.getString("seed");
    var random = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
    return seed.equals("any") ? random : Integer.parseInt(seed);
  }

  private long getReferenceTime(Config config, TimeFactory timeFactory) {
    var string = config.getString("reference-time");
    return string.equals("now") ? timeFactory.getTime() : timeFactory.parseTime(string);
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
    return new RecordLogger(LoggerFactory.getLogger("EventLogger"), "e", logged);
  }

  private RecordLogger getPropertyLogger(Set<Class<? extends LogRecord>> logged) {
    return new RecordLogger(LoggerFactory.getLogger("PropertyLogger"), "p", logged);
  }
}
