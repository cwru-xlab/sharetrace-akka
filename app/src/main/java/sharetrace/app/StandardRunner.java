package sharetrace.app;

import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.config.AppConfig;
import sharetrace.model.Context;
import sharetrace.model.Parameters;

public class StandardRunner implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {
    var config = AppConfig.of(parameters, context);
    var keyFactory = config.getKeyFactory();
    for (int i = 0; i < config.getIterations(); i++) {
      RiskPropagationBuilder.create()
          .context(context)
          .parameters(parameters)
          .scoreFactory(config.getScoreFactory())
          .networkFactory(config.getNetworkFactory())
          .keyFactory(keyFactory)
          .build()
          .run(config.getRepeats());
    }
  }
}
