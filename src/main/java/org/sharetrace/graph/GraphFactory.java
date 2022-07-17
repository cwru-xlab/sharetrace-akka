package org.sharetrace.graph;

import java.util.function.Supplier;
import org.jgrapht.Graph;
import org.jgrapht.GraphTests;
import org.jgrapht.GraphType;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph;
import org.sharetrace.util.Preconditions;

public final class GraphFactory {

  private GraphFactory() {}

  public static Graph<Integer, Edge<Integer>> toDirected(Graph<Integer, Edge<Integer>> undirected) {
    Graph<Integer, Edge<Integer>> directed;
    if (undirected.getType().isDirected()) {
      directed = undirected;
    } else {
      directed = newDirectedGraph();
      for (Edge<Integer> edge : undirected.edgeSet()) {
        Graphs.addEdgeWithVertices(directed, edge.source(), edge.target());
        directed.addEdge(edge.target(), edge.source());
      }
    }
    return directed;
  }

  public static Graph<Integer, Edge<Integer>> newDirectedGraph() {
    return newGraph(DefaultGraphType.directedSimple());
  }

  private static Graph<Integer, Edge<Integer>> newGraph(GraphType graphType) {
    return new FastutilMapIntVertexGraph<>(vertexIdFactory(), Edge::new, graphType, false);
  }

  private static Supplier<Integer> vertexIdFactory() {
    int[] id = new int[] {0};
    return () -> id[0]++;
  }

  public static Graph<Integer, Edge<Integer>> copyDirected(Graph<Integer, Edge<Integer>> graph) {
    Graph<Integer, Edge<Integer>> copy = requireEmpty(GraphFactory.newDirectedGraph());
    Graphs.addGraph(GraphTests.requireDirected(copy), GraphTests.requireDirected(graph));
    return copy;
  }

  private static <V, E> Graph<V, E> requireEmpty(Graph<V, E> graph) {
    Preconditions.checkState(GraphTests.isEmpty(graph), () -> "Graph must be empty");
    return graph;
  }

  public static Graph<Integer, Edge<Integer>> copyUndirected(Graph<Integer, Edge<Integer>> graph) {
    Graph<Integer, Edge<Integer>> copy = requireEmpty(GraphFactory.newUndirectedGraph());
    Graphs.addGraph(GraphTests.requireUndirected(copy), GraphTests.requireUndirected(graph));
    return copy;
  }

  public static Graph<Integer, Edge<Integer>> newUndirectedGraph() {
    return newGraph(DefaultGraphType.simple());
  }
}
