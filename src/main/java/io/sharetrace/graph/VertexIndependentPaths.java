package io.sharetrace.graph;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.BidirectionalDijkstraShortestPath;
import org.jgrapht.alg.shortestpath.SuurballeKDisjointShortestPaths;
import org.jgrapht.graph.DefaultEdge;
import org.jheaps.tree.FibonacciHeap;

/**
 * Computes the approximate number of vertex-independent paths (<a
 * href=https://dx.doi.org/10.2139/ssrn.1831790>White and Newman 2011</a>). The API allows for
 * serial or parallel (default) execution when finding the paths between a single source and
 * multiple target vertices or when finding the paths between all pairs of vertices.
 */
public final class VertexIndependentPaths {

  private static final int MIN_PARALLEL_VERTICES = 50;
  private static final int ADJACENT_PATHS = 2;
  private final Graph<Integer, DefaultEdge> graph;
  private final boolean isDirected;
  private final int numVertices;
  private final int numPairs;
  private final Graph<Integer, DefaultEdge> directed;

  public VertexIndependentPaths(Graph<Integer, DefaultEdge> graph) {
    this.graph = graph;
    this.isDirected = graph.getType().isDirected();
    this.numVertices = graph.vertexSet().size();
    this.numPairs = numVertices * (numVertices - 1) / (isDirected ? 1 : 2);
    this.directed = Graphs.toDirected(graph);
  }

  public int getPathCount(int source, int target) {
    return getPathCount(source, target, Integer.MAX_VALUE);
  }

  public int getPathCount(int source, int target, int maxFind) {
    int stopAt = Math.min(maxFind, maxPossiblePaths(source, target));
    return (stopAt > 0) ? pathCount(source, target, stopAt) : 0;
  }

  private int maxPossiblePaths(int source, int target) {
    int numPaths;
    if (source == target) {
      numPaths = 0;
    } else if (isDirected) {
      numPaths = Math.min(graph.outDegreeOf(source), graph.inDegreeOf(target));
    } else {
      numPaths = Math.min(graph.degreeOf(source), graph.degreeOf(target));
    }
    return numPaths;
  }

  private int pathCount(int source, int target, int maxFind) {
    return isAdjacent(source, target)
        ? adjacentPathCount(source, target, maxFind)
        : nonadjacentPathCount(source, target, maxFind);
  }

  private boolean isAdjacent(int source, int target) {
    return graph.getEdge(source, target) != null;
  }

  private int adjacentPathCount(int source, int target, int maxFind) {
    Graph<Integer, ?> graph = Graphs.copyGraph(directed);
    List<? extends GraphPath<Integer, ?>> paths = adjacentKShortestPaths(graph, source, target);
    int numFound = 1; // Trivial path along edge incident to source and target.
    while (paths.size() > 1 && numFound < maxFind) {
      numFound++;
      // Remove the vertices along the path that is not the edge between the source and target.
      graph.removeAllVertices(withoutEndpoints(paths.get(1)));
      paths = adjacentKShortestPaths(graph, source, target);
    }
    return numFound;
  }

  private static List<? extends GraphPath<Integer, ?>> adjacentKShortestPaths(
      Graph<Integer, ?> graph, int source, int target) {
    // Suurballe provides a simpler implementation since it ensures no loops.
    // Suurballe copies internally, so we must pass in the graph on every call.
    return new SuurballeKDisjointShortestPaths<>(graph).getPaths(source, target, ADJACENT_PATHS);
  }

  private int nonadjacentPathCount(int source, int target, int maxFind) {
    Graph<Integer, ?> graph = Graphs.copyGraph(this.graph);
    ShortestPathAlgorithm<Integer, ?> shortestPaths = newShortestPaths(graph);
    GraphPath<Integer, ?> path = shortestPaths.getPath(source, target);
    int numFound = 0;
    while (path != null && numFound < maxFind) {
      numFound++;
      graph.removeAllVertices(withoutEndpoints(path));
      path = shortestPaths.getPath(source, target);
    }
    return numFound;
  }

  private static <V> List<V> withoutEndpoints(GraphPath<V, ?> path) {
    List<V> vertices = path.getVertexList();
    return (vertices.size() < 3) ? List.of() : vertices.subList(1, vertices.size() - 1);
  }

  private static <V, E> ShortestPathAlgorithm<V, E> newShortestPaths(Graph<V, E> graph) {
    // Fibonacci heap provides O(1) insert vs. pairing heap O(log n).
    return new BidirectionalDijkstraShortestPath<>(graph, FibonacciHeap::new);
  }

  public List<Integer> getPathCounts(int source) {
    return getPathCounts(source, Integer.MAX_VALUE);
  }

  public List<Integer> getPathCounts(int source, int maxFind) {
    return getPathCounts(source, maxFind, true);
  }

  public List<Integer> getPathCounts(int source, int maxFind, boolean allowParallel) {
    return vertices(allowParallel)
        .filter(target -> source != target)
        .map(target -> getPathCount(source, target, maxFind))
        .collect(newCounts(numVertices - 1), List::add, List::addAll);
  }

  private IntStream vertices(boolean allowParallel) {
    return allowParallel && numVertices > MIN_PARALLEL_VERTICES
        ? graph.vertexSet().parallelStream().mapToInt(Number::intValue)
        : graph.vertexSet().stream().mapToInt(Number::intValue);
  }

  private static Supplier<List<Integer>> newCounts(int size) {
    return () -> new IntArrayList(size);
  }

  public List<Integer> getPathCounts(int source, boolean allowParallel) {
    return getPathCounts(source, Integer.MAX_VALUE, allowParallel);
  }

  public List<Integer> getAllPathCounts(int maxFind) {
    return getAllPathCounts(maxFind, true);
  }

  public List<Integer> getAllPathCounts(int maxFind, boolean allowParallel) {
    return vertices(allowParallel)
        .flatMap(source -> uniqueSourceCounts(source, maxFind))
        .collect(newCounts(numPairs), List::add, List::addAll);
  }

  private IntStream uniqueSourceCounts(int source, int maxFind) {
    return vertices(false)
        .filter(target -> isDirected ? source != target : source < target)
        .map(target -> getPathCount(source, target, maxFind));
  }

  public List<Integer> getAllPathCounts() {
    return getAllPathCounts(true);
  }

  public List<Integer> getAllPathCounts(boolean allowParallel) {
    return getAllPathCounts(Integer.MAX_VALUE, allowParallel);
  }
}
