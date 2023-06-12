package sharetrace.graph;

import java.util.Map;
import java.util.Random;
import org.apache.commons.math3.random.RandomAdaptor;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.util.IdFactory;

@Value.Immutable
abstract class BaseBarabasiAlbertTemporalNetworkFactory
    extends GeneratedTemporalNetworkFactory<Integer> {

  public abstract int initialNodes();

  public abstract int newEdges();

  @Override
  protected Graph<Integer, TemporalEdge> newTarget() {
    return GraphFactory.newIntGraph();
  }

  @Override
  protected GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    Random random = RandomAdaptor.createAdaptor(random());
    return new BarabasiAlbertGraphGenerator<>(initialNodes(), newEdges(), nodes(), random);
  }

  @Override
  protected TemporalNetwork<Integer> newNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleTemporalNetwork<>(
        target, IdFactory.newIntString(), "BarabasiAlbert", properties());
  }

  private Map<String, ?> properties() {
    return Map.ofEntries(
        Map.entry("nodes", nodes()),
        Map.entry("initialNodes", initialNodes()),
        Map.entry("newEdges", newEdges()));
  }
}
