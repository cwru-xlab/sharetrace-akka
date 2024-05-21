package sharetrace.app;

import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.config.AppConfig;
import sharetrace.model.Context;
import sharetrace.model.Parameters;

abstract class AbstractParameterRunner<V> implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {
    var config = AppConfig.of(parameters, context);
    var keyFactory = config.getKeyFactory();
    for (var repeats : config.getIterations()) {
      var scoreFactory = config.getScoreFactory();
      var networkFactory = config.getNetworkFactory();
      for (var value : parameterValues(config)) {
        RiskPropagationBuilder.create()
            .context(context)
            .parameters(updateParameters(parameters, value))
            .scoreFactory(scoreFactory)
            .networkFactory(networkFactory)
            .keyFactory(keyFactory)
            .build()
            .run(repeats);
      }
    }
  }

  protected abstract Iterable<V> parameterValues(AppConfig config);

  protected abstract Parameters updateParameters(Parameters parameters, V value);
}
