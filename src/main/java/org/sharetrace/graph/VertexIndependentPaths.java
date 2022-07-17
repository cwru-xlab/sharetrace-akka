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
import org.sharetrace.util.Preconditions;

public class VertexIndependentPaths {

  private static final int MIN_PARALLEL_VERTICES = 50;
  private final Graph<Integer, Edge<Integer>> graph;
  private final int nVertices;
  private final ShortestPathsFactory<Integer, Edge<Integer>> shortestPathsFactory;
  private Graph<Integer, Edge<Integer>> directed;

  public VertexIndependentPaths(Graph<Integer, Edge<Integer>> graph) {
    this(graph, BidirectionalDijkstraShortestPath::new);
  }

  public VertexIndependentPaths(
      Graph<Integer, Edge<Integer>> graph,
      ShortestPathsFactory<Integer, Edge<Integer>> shortestPathsFactory) {
    checkIsUndirected(graph, () -> "'graph' must be undirected");
    this.graph = graph;
    this.nVertices = graph.vertexSet().size();
    this.shortestPathsFactory = shortestPathsFactory;
  }

  private static void checkIsUndirected(Graph<?, ?> graph, Supplier<String> message) {
    Preconditions.checkState(graph.getType().isUndirected(), message);
  }

  public int getPathCount(int source, int target) {
    return getPathCount(source, target, Integer.MAX_VALUE);
  }

  public int getPathCount(int source, int target, int maxFind) {
    int stopAt = Math.min(maxFind, maxPossiblePaths(source, target));
    return stopAt > 0 ? nontrivialPathCount(source, target, stopAt) : 0;
  }

  private int maxPossiblePaths(int source, int target) {
    return source == target ? 0 : Math.min(graph.degreeOf(source), graph.degreeOf(target));
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
    Graph<Integer, Edge<Integer>> copy = GraphFactory.copyDirected(directed);
    KShortestPathAlgorithm<Integer, Edge<Integer>> shortestPaths = newKShortestPaths(copy);
    List<GraphPath<Integer, Edge<Integer>>> paths;
    int nFound = 1;
    do {
      if ((paths = shortestPaths.getPaths(source, target, 2)).size() == 2) {
        nFound++;
        // Remove the vertices along the path that is not the edge between the source and target.
        copy.removeAllVertices(withoutEndpoints(paths.get(1)));
        // Suurballe copies internally, so we must pass in the updated graph.
        shortestPaths = newKShortestPaths(copy);
      }
    } while (paths.size() > 1 && nFound < maxFind);
    return nFound;
  }

  private int nonadjacentPathCount(int source, int target, int maxFind) {
    Graph<Integer, Edge<Integer>> copy = GraphFactory.copyUndirected(graph);
    ShortestPathAlgorithm<Integer, Edge<Integer>> shortestPaths =
        shortestPathsFactory.newShortestPaths(copy);
    GraphPath<Integer, Edge<Integer>> path;
    int nFound = 0;
    do {
      if ((path = shortestPaths.getPath(source, target)) != null) {
        nFound++;
        copy.removeAllVertices(withoutEndpoints(path));
      }
    } while (path != null && nFound < maxFind);
    return nFound;
  }

  private static <V, E> KShortestPathAlgorithm<V, E> newKShortestPaths(Graph<V, E> graph) {
    // Suurballe provides a simpler implementation since it ensures no loops.
    return new SuurballeKDisjointShortestPaths<>(graph);
  }

  private List<Integer> withoutEndpoints(GraphPath<Integer, Edge<Integer>> path) {
    List<Integer> vertices = path.getVertexList();
    return vertices.size() < 3 ? List.of() : vertices.subList(1, vertices.size() - 1);
  }

  public List<Integer> getPathCounts(int source) {
    return getPathCounts(source, Integer.MAX_VALUE);
  }

  public List<Integer> getPathCounts(int source, int maxFind) {
    return getPathCounts(source, maxFind, true);
  }

  public List<Integer> getPathCounts(int source, int maxFind, boolean allowParallel) {
    setDirected();
    return vertices(allowParallel)
        .filter(target -> source != target)
        .map(target -> getPathCount(source, target, maxFind))
        .collect(newCounts(nVertices - 1), Collection::add, Collection::addAll);
  }

  private void setDirected() {
    if (directed == null) {
      directed = GraphFactory.toDirected(graph);
    }
  }

  private IntStream vertices(boolean allowParallel) {
    return nVertices > MIN_PARALLEL_VERTICES && allowParallel
        ? graph.vertexSet().parallelStream().mapToInt(Integer::valueOf)
        : graph.vertexSet().stream().mapToInt(Integer::valueOf);
  }

  private Supplier<List<Integer>> newCounts(int size) {
    return () -> new IntArrayList(size);
  }

  public List<Integer> getPathCounts(int source, boolean allowParallel) {
    return getPathCounts(source, Integer.MAX_VALUE, allowParallel);
  }

  public List<Integer> getAllPathCounts(int maxFind) {
    return getAllPathCounts(maxFind, true);
  }

  public List<Integer> getAllPathCounts(int maxFind, boolean allowParallel) {
    setDirected();
    return vertices(allowParallel)
        .flatMap(source -> uniqueSourceCounts(source, maxFind))
        .collect(newCounts(nVertices * (nVertices - 1) / 2), Collection::add, Collection::addAll);
  }

  private IntStream uniqueSourceCounts(int source, int maxFind) {
    return vertices(false)
        .filter(target -> source < target)
        .map(target -> getPathCount(source, target, maxFind));
  }

  public List<Integer> getAllPathCounts() {
    return getAllPathCounts(true);
  }

  public List<Integer> getAllPathCounts(boolean allowParallel) {
    return getAllPathCounts(Integer.MAX_VALUE, allowParallel);
  }
}
