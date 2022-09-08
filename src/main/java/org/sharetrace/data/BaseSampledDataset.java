package org.sharetrace.data;

import org.immutables.value.Value;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.GraphGeneratorFactory;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.graph.SampledContactNetwork;
import org.sharetrace.message.RiskScore;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseSampledDataset implements Dataset {

  @Override
  @Value.Derived
  public ContactNetwork contactNetwork() {
    return SampledContactNetwork.builder()
        .addAllLoggable(loggable())
        .graphGenerator(graphGenerator())
        .contactTimeFactory(contactTimeFactory())
        .build();
  }

  @Override
  public RiskScore riskScore(int user) {
    return riskScoreFactory().riskScore(user);
  }

  @Value.Derived
  protected GraphGenerator<Integer, DefaultEdge, ?> graphGenerator() {
    return graphGeneratorFactory().graphGenerator(numNodes());
  }

  protected abstract GraphGeneratorFactory graphGeneratorFactory();

  protected abstract int numNodes();

  protected abstract ContactTimeFactory contactTimeFactory();

  protected abstract RiskScoreFactory riskScoreFactory();
}
