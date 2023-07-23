package sharetrace.config;

import com.typesafe.config.Config;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import sharetrace.graph.BarabasiAlbertTemporalNetworkFactoryBuilder;
import sharetrace.graph.FileTemporalNetworkFactoryBuilder;
import sharetrace.graph.GnmRandomTemporalNetworkFactoryBuilder;
import sharetrace.graph.RandomRegularTemporalNetworkFactoryBuilder;
import sharetrace.graph.ScaleFreeTemporalNetworkFactoryBuilder;
import sharetrace.graph.TemporalNetworkFactory;
import sharetrace.graph.WattsStrogatzTemporalNetworkFactoryBuilder;
import sharetrace.model.Parameters;
import sharetrace.model.factory.RandomRiskScoreFactoryBuilder;
import sharetrace.model.factory.RandomTimeFactoryBuilder;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.model.factory.TimeFactory;
import sharetrace.util.Context;
import sharetrace.util.DistributedRandom;

public class OtherFactory {

  private static TemporalNetworkFactory<?> parseNetworkFactory(Config config, Context context) {
    return switch (config.getString("type")) {
      case ("gnm-random") -> GnmRandomTemporalNetworkFactoryBuilder.create()
          .nodes(config.getInt("gnm-random.nodes"))
          .edges(config.getInt("gnm-random.edges"))
          .timeFactory(timeFactory(config, context))
          .randomGenerator(context.randomGenerator())
          .build();
      case ("random-regular") -> RandomRegularTemporalNetworkFactoryBuilder.create()
          .nodes(config.getInt("random-regular.nodes"))
          .degree(config.getInt("random-regular.degree"))
          .timeFactory(timeFactory(config, context))
          .randomGenerator(context.randomGenerator())
          .build();
      case ("barabasi-albert") -> BarabasiAlbertTemporalNetworkFactoryBuilder.create()
          .initialNodes(config.getInt("barabasi-albert.initial-nodes"))
          .newEdges(config.getInt("barabasi-albert.new-edges"))
          .nodes(config.getInt("barabasi-albert.nodes"))
          .timeFactory(timeFactory(config, context))
          .randomGenerator(context.randomGenerator())
          .build();
      case ("watts-strogatz") -> WattsStrogatzTemporalNetworkFactoryBuilder.create()
          .nodes(config.getInt("watts-strogatz.nodes"))
          .nearestNeighbors(config.getInt("watts-strogatz.nearest-neighbors"))
          .rewiringProbability(config.getDouble("watts-strogatz.rewiring-probability"))
          .timeFactory(timeFactory(config, context))
          .randomGenerator(context.randomGenerator())
          .build();
      case ("scale-free") -> ScaleFreeTemporalNetworkFactoryBuilder.create()
          .nodes(config.getInt("scale-free.nodes"))
          .timeFactory(timeFactory(config, context))
          .randomGenerator(context.randomGenerator())
          .build();
      case ("file") -> FileTemporalNetworkFactoryBuilder.<String>create()
          .path(Path.of(config.getString("file.path")))
          .delimiter(config.getString("file.delimiter"))
          .timestamp(context.referenceTime())
          .nodeParser(String::valueOf)
          .build();
      default -> throw new NoSuchElementException();
    };
  }

  private static RiskScoreFactory<?> scoreFactory(
      Config config, Parameters parameters, Context context) {
    return RandomRiskScoreFactoryBuilder.<String>create()
        .random(random(config.getConfig("value-distribution"), context.randomGenerator()))
        .timeFactory(timeFactory(config, context))
        .scoreExpiry(parameters.scoreExpiry())
        .build();
  }

  private static TimeFactory timeFactory(Config config, Context context) {
    return RandomTimeFactoryBuilder.create()
        .timestamp(context.referenceTime())
        .range(config.getDuration("time-range"))
        .random(random(config.getConfig("time-distribution"), context.randomGenerator()))
        .build();
  }

  private static DistributedRandom random(Config config, RandomGenerator generator) {
    switch (config.getString("type")) {
      case ("normal") -> {
        var mean = config.getDouble("normal.mean");
        var standardDeviation = config.getDouble("normal.standard-deviation");
        return DistributedRandom.from(new NormalDistribution(generator, mean, standardDeviation));
      }
      case ("beta") -> {
        var alpha = config.getDouble("beta.alpha");
        var beta = config.getDouble("beta.beta");
        return DistributedRandom.from(new BetaDistribution(generator, alpha, beta));
      }
      case ("uniform") -> {
        var lowerBound = config.getDouble("uniform.lower-bound");
        var upperBound = config.getDouble("uniform.upper-bound");
        return DistributedRandom.from(
            new UniformRealDistribution(generator, lowerBound, upperBound));
      }
      default -> throw new NoSuchElementException();
    }
  }
}
