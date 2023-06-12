package sharetrace.experiment;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import sharetrace.experiment.data.RandomGeneratorFactory;
import sharetrace.experiment.data.RandomRiskScoreFactory;
import sharetrace.experiment.data.RandomTimestampFactory;
import sharetrace.experiment.data.RiskScoreFactory;
import sharetrace.experiment.data.TimestampFactory;
import sharetrace.graph.BarabasiAlbertTemporalNetworkFactory;
import sharetrace.graph.FileTemporalNetworkFactory;
import sharetrace.graph.GnmRandomTemporalNetworkFactory;
import sharetrace.graph.RandomRegularTemporalNetworkFactory;
import sharetrace.graph.ScaleFreeTemporalNetworkFactory;
import sharetrace.graph.TemporalNetworkFactory;
import sharetrace.graph.WattsStrogatzTemporalNetworkFactory;
import sharetrace.model.UserParameters;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.util.DistributedRandom;
import sharetrace.util.Identifiers;
import sharetrace.util.cache.CacheParameters;
import sharetrace.util.logging.LogRecord;

public final class ExperimentFactory {

  private ExperimentFactory() {}

  public static void create() {
    create(ConfigFactory.load().getConfig("sharetrace")).run();
  }

  public static void main(String[] args) {
    create();
  }

  @SuppressWarnings("unchecked")
  public static ExperimentState<?> create(Config config) {
    Context context = parseContext(config.getConfig("context"));
    UserParameters userParams = parseParameters(config.getConfig("user"));
    return ExperimentState.builder()
        .context(context)
        .userParameters(userParams)
        .cacheParameters(parseParameters(config.getConfig("user.cache"), context, userParams))
        .networkFactory(
            (TemporalNetworkFactory<Object>)
                parseNetworkFactory(config.getConfig("experiment.data.network"), context))
        .scoreFactory(
            (RiskScoreFactory<Object>)
                parseScoreFactory(config.getConfig("experiment.data.risk-scores"), context))
        .addAllLoggable(parseLoggable(config.getConfig("experiment")))
        .build();
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
    Object factory = newInstance(config, "random-generator-factory");
    return ((RandomGeneratorFactory) factory).getRandom(seed);
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

  private static TemporalNetworkFactory<?> parseNetworkFactory(Config config, Context context) {
    switch (config.getString("type")) {
      case ("gnm-random"):
        return GnmRandomTemporalNetworkFactory.builder()
            .nodes(config.getInt("gnm-random.nodes"))
            .edges(config.getInt("gnm-random.edges"))
            .timestampFactory(parseTimestampFactory(config, context))
            .random(context.random())
            .build();
      case ("random-regular"):
        return RandomRegularTemporalNetworkFactory.builder()
            .nodes(config.getInt("random-regular.nodes"))
            .degree(config.getInt("random-regular.degree"))
            .timestampFactory(parseTimestampFactory(config, context))
            .random(context.random())
            .build();
      case ("barabasi-albert"):
        return BarabasiAlbertTemporalNetworkFactory.builder()
            .initialNodes(config.getInt("barabasi-albert.initial-nodes"))
            .newEdges(config.getInt("barabasi-albert.new-edges"))
            .nodes(config.getInt("barabasi-albert.nodes"))
            .timestampFactory(parseTimestampFactory(config, context))
            .random(context.random())
            .build();
      case ("watts-strogatz"):
        return WattsStrogatzTemporalNetworkFactory.builder()
            .nodes(config.getInt("watts-strogatz.nodes"))
            .nearestNeighbors(config.getInt("watts-strogatz.nearest-neighbors"))
            .rewiringProbability(config.getDouble("watts-strogatz.rewiring-probability"))
            .timestampFactory(parseTimestampFactory(config, context))
            .random(context.random())
            .build();
      case ("scale-free"):
        config = config.getConfig("scale-free");
        return ScaleFreeTemporalNetworkFactory.builder()
            .nodes(config.getInt("scale-free.nodes"))
            .timestampFactory(parseTimestampFactory(config, context))
            .random(context.random())
            .build();
      case ("file"):
        return FileTemporalNetworkFactory.<String>builder()
            .path(Path.of(config.getString("file.path")))
            .delimiter(config.getString("file.delimiter"))
            .referenceTimestamp(context.referenceTimestamp())
            .nodeParser(String::valueOf)
            .build();
      default:
        throw new NoSuchElementException();
    }
  }

  private static RiskScoreFactory<?> parseScoreFactory(Config config, Context context) {
    Config distributionConfig = config.getConfig("value-distribution");
    return RandomRiskScoreFactory.<String>builder()
        .random(parseRandom(distributionConfig, context.random()))
        .timestampFactory(parseTimestampFactory(config, context))
        .build();
  }

  private static TimestampFactory parseTimestampFactory(Config config, Context context) {
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
