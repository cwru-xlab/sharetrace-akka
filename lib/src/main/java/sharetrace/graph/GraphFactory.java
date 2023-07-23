package sharetrace.graph;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.opt.graph.fastutil.FastutilMapGraph;
import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph;

public final class GraphFactory {

  private static final boolean FAST_LOOKUPS = false;
  private static final GraphType GRAPH_TYPE = DefaultGraphType.simple().asWeighted();
  private static final Supplier<TemporalEdge> EDGE_FACTORY = TemporalEdge::new;

  @SuppressWarnings("unchecked")
  public static <V> Graph<V, TemporalEdge> newGraph(V node) {
    return node instanceof Integer ? (Graph<V, TemporalEdge>) newIntGraph() : newGraph();
  }

  public static <V> Graph<V, TemporalEdge> newGraph() {
    return new FastutilMapGraph<>(null, EDGE_FACTORY, GRAPH_TYPE, FAST_LOOKUPS);
  }

  public static Graph<Integer, TemporalEdge> newIntGraph() {
    Supplier<Integer> nodeFactory = new AtomicInteger(0)::getAndIncrement;
    return new FastutilMapIntVertexGraph<>(nodeFactory, EDGE_FACTORY, GRAPH_TYPE, FAST_LOOKUPS);
  }
}
