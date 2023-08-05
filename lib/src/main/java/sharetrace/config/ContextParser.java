package sharetrace.config;

import com.typesafe.config.Config;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import java.time.Instant;
import java.time.InstantSource;
import java.time.ZoneId;
import java.util.Map;
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
    var mdc = new Object2ObjectOpenHashMap<String, String>(map.size());
    map.forEach((k, v) -> mdc.put(k, (String) v));
    return Object2ObjectMaps.unmodifiable(mdc);
  }

  private InstantSource getTimeSource(Config config) {
    var timezone = ZoneId.of(config.getString("timezone"));
    return InstantSource.system().withZone(timezone);
  }

  private long getSeed(Config config) {
    var seed = config.getString("seed");
    return seed.equals("any") ? IdFactory.newSeed() : Long.parseLong(seed);
  }

  private Instant getReferenceTime(Config config, InstantSource timeSource) {
    var referenceTime = config.getString("reference-time");
    return referenceTime.equals("now") ? timeSource.instant() : Instant.parse(referenceTime);
  }

  private RandomGenerator getRandomGenerator(Config config, long seed) {
    var className = config.getString("random-generator-factory");
    var factory = InstanceFactory.getInstance(className);
    return ((RandomGeneratorFactory) factory).getRandomGenerator(seed);
  }

  private Set<Class<? extends LogRecord>> getLoggable(Config config) {
    var loggable = config.getStringList("loggable");
    return loggable.stream()
        .<Class<? extends LogRecord>>map(ClassFactory::getClass)
        .collect(Collectors.collectingAndThen(ObjectOpenHashSet.toSet(), ObjectSets::unmodifiable));
  }
}
