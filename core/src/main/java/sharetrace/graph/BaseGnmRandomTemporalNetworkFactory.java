package sharetrace.graph;

import java.util.Map;
import java.util.Random;
import org.apache.commons.math3.random.RandomAdaptor;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.util.Identifiers;

@Value.Immutable
abstract class BaseGnmRandomTemporalNetworkFactory
    extends GeneratedTemporalNetworkFactory<Integer> {

  private static final boolean ALLOW_LOOPS = false;
  private static final boolean ALLOW_MULTIPLE_EDGES = false;

  public abstract int edges();

  @Override
  protected Graph<Integer, TemporalEdge> newTarget() {
    return TemporalNetworkFactoryHelper.newIntTarget();
  }

  @Override
  protected GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    Random random = RandomAdaptor.createAdaptor(random());
    return new GnmRandomGraphGenerator<>(
        nodes(), edges(), random, ALLOW_LOOPS, ALLOW_MULTIPLE_EDGES);
  }

  @Override
  protected TemporalNetwork<Integer> newNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleTemporalNetwork<>(
        target, Identifiers.newIntString(), "GnmRandom", properties());
  }

  private Map<String, ?> properties() {
    return Map.ofEntries(
        Map.entry("nodes", nodes()),
        Map.entry("edges", edges()),
        Map.entry("allowLoops", ALLOW_LOOPS),
        Map.entry("allowMultipleEdges", ALLOW_MULTIPLE_EDGES));
  }
}
