package org.sharetrace.data;

import java.util.Set;
import org.immutables.value.Value;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.GraphGeneratorFactory;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.graph.SampledContactNetwork;
import org.sharetrace.logging.Loggable;
import org.sharetrace.message.RiskScore;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseSampledDataset implements Dataset {

  @Override
  public RiskScore getRiskScore(int user) {
    return riskScoreFactory().getRiskScore(user);
  }

  protected abstract RiskScoreFactory riskScoreFactory();

  @Override
  @Value.Derived
  public SampledContactNetwork getContactNetwork() {
    return SampledContactNetwork.builder()
        .addAllLoggable(loggable())
        .graphGenerator(graphGenerator())
        .contactTimeFactory(contactTimeFactory())
        .build();
  }

  protected abstract Set<Class<? extends Loggable>> loggable();

  @Value.Derived
  protected GraphGenerator<Integer, DefaultEdge, ?> graphGenerator() {
    return graphGeneratorFactory().getGraphGenerator(numNodes());
  }

  protected abstract GraphGeneratorFactory graphGeneratorFactory();

  protected abstract int numNodes();

  protected abstract ContactTimeFactory contactTimeFactory();
}
