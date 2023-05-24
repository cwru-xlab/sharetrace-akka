package io.sharetrace.experiment.data;

import io.sharetrace.experiment.data.factory.ContactTimeFactory;
import io.sharetrace.experiment.data.factory.GraphGeneratorFactory;
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
        .graphGeneratorFactory(graphGeneratorFactory())
        .users(users())
        .scoreFactory(scoreFactory())
        .contactTimeFactory(contactTimeFactory())
        .build();
  }

  protected abstract GraphGeneratorFactory graphGeneratorFactory();

  protected abstract int users();

  protected abstract ContactTimeFactory contactTimeFactory();

  @Override
  @Value.Default // Allows the contact network to be passed on to a copied instance.
  public ContactNetwork contactNetwork() {
    return SampledContactNetwork.builder()
        .graphGenerator(graphGenerator())
        .contactTimeFactory(contactTimeFactory())
        .build();
  }

  @Value.Lazy
  protected GraphGenerator<Integer, DefaultEdge, ?> graphGenerator() {
    return graphGeneratorFactory().get(users());
  }
}
