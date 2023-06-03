package io.sharetrace.experiment;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.sharetrace.experiment.data.*;
import io.sharetrace.graph.FileTemporalNetworkGenerator;
import io.sharetrace.graph.ScaleFreeTemporalNetworkFactory;
import io.sharetrace.graph.TemporalEdge;
import io.sharetrace.graph.TemporalNetworkFactory;
import io.sharetrace.graph.TemporalNetworkFactoryHelper;
import io.sharetrace.model.UserParameters;
import io.sharetrace.model.message.RiskScoreMessage;
import io.sharetrace.util.DistributedRandom;
import io.sharetrace.util.Identifiers;
import io.sharetrace.util.cache.CacheParameters;
import io.sharetrace.util.logging.LogRecord;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.RandomRegularGraphGenerator;
import org.jgrapht.generate.ScaleFreeGraphGenerator;
import org.jgrapht.generate.WattsStrogatzGraphGenerator;

public final class ExperimentFactory {

  private ExperimentFactory() {}

  public static void create() {
    create(ConfigFactory.load().getConfig("sharetrace")).run();
  }

  public static void main(String[] args) {
    create();
  }

  public static ExperimentState<?> create(Config config) {
    Context context = parseContext(config.getConfig("context"));
    UserParameters userParams = parseParameters(config.getConfig("user"));
    TemporalNetworkFactory<String> networkFactory =
        parseNetworkFactory(config.getConfig("experiment.data.network"), context);
    return ExperimentState.<String>builder()
        .context(context)
        .userParameters(userParams)
        .cacheParameters(parseParameters(config.getConfig("user.cache"), context, userParams))
        .networkFactory(networkFactory)
        .scoreFactory(parseScoreFactory(config.getConfig("experiment.data.risk-scores"), context))
        .addAllLoggable(parseLoggable(config.getConfig("experiment")))
        .build();
  }

  private static Supplier<String> vertexFactory() {
    AtomicInteger counter = new AtomicInteger(0);
    return () -> String.valueOf(counter.getAndIncrement());
  }

  private static Set<Class<? extends LogRecord>> parseLoggable(final Config config) {
    return Stream.of("standard", "parameters", "noise", "runtime")
        .filter(type -> type.equals(config.getString("type")))
        .findFirst()
        .map(config::getConfig)
        .map(ExperimentFactory::loadLoggable)
        .orElseThrow();
  }

  private static Set<Class<? extends LogRecord>> loadLoggable(Config config) {
    Set<Class<? extends LogRecord>> loggable = new HashSet<>();
    for (String className : config.getStringList("loggable")) {
      loggable.add(loadClass(className));
    }
    return loggable;
  }

  private static Context parseContext(Config config) {
    Clock clock = parseClock(config);
    long seed = parseSeed(config);
    return Context.builder()
        .clock(clock)
        .referenceTimestamp(parseReferenceTime(config, clock))
        .seed(seed)
        .random(parseRandom(config, seed))
        .build();
  }

  private static Clock parseClock(Config config) {
    String timezone = config.getString("timezone");
    return Clock.system(ZoneId.of(timezone));
  }

  private static Instant parseReferenceTime(Config config, Clock clock) {
    String referenceTime = config.getString("reference-time");
    return referenceTime.equals("now") ? Instant.now(clock) : Instant.parse(referenceTime);
  }

  private static long parseSeed(Config config) {
    String seed = config.getString("seed");
    return seed.equals("any") ? Identifiers.newLong() : Long.parseLong(seed);
  }

  private static RandomGenerator parseRandom(Config config, long seed) {
    return ((RandomGeneratorFactory) newInstance(config, "random-generator-factory"))
        .getRandom(seed);
  }

  private static UserParameters parseParameters(Config config) {
    return UserParameters.builder()
        .contactExpiry(config.getDuration("contact-expiry"))
        .scoreExpiry(config.getDuration("score-expiry"))
        .idleTimeout(config.getDuration("idle-timeout"))
        .timeBuffer(config.getDuration("time-buffer"))
        .sendCoefficient((float) config.getDouble("send-coefficient"))
        .transmissionRate((float) config.getDouble("transmission-rate"))
        .tolerance((float) config.getDouble("tolerance"))
        .build();
  }

  private static CacheParameters<RiskScoreMessage> parseParameters(
      Config config, Context context, UserParameters userParameters) {
    return CacheParameters.<RiskScoreMessage>builder()
        .interval(config.getDuration("interval"))
        .intervals(config.getLong("intervals"))
        .forwardIntervals(config.getLong("forward-intervals"))
        .refreshPeriod(config.getDuration("refresh-period"))
        .clock(context.clock())
        .mergeStrategy(newInstance(config, "merge-strategy", userParameters))
        .build();
  }

  private static TemporalNetworkFactory<String> parseNetworkFactory(
      Config config, Context context) {
    //    GraphGenerator<String, TemporalEdge, String> generator = parseGraphGenerator(config,
    // context);
    //    if (generator instanceof FileTemporalNetworkGenerator) {
    //      return (TemporalNetworkFactory<String>) generator;
    //    } else {
    //      TimeFactory timeFactory = parseTimeFactory(config, context);
    //      return ForwardingTemporalNetworkGenerator.<String>builder()
    //          .delegate(generator)
    //          .timeFactory((v1, v2) -> timeFactory.get())
    //          .build();
    //    }
    return ScaleFreeTemporalNetworkFactory.<String>builder()
        .randomGenerator(context.random())
        .vertices(100)
        .vertexFactory(TemporalNetworkFactoryHelper.stringVertexFactory())
        .build();
  }

  private static GraphGenerator<String, TemporalEdge, String> parseGraphGenerator(
      Config config, Context context) {
    switch (config.getString("type")) {
      case ("gnm-random"):
        config = config.getConfig("gnm-random");
        boolean allowLoops = false;
        boolean multipleEdges = false;
        return new GnmRandomGraphGenerator<>(
            config.getInt("vertices"),
            config.getInt("edges"),
            context.seed(),
            allowLoops,
            multipleEdges);
      case ("random-regular"):
        config = config.getConfig("random-regular");
        return new RandomRegularGraphGenerator<>(
            config.getInt("vertices"), config.getInt("degree"), context.seed());
      case ("barabasi-albert"):
        config = config.getConfig("barabasi-albert");
        return new BarabasiAlbertGraphGenerator<>(
            config.getInt("initial-vertices"),
            config.getInt("new-edges"),
            config.getInt("vertices"),
            context.seed());
      case ("watts-strogatz"):
        config = config.getConfig("watts-strogatz");
        return new WattsStrogatzGraphGenerator<>(
            config.getInt("vertices"),
            config.getInt("nearest-neighbors"),
            config.getDouble("rewiring-probability"),
            context.seed());
      case ("scale-free"):
        config = config.getConfig("scale-free");
        return new ScaleFreeGraphGenerator<>(config.getInt("vertices"), context.seed());
      case ("file"):
        config = config.getConfig("file");
        return FileTemporalNetworkGenerator.<String>builder()
            .path(Path.of(config.getString("file.path")))
            .delimiter(config.getString("file.delimiter"))
            .referenceTimestamp(context.referenceTimestamp())
            .vertexParser(String::valueOf)
            .build();
      default:
        throw new NoSuchElementException();
    }
  }

  private static RiskScoreFactory<String> parseScoreFactory(Config config, Context context) {
    Config distributionConfig = config.getConfig("value-distribution");
    return RandomRiskScoreFactory.<String>builder()
        .random(parseRandom(distributionConfig, context.random()))
        .timestampFactory(parseTimeFactory(config, context))
        .build();
  }

  private static TimestampFactory parseTimeFactory(Config config, Context context) {
    Config distributionConfig = config.getConfig("look-back-distribution");
    return RandomTimestampFactory.builder()
        .referenceTimestamp(context.referenceTimestamp())
        .backwardRange(config.getDuration("backward-range"))
        .random(parseRandom(distributionConfig, context.random()))
        .build();
  }

  private static DistributedRandom parseRandom(Config config, RandomGenerator generator) {
    switch (config.getString("type")) {
      case ("normal"):
        config = config.getConfig("normal");
        double mean = config.getDouble("mean");
        double standardDeviation = config.getDouble("standard-deviation");
        return DistributedRandom.from(new NormalDistribution(generator, mean, standardDeviation));
      case ("beta"):
        config = config.getConfig("beta");
        double alpha = config.getDouble("alpha");
        double beta = config.getDouble("beta");
        return DistributedRandom.from(new BetaDistribution(generator, alpha, beta));
      case ("uniform"):
        config = config.getConfig("uniform");
        double lowerBound = config.getDouble("lower-bound");
        double upperBound = config.getDouble("upper-bound");
        return DistributedRandom.from(
            new UniformRealDistribution(generator, lowerBound, upperBound));
      default:
        throw new NoSuchElementException();
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T newInstance(Config config, String configPath, Object... parameters) {
    Class<?>[] types = Arrays.stream(parameters).map(Object::getClass).toArray(Class<?>[]::new);
    String className = config.getString(configPath);
    try {
      return (T) loadClass(className).getConstructor(types).newInstance(parameters);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> Class<T> loadClass(String name) {
    try {
      return (Class<T>) ClassLoader.getSystemClassLoader().loadClass(name);
    } catch (ClassNotFoundException exception) {
      throw new RuntimeException(exception);
    }
  }
}
