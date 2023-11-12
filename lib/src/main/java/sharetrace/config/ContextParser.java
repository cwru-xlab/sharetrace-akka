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
import org.slf4j.LoggerFactory;
import sharetrace.logging.LogRecord;
import sharetrace.logging.RecordLogger;
import sharetrace.util.Context;
import sharetrace.util.ContextBuilder;

public record ContextParser(Config contextConfig) implements ConfigParser<Context> {

  @Override
  public Context parse(Config config) {
    var timeSource = getTimeSource(config);
    var seed = getSeed(config);
    var loggable = getLoggable(config);
    return ContextBuilder.create()
        .mdc(getMdc(config))
        .config(contextConfig)
        .timeSource(timeSource)
        .seed(seed)
        .referenceTime(getReferenceTime(config, timeSource))
        .randomGenerator(getRandomGenerator(config, seed))
        .loggable(loggable)
        .eventsLogger(getEventsLogger(loggable))
        .settingsLogger(getSettingsLogger(loggable))
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
    var className = config.getString("random-generator");
    return InstanceFactory.getInstance(className, RandomGenerator.class, seed);
  }

  private Set<Class<? extends LogRecord>> getLoggable(Config config) {
    return config.getStringList("loggable").stream()
        .map(className -> ClassFactory.getClass(className, LogRecord.class))
        .collect(Collectors.toSet());
  }

  private RecordLogger getEventsLogger(Set<Class<? extends LogRecord>> loggable) {
    return new RecordLogger(LoggerFactory.getLogger("EventsLogger"), "event", loggable);
  }

  private RecordLogger getSettingsLogger(Set<Class<? extends LogRecord>> loggable) {
    return new RecordLogger(LoggerFactory.getLogger("SettingsLogger"), "setting", loggable);
  }
}
