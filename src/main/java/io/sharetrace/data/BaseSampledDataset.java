package io.sharetrace.data;

import io.sharetrace.data.factory.ContactTimeFactory;
import io.sharetrace.data.factory.GraphGeneratorFactory;
import io.sharetrace.data.factory.RiskScoreFactory;
import io.sharetrace.graph.Contact;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.graph.SampledContactNetwork;
import io.sharetrace.model.RiskScore;
import java.util.Set;
import org.immutables.value.Value;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseSampledDataset implements Dataset {

  @Override
  public Set<Integer> users() {
    return contactNetwork().users();
  }

  @Override
  public Set<Contact> contacts() {
    return contactNetwork().contacts();
  }

  @Override
  public void logMetrics() {
    contactNetwork().logMetrics();
  }

  @Override
  public RiskScore riskScore(int user) {
    return scoreFactory().riskScore(user);
  }

  public SampledDataset withNewContactNetwork() {
    return SampledDataset.builder()
        .addAllLoggable(loggable())
        .graphGeneratorFactory(graphGeneratorFactory())
        .numNodes(numNodes())
        .scoreFactory(scoreFactory())
        .contactTimeFactory(contactTimeFactory())
        .build();
  }

  @Value.Default // Allows the contact network to be passed on to a copied instance.
  protected ContactNetwork contactNetwork() {
    return SampledContactNetwork.builder()
        .addAllLoggable(loggable())
        .graphGenerator(graphGenerator())
        .contactTimeFactory(contactTimeFactory())
        .build();
  }

  @Value.Lazy
  protected GraphGenerator<Integer, DefaultEdge, ?> graphGenerator() {
    return graphGeneratorFactory().graphGenerator(numNodes());
  }

  protected abstract GraphGeneratorFactory graphGeneratorFactory();

  protected abstract int numNodes();

  protected abstract ContactTimeFactory contactTimeFactory();

  protected abstract RiskScoreFactory scoreFactory();
}
