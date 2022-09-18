package io.sharetrace.data;

import io.sharetrace.data.factory.ContactTimeFactory;
import io.sharetrace.data.factory.GraphGeneratorFactory;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.graph.SampledContactNetwork;
import org.immutables.value.Value;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseSampledDataset extends AbstractDataset {

  @Override
  public SampledDataset withNewContactNetwork() {
    return SampledDataset.builder()
        .addAllLoggable(loggable())
        .graphGeneratorFactory(graphGeneratorFactory())
        .numNodes(numNodes())
        .scoreFactory(scoreFactory())
        .contactTimeFactory(contactTimeFactory())
        .build();
  }

  @Override
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
}
