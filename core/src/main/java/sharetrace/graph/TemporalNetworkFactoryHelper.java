package sharetrace.graph;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.opt.graph.fastutil.FastutilMapGraph;
import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph;

public final class TemporalNetworkFactoryHelper {

  private static final boolean FAST_LOOKUPS = false;
  private static final GraphType GRAPH_TYPE = DefaultGraphType.simple();
  private static final Supplier<TemporalEdge> EDGE_FACTORY = TemporalEdge::new;

  @SuppressWarnings("unchecked")
  public static <V> Graph<V, TemporalEdge> newTarget(V node) {
    return node instanceof Integer ? (Graph<V, TemporalEdge>) newIntTarget() : newTarget();
  }

  public static <V> Graph<V, TemporalEdge> newTarget() {
    return new FastutilMapGraph<>(null, EDGE_FACTORY, GRAPH_TYPE, FAST_LOOKUPS);
  }

  public static Graph<Integer, TemporalEdge> newIntTarget() {
    return new FastutilMapIntVertexGraph<>(nodeFactory(), EDGE_FACTORY, GRAPH_TYPE, FAST_LOOKUPS);
  }

  private static Supplier<Integer> nodeFactory() {
    return new AtomicInteger(0)::getAndIncrement;
  }
}
