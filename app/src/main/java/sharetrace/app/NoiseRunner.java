package sharetrace.app;

import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.config.Parsed;
import sharetrace.model.Parameters;
import sharetrace.util.Context;

public record NoiseRunner() implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {
    var parsers = Parsed.of(parameters, context);
    for (int i = 0; i < parsers.networkCount(); i++) {
      var network = parsers.network();
      var scoreFactory = parsers.scoreFactory().cached();
      for (var noise : parsers.randoms()) {
        RiskPropagationBuilder.<Integer>create()
            .context(context)
            .parameters(parameters)
            .riskScoreFactory(scoreFactory.withNoise(noise))
            .contactNetwork(network)
            .build()
            .run(parsers.iterationCount());
      }
    }
  }
}
