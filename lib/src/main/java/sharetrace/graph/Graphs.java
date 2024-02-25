package sharetrace.graph;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.jgrapht.Graph;
import org.jgrapht.GraphTests;
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

  public static Graph<Integer, DefaultEdge> copy(Graph<Integer, DefaultEdge> graph) {
    return graph.getType().isDirected() ? copyDirected(graph) : copyUndirected(graph);
  }

  public static Graph<Integer, DefaultEdge> copyDirected(Graph<Integer, DefaultEdge> directed) {
    return copy(GraphTests.requireDirected(directed), newDirectedGraph());
  }

  public static Graph<Integer, DefaultEdge> copyUndirected(Graph<Integer, DefaultEdge> undirected) {
    return copy(GraphTests.requireUndirected(undirected), newUndirectedGraph());
  }

  public static Graph<Integer, TemporalEdge> newTemporalGraph() {
    return newGraph(DefaultGraphType.simple().asWeighted(), TemporalEdge::new);
  }

  public static Graph<Integer, DefaultEdge> newDirectedGraph() {
    return newGraph(DefaultGraphType.directedSimple(), DefaultEdge::new);
  }

  public static Graph<Integer, DefaultEdge> newUndirectedGraph() {
    return newGraph(DefaultGraphType.simple(), DefaultEdge::new);
  }

  public static Graph<Integer, DefaultEdge> asDirected(Graph<Integer, DefaultEdge> graph) {
    if (graph.getType().isDirected()) {
      return graph;
    }
    var directed = newDirectedGraph();
    for (var edge : graph.edgeSet()) {
      var source = graph.getEdgeSource(edge);
      var target = graph.getEdgeTarget(edge);
      directed.addVertex(source);
      directed.addVertex(target);
      directed.addEdge(source, target);
      directed.addEdge(target, source);
    }
    return directed;
  }

  private static Graph<Integer, DefaultEdge> copy(
      Graph<Integer, DefaultEdge> source, Graph<Integer, DefaultEdge> target) {
    org.jgrapht.Graphs.addGraph(target, source);
    return target;
  }

  private static <E> Graph<Integer, E> newGraph(GraphType graphType, Supplier<E> edgeFactory) {
    Supplier<Integer> nodeFactory = new AtomicInteger(0)::getAndIncrement;
    var fastLookups = false;
    return new FastutilMapIntVertexGraph<>(nodeFactory, edgeFactory, graphType, fastLookups);
  }
}
