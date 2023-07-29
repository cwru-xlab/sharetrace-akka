package sharetrace.app;

import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.config.Parsed;
import sharetrace.model.Parameters;
import sharetrace.util.Context;

public record NoiseRunner() implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {
    var parsed = Parsed.of(parameters, context);
    for (int i = 0; i < parsed.networks(); i++) {
      var network = parsed.network();
      var scoreFactory = parsed.scoreFactory().cached();
      for (var noise : parsed.randoms()) {
        RiskPropagationBuilder.<Integer>create()
            .context(context)
            .parameters(parameters)
            .riskScoreFactory(scoreFactory.withNoise(noise))
            .contactNetwork(network)
            .build()
            .run(parsed.iterations());
      }
    }
  }
}
