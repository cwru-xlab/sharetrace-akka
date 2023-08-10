package sharetrace.app;

import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.config.Parsed;
import sharetrace.model.Parameters;
import sharetrace.util.Context;
import sharetrace.util.KeyFactory;

public record NoiseRunner() implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {
    var parsed = Parsed.of(parameters, context);
    var keyFactory = KeyFactory.autoIncrementing();
    for (var i = 0; i < parsed.networks(); i++) {
      var network = parsed.network();
      var scoreFactory = parsed.scoreFactory().cached();
      for (var noise : parsed.randoms()) {
        RiskPropagationBuilder.create()
            .context(context)
            .parameters(parameters)
            .riskScoreFactory(scoreFactory.withNoise(noise))
            .contactNetwork(network)
            .keyFactory(keyFactory)
            .build()
            .run(parsed.iterations());
      }
    }
  }
}
