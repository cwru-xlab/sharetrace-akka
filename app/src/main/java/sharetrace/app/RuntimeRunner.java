package sharetrace.app;

import java.util.List;
import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.graph.TemporalNetworkFactory;
import sharetrace.model.Parameters;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.util.Context;

public record RuntimeRunner<K>() implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {
    var networkFactories = List.<TemporalNetworkFactory<K>>of();
    var iterations = 1;
    var networks = 1;
    RiskScoreFactory<K> scoreFactory = null;
    for (var networkFactory : networkFactories) {
      for (var i = 0; i < networks; i++) {
        RiskPropagationBuilder.<K>create()
            .context(context)
            .parameters(parameters)
            .scoreFactory(scoreFactory)
            .contactNetwork(networkFactory.getNetwork())
            .build()
            .run(iterations);
      }
    }
  }
}
