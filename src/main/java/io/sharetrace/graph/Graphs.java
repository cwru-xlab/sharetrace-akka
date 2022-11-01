package io.sharetrace.graph;

import org.jgrapht.Graph;
import org.jgrapht.GraphTests;
import org.jgrapht.GraphType;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph;
import org.jgrapht.util.SupplierUtil;

public final class Graphs {

  private Graphs() {}

  public static Graph<Integer, DefaultEdge> copy(Graph<Integer, DefaultEdge> graph) {
    return graph.getType().isDirected() ? copyDirected(graph) : copyUndirected(graph);
  }

  public static Graph<Integer, DefaultEdge> copyDirected(Graph<Integer, DefaultEdge> directed) {
    return copy(GraphTests.requireDirected(directed), newDirectedGraph());
  }

  public static Graph<Integer, DefaultEdge> copyUndirected(Graph<Integer, DefaultEdge> undirected) {
    return copy(GraphTests.requireUndirected(undirected), newUndirectedGraph());
  }

  private static <V, E> Graph<V, E> copy(Graph<V, E> src, Graph<V, E> dst) {
    org.jgrapht.Graphs.addGraph(dst, src);
    return dst;
  }

  public static Graph<Integer, DefaultEdge> newDirectedGraph() {
    return newGraph(DefaultGraphType.directedSimple());
  }

  public static Graph<Integer, DefaultEdge> newUndirectedGraph() {
    return newGraph(DefaultGraphType.simple());
  }

  private static Graph<Integer, DefaultEdge> newGraph(GraphType graphType) {
    return new FastutilMapIntVertexGraph<>(
        SupplierUtil.createIntegerSupplier(), DefaultEdge::new, graphType, false);
  }

  public static Graph<Integer, DefaultEdge> asDirected(Graph<Integer, DefaultEdge> graph) {
    Graph<Integer, DefaultEdge> directed;
    if (graph.getType().isDirected()) {
      directed = graph;
    } else {
      directed = newDirectedGraph();
      for (DefaultEdge edge : graph.edgeSet()) {
        int source = graph.getEdgeSource(edge);
        int target = graph.getEdgeTarget(edge);
        directed.addVertex(source);
        directed.addVertex(target);
        directed.addEdge(source, target);
        directed.addEdge(target, source);
      }
    }
    return directed;
  }

  public static Graph<Integer, DefaultEdge> newUndirectedGraph(
      GraphGenerator<Integer, DefaultEdge, ?> generator) {
    Graph<Integer, DefaultEdge> graph = newUndirectedGraph();
    generator.generateGraph(graph);
    return graph;
  }
}
