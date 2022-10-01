package io.sharetrace.graph;

import org.jgrapht.Graph;
import org.jgrapht.GraphTests;
import org.jgrapht.GraphType;
import org.jgrapht.Graphs;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph;
import org.jgrapht.util.SupplierUtil;

public final class GraphFactory {

  private GraphFactory() {}

  public static Graph<Integer, DefaultEdge> copyGraph(Graph<Integer, DefaultEdge> graph) {
    return graph.getType().isDirected() ? copyDirected(graph) : copyUndirected(graph);
  }

  public static Graph<Integer, DefaultEdge> copyDirected(Graph<Integer, DefaultEdge> directed) {
    Graph<Integer, DefaultEdge> copy = newDirectedGraph();
    Graphs.addGraph(copy, GraphTests.requireDirected(directed));
    return copy;
  }

  public static Graph<Integer, DefaultEdge> copyUndirected(Graph<Integer, DefaultEdge> undirected) {
    Graph<Integer, DefaultEdge> copy = newUndirectedGraph();
    Graphs.addGraph(copy, GraphTests.requireUndirected(undirected));
    return copy;
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

  public static Graph<Integer, DefaultEdge> toDirected(Graph<Integer, DefaultEdge> graph) {
    Graph<Integer, DefaultEdge> directed;
    if (graph.getType().isDirected()) {
      directed = graph;
    } else {
      directed = newDirectedGraph();
      int source, target;
      for (DefaultEdge edge : graph.edgeSet()) {
        source = graph.getEdgeSource(edge);
        target = graph.getEdgeTarget(edge);
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
