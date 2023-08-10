package sharetrace.app;

import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.config.Parsed;
import sharetrace.model.Parameters;
import sharetrace.util.Context;
import sharetrace.util.KeyFactory;

public record StandardRunner() implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {
    var parsed = Parsed.of(parameters, context);
    var keyFactory = KeyFactory.autoIncrementing();
    RiskPropagationBuilder.create()
        .context(context)
        .parameters(parameters)
        .riskScoreFactory(parsed.scoreFactory())
        .contactNetwork(parsed.network())
        .keyFactory(keyFactory)
        .build()
        .run(parsed.iterations());
  }
}
