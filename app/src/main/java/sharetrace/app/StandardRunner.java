package sharetrace.app;

import sharetrace.algorithm.KeyFactory;
import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.config.Parsed;
import sharetrace.model.Context;
import sharetrace.model.Parameters;

public record StandardRunner() implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {
    var parsed = Parsed.of(parameters, context);
    RiskPropagationBuilder.create()
        .context(context)
        .parameters(parameters)
        .riskScoreFactory(parsed.scoreFactory())
        .contactNetwork(parsed.network())
        .keyFactory(KeyFactory.autoIncrementing())
        .build()
        .run(parsed.iterations());
  }
}
