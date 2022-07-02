package org.sharetrace.data;

import java.time.Instant;
import java.util.Set;
import org.immutables.value.Value;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.graph.Edge;
import org.sharetrace.logging.Loggable;
import org.sharetrace.message.RiskScore;

@Value.Immutable
abstract class BaseSyntheticDataset implements Dataset {

  @Override
  public Instant getContactTime(int user1, int user2) {
    return contactTimeFactory().getContactTime(user1, user2);
  }

  protected abstract ContactTimeFactory contactTimeFactory();

  @Override
  public RiskScore getRiskScore(int user) {
    return riskScoreFactory().getRiskScore(user);
  }

  protected abstract RiskScoreFactory riskScoreFactory();

  @Override
  @Value.Derived
  public ContactNetwork getContactNetwork() {
    return ContactNetwork.create(graphGenerator(), loggable());
  }

  protected abstract GraphGenerator<Integer, Edge<Integer>, ?> graphGenerator();

  protected abstract Set<Class<? extends Loggable>> loggable();
}
