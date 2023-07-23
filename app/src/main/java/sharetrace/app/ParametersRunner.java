package sharetrace.app;

import java.util.List;
import sharetrace.algorithm.RiskPropagationBuilder;
import sharetrace.graph.TemporalNetworkFactory;
import sharetrace.model.Parameters;
import sharetrace.model.ParametersBuilder;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.util.Context;

public record ParametersRunner<K>() implements Runner {

  @Override
  public void run(Parameters parameters, Context context) {
    var iterations = 1;
    var networks = 1;
    var transmissionRates = List.<Float>of();
    var sendCoefficients = List.<Float>of();
    TemporalNetworkFactory<K> networkFactory = null;
    for (int i = 0; i < networks; i++) {
      var network = networkFactory.getNetwork();
      RiskScoreFactory<K> scoreFactory = null; // cached
      for (float transmissionRate : transmissionRates) {
        for (float sendCoefficient : sendCoefficients) {
          RiskPropagationBuilder.<K>create()
              .context(context)
              .parameters(update(parameters, transmissionRate, sendCoefficient))
              .scoreFactory(scoreFactory)
              .contactNetwork(network)
              .build()
              .run(iterations);
        }
      }
    }
  }

  private Parameters update(Parameters parameters, float transmissionRate, float sendCoefficient) {
    return ParametersBuilder.builder(parameters)
        .transmissionRate(transmissionRate)
        .sendCoefficient(sendCoefficient)
        .build();
  }
}
