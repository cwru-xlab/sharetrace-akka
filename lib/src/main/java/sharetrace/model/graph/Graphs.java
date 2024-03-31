package sharetrace.model.graph;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph;

public final class Graphs {

  private Graphs() {}

  public static void addTemporalEdge(
      Graph<Integer, TemporalEdge> graph, int source, int target, long time) {
    graph.addVertex(source);
    graph.addVertex(target);
    var edge = graph.getEdge(source, target);
    if (edge == null) {
      edge = graph.addEdge(source, target);
    }
    edge.updateTime(time, Math::max);
    if (graph.getType().isWeighted()) {
      graph.setEdgeWeight(edge, edge.getTime());
    }
  }

  public static Graph<Integer, TemporalEdge> newTemporalGraph() {
    return newGraph(DefaultGraphType.simple().asWeighted(), TemporalEdge::new);
  }

  public static Graph<Integer, DefaultEdge> newDirectedGraph() {
    return newGraph(DefaultGraphType.directedSimple(), DefaultEdge::new);
  }

  private static <E> Graph<Integer, E> newGraph(GraphType graphType, Supplier<E> edgeFactory) {
    Supplier<Integer> nodeFactory = new AtomicInteger(0)::getAndIncrement;
    var fastLookups = false;
    return new FastutilMapIntVertexGraph<>(nodeFactory, edgeFactory, graphType, fastLookups);
  }
}
