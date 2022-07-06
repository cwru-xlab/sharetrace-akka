package org.sharetrace.data;

import java.util.Set;
import org.immutables.value.Value;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.graph.Edge;
import org.sharetrace.graph.SyntheticContactNetwork;
import org.sharetrace.logging.Loggable;
import org.sharetrace.message.RiskScore;

@Value.Immutable
abstract class BaseSyntheticDataset implements Dataset {

  @Override
  public RiskScore getRiskScore(int user) {
    return riskScoreFactory().getRiskScore(user);
  }

  protected abstract RiskScoreFactory riskScoreFactory();

  @Override
  @Value.Derived
  public ContactNetwork getContactNetwork() {
    return SyntheticContactNetwork.builder()
        .addAllLoggable(loggable())
        .graphGenerator(graphGenerator())
        .contactTimeFactory(contactTimeFactory())
        .build();
  }

  protected abstract Set<Class<? extends Loggable>> loggable();

  protected abstract GraphGenerator<Integer, Edge<Integer>, ?> graphGenerator();

  protected abstract ContactTimeFactory contactTimeFactory();
}
