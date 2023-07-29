package sharetrace.app;

import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.config.Parsed;
import sharetrace.model.Parameters;
import sharetrace.util.Context;

public record RuntimeRunner() implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {
    var parsed = Parsed.of(parameters, context);
    for (var networkFactory : parsed.networkFactories()) {
      for (var i = 0; i < parsed.networkCount(); i++) {
        RiskPropagationBuilder.<Integer>create()
            .context(context)
            .parameters(parameters)
            .riskScoreFactory(parsed.scoreFactory())
            .contactNetwork(networkFactory.getContactNetwork())
            .build()
            .run(parsed.iterationCount());
      }
    }
  }
}
