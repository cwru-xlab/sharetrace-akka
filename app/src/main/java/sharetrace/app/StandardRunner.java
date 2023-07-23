package sharetrace.app;

import java.time.Duration;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.graph.GnmRandomTemporalNetworkFactoryBuilder;
import sharetrace.graph.TemporalNetwork;
import sharetrace.model.Parameters;
import sharetrace.model.factory.RandomRiskScoreFactoryBuilder;
import sharetrace.model.factory.RandomTimeFactoryBuilder;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.model.factory.TimeFactory;
import sharetrace.util.Context;
import sharetrace.util.DistributedRandom;

public record StandardRunner() implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {
    RiskPropagationBuilder.<Integer>create()
        .context(context)
        .parameters(parameters)
        .scoreFactory(scoreFactory(parameters, context))
        .contactNetwork(temporalNetwork(context))
        .build()
        .run();
  }

  private TemporalNetwork<Integer> temporalNetwork(Context context) {
    return GnmRandomTemporalNetworkFactoryBuilder.create()
        .nodes(100)
        .edges(200)
        .timeFactory(timeFactory(context))
        .randomGenerator(context.randomGenerator())
        .build()
        .getNetwork();
  }

  private RiskScoreFactory<Integer> scoreFactory(Parameters parameters, Context context) {
    return RandomRiskScoreFactoryBuilder.<Integer>create()
        .random(random(context))
        .timeFactory(timeFactory(context))
        .scoreExpiry(parameters.scoreExpiry())
        .build();
  }

  private TimeFactory timeFactory(Context context) {
    return RandomTimeFactoryBuilder.create()
        .timestamp(context.referenceTime())
        .range(Duration.ofDays(14))
        .random(random(context))
        .build();
  }

  private DistributedRandom random(Context context) {
    return DistributedRandom.from(new UniformRealDistribution(context.randomGenerator(), 0, 1));
  }
}
