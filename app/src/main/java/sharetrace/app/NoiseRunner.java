package sharetrace.app;

import java.util.List;
import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.graph.TemporalNetworkFactory;
import sharetrace.model.Parameters;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.util.Context;
import sharetrace.util.DistributedRandom;

public record NoiseRunner<K>() implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {

    var iterations = 1;
    var networks = 1;
    TemporalNetworkFactory<K> networkFactory = null;
    RiskScoreFactory<K> scoreFactory = null;
    var noises = List.<DistributedRandom>of();
    for (int i = 0; i < networks; i++) {
      var network = networkFactory.getNetwork();
      for (var noise : noises) {
        RiskPropagationBuilder.<K>create()
            .context(context)
            .parameters(parameters)
            .scoreFactory(scoreFactory.cached().withNoise(noise))
            .contactNetwork(network)
            .build()
            .run(iterations);
      }
    }
  }
}
