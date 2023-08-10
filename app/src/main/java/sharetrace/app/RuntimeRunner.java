package sharetrace.app;

import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.config.Parsed;
import sharetrace.model.Parameters;
import sharetrace.util.Context;
import sharetrace.util.KeyFactory;

public record RuntimeRunner() implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {
    var parsed = Parsed.of(parameters, context);
    var keyFactory = KeyFactory.autoIncrementing();
    for (var networkFactory : parsed.networkFactories()) {
      for (var i = 0; i < parsed.networks(); i++) {
        RiskPropagationBuilder.create()
            .context(context)
            .parameters(parameters)
            .riskScoreFactory(parsed.scoreFactory())
            .contactNetwork(networkFactory.getContactNetwork())
            .keyFactory(keyFactory)
            .build()
            .run(parsed.iterations());
      }
    }
  }
}
