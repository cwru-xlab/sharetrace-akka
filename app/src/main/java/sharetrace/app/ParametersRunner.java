package sharetrace.app;

import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.config.Parsed;
import sharetrace.model.Parameters;
import sharetrace.model.ParametersBuilder;
import sharetrace.util.Context;

public record ParametersRunner() implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {
    var parsed = Parsed.of(parameters, context);
    for (var i = 0; i < parsed.networks(); i++) {
      var network = parsed.network();
      var scoreFactory = parsed.scoreFactory().cached();
      for (var transmissionRate : parsed.transmissionRates()) {
        for (var sendCoefficient : parsed.sendCoefficients()) {
          RiskPropagationBuilder.<Integer>create()
              .context(context)
              .parameters(
                  ParametersBuilder.builder(parameters)
                      .transmissionRate(transmissionRate)
                      .sendCoefficient(sendCoefficient)
                      .build())
              .riskScoreFactory(scoreFactory)
              .contactNetwork(network)
              .build()
              .run(parsed.iterations());
        }
      }
    }
  }
}
