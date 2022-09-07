package org.sharetrace.graph;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.KShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.BidirectionalDijkstraShortestPath;
import org.jgrapht.alg.shortestpath.SuurballeKDisjointShortestPaths;
import org.jgrapht.graph.DefaultEdge;
import org.jheaps.tree.FibonacciHeap;

public class VertexIndependentPaths {

  private static final int MIN_PARALLEL_VERTICES = 50;
  private final Graph<Integer, DefaultEdge> graph;
  private final boolean isDirected;
  private final int numVertices;
  private final int numPairs;
  private final Graph<Integer, DefaultEdge> directed;

  public <V, E> VertexIndependentPaths(Graph<V, E> graph) {
    this.graph = GraphFactory.toIntGraph(graph);
    this.isDirected = graph.getType().isDirected();
    this.numVertices = graph.vertexSet().size();
    this.numPairs = numVertices * (numVertices - 1) / (isDirected ? 1 : 2);
    this.directed = GraphFactory.toDirected(this.graph);
  }

  public int getPathCount(int source, int target) {
    return getPathCount(source, target, Integer.MAX_VALUE);
  }

  public int getPathCount(int source, int target, int maxFind) {
    int stopAt = Math.min(maxFind, maxPossiblePaths(source, target));
    return stopAt > 0 ? nontrivialPathCount(source, target, stopAt) : 0;
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

  private int nontrivialPathCount(int source, int target, int maxFind) {
    return isAdjacent(source, target)
        ? adjacentPathCount(source, target, maxFind)
        : nonadjacentPathCount(source, target, maxFind);
  }

  private boolean isAdjacent(int v1, int v2) {
    return graph.getEdge(v1, v2) != null;
  }

  private int adjacentPathCount(int source, int target, int maxFind) {
    Graph<Integer, ?> graph = GraphFactory.copyDirected(directed);
    KShortestPathAlgorithm<Integer, ?> shortestPaths = newKShortestPaths(graph);
    List<? extends GraphPath<Integer, ?>> paths;
    int nFound = 1; // Trivial path along edge incident to source and target.
    do {
      if ((paths = shortestPaths.getPaths(source, target, 2)).size() == 2) {
        nFound++;
        // Remove the vertices along the path that is not the edge between the source and target.
        graph.removeAllVertices(withoutEndpoints(paths.get(1)));
        // Suurballe copies internally, so we must pass in the updated graph.
        shortestPaths = newKShortestPaths(graph);
      }
    } while (paths.size() > 1 && nFound < maxFind);
    return nFound;
  }

  private static <V, E> KShortestPathAlgorithm<V, E> newKShortestPaths(Graph<V, E> graph) {
    // Suurballe provides a simpler implementation since it ensures no loops.
    return new SuurballeKDisjointShortestPaths<>(graph);
  }

  private static <V> List<V> withoutEndpoints(GraphPath<V, ?> path) {
    List<V> vertices = path.getVertexList();
    return vertices.size() < 3 ? List.of() : vertices.subList(1, vertices.size() - 1);
  }

  private int nonadjacentPathCount(int source, int target, int maxFind) {
    Graph<Integer, ?> graph = GraphFactory.copyGraph(this.graph);
    ShortestPathAlgorithm<Integer, ?> shortestPaths = newShortestPaths(graph);
    GraphPath<Integer, ?> path;
    int nFound = 0;
    do {
      if ((path = shortestPaths.getPath(source, target)) != null) {
        nFound++;
        graph.removeAllVertices(withoutEndpoints(path));
      }
    } while (path != null && nFound < maxFind);
    return nFound;
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
        .collect(newCounts(numVertices - 1), Collection::add, Collection::addAll);
  }

  private IntStream vertices(boolean allowParallel) {
    return numVertices > MIN_PARALLEL_VERTICES && allowParallel
        ? graph.vertexSet().parallelStream().mapToInt(Integer::valueOf)
        : graph.vertexSet().stream().mapToInt(Integer::valueOf);
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
        .collect(newCounts(numPairs), Collection::add, Collection::addAll);
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
