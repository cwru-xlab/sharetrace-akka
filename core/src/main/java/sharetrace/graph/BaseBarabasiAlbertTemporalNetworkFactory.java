package sharetrace.graph;

import java.util.Map;
import java.util.Random;
import org.apache.commons.math3.random.RandomAdaptor;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.util.Identifiers;

@Value.Immutable
abstract class BaseBarabasiAlbertTemporalNetworkFactory
    extends GeneratedTemporalNetworkFactory<Integer> {

  public abstract int initialVertices();

  public abstract int newEdges();

  @Override
  protected Graph<Integer, TemporalEdge> newTarget() {
    return TemporalNetworkFactoryHelper.newIntTarget();
  }

  @Override
  protected GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    Random random = RandomAdaptor.createAdaptor(random());
    return new BarabasiAlbertGraphGenerator<>(initialVertices(), newEdges(), vertices(), random);
  }

  @Override
  protected TemporalNetwork<Integer> newNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleTemporalNetwork<>(
        target, Identifiers.newIntString(), "BarabasiAlbert", properties());
  }

  private Map<String, ?> properties() {
    return Map.ofEntries(
        Map.entry("vertices", vertices()),
        Map.entry("initialVertices", initialVertices()),
        Map.entry("newEdges", newEdges()));
  }
}
