package sharetrace.app;

import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.config.AppConfig;
import sharetrace.model.Context;
import sharetrace.model.Parameters;
import sharetrace.model.ParametersBuilder;

public class ParametersExperimentRunner implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {
    var config = AppConfig.of(parameters, context);
    var keyFactory = config.getKeyFactory();
    for (var repeats : config.getIterations()) {
      var scoreFactory = config.getScoreFactory();
      var networkFactory = config.getNetworkFactory();
      for (double sc : config.getSendCoefficients()) {
        RiskPropagationBuilder.create()
            .context(context)
            .parameters(ParametersBuilder.from(parameters).withSendCoefficient(sc))
            .scoreFactory(scoreFactory)
            .networkFactory(networkFactory)
            .keyFactory(keyFactory)
            .build()
            .run(repeats);
      }
    }
  }
}
