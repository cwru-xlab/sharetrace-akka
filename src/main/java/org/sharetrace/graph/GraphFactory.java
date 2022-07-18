package org.sharetrace.graph;

import java.util.function.Supplier;
import org.jgrapht.Graph;
import org.jgrapht.GraphTests;
import org.jgrapht.GraphType;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph;

public final class GraphFactory {

  private GraphFactory() {}

  public static Graph<Integer, Edge<Integer>> toDirected(Graph<Integer, Edge<Integer>> graph) {
    Graph<Integer, Edge<Integer>> directed;
    if (graph.getType().isDirected()) {
      directed = copyDirected(graph);
    } else {
      directed = newDirectedGraph();
      for (Edge<Integer> edge : graph.edgeSet()) {
        Graphs.addEdgeWithVertices(directed, edge.source(), edge.target());
        directed.addEdge(edge.target(), edge.source());
      }
    }
    return directed;
  }

  public static Graph<Integer, Edge<Integer>> newDirectedGraph() {
    return newGraph(DefaultGraphType.directedSimple());
  }

  public static Graph<Integer, Edge<Integer>> copyDirected(Graph<Integer, Edge<Integer>> directed) {
    Graph<Integer, Edge<Integer>> copy = GraphFactory.newDirectedGraph();
    Graphs.addGraph(copy, GraphTests.requireDirected(directed));
    return copy;
  }

  public static Graph<Integer, Edge<Integer>> copyUndirected(
      Graph<Integer, Edge<Integer>> undirected) {
    Graph<Integer, Edge<Integer>> copy = GraphFactory.newUndirectedGraph();
    Graphs.addGraph(copy, GraphTests.requireUndirected(undirected));
    return copy;
  }

  public static Graph<Integer, Edge<Integer>> newUndirectedGraph() {
    return newGraph(DefaultGraphType.simple());
  }

  private static Graph<Integer, Edge<Integer>> newGraph(GraphType graphType) {
    return new FastutilMapIntVertexGraph<>(vertexIdFactory(), Edge::new, graphType, false);
  }

  private static Supplier<Integer> vertexIdFactory() {
    int[] id = new int[] {0};
    return () -> id[0]++;
  }
}