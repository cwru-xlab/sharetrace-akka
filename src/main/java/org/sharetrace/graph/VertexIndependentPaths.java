package org.sharetrace.graph;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.GraphType;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.BidirectionalDijkstraShortestPath;
import org.sharetrace.util.Preconditions;

public class VertexIndependentPaths<V, E> {

  private static final int MIN_PARALLEL_VERTICES = 50;
  private final Graph<V, E> graph;
  private final Supplier<Graph<V, E>> emptyGraphFactory;
  private final Function<Graph<V, E>, ShortestPathAlgorithm<V, E>> shortestPathsFactory;

  public VertexIndependentPaths(Graph<V, E> graph, Supplier<Graph<V, E>> emptyGraphFactory) {
    this(graph, emptyGraphFactory, BidirectionalDijkstraShortestPath::new);
  }

  public VertexIndependentPaths(
      Graph<V, E> graph,
      Supplier<Graph<V, E>> emptyGraphFactory,
      Function<Graph<V, E>, ShortestPathAlgorithm<V, E>> shortestPathsFactory) {
    this.graph = graph;
    this.emptyGraphFactory = emptyGraphFactory;
    this.shortestPathsFactory = shortestPathsFactory;
  }

  public int getPathCount(V source, V sink) {
    return getPathCount(source, sink, Integer.MAX_VALUE);
  }

  public int getPathCount(V source, V sink, int maxFind) {
    int stopAt = Math.min(maxFind, maxPossiblePaths(source, sink));
    return stopAt > 0 ? nontrivialPathCount(source, sink, stopAt) : 0;
  }

  private int maxPossiblePaths(V source, V sink) {
    return isValidPair(source, sink) ? Math.min(graph.degreeOf(source), graph.degreeOf(sink)) : 0;
  }

  private int nontrivialPathCount(V source, V sink, int maxFind) {
    Graph<V, E> graph = copyGraph();
    ShortestPathAlgorithm<V, E> shortestPaths = shortestPathsFactory.apply(graph);
    GraphPath<V, E> path;
    int nFound = 0;
    do {
      if ((path = shortestPaths.getPath(source, sink)) != null) {
        graph.removeAllVertices(withoutSourceAndSink(path));
        nFound++;
      }
    } while (path != null && nFound < maxFind);
    return nFound;
  }

  private boolean isValidPair(V source, V sink) {
    return !source.equals(sink) && graph.getEdge(source, sink) == null;
  }

  private Graph<V, E> copyGraph() {
    Graph<V, E> copy = newEmptyGraph();
    Graphs.addGraph(copy, graph);
    return copy;
  }

  private List<V> withoutSourceAndSink(GraphPath<V, E> path) {
    List<V> vertices = path.getVertexList();
    return vertices.size() < 3 ? List.of() : vertices.subList(1, vertices.size() - 1);
  }

  private Graph<V, E> newEmptyGraph() {
    Graph<V, E> graph = emptyGraphFactory.get();
    checkIsEmpty(graph);
    checkGraphType(graph);
    return graph;
  }

  private void checkIsEmpty(Graph<V, E> supplied) {
    Preconditions.checkState(
        supplied.vertexSet().isEmpty(), () -> "'emptyGraphFactory' provided a non-empty graph");
  }

  private void checkGraphType(Graph<V, E> supplied) {
    GraphType suppliedType = supplied.getType();
    GraphType expectedType = graph.getType();
    Supplier<String> message = () -> graphTypeMessage(expectedType, suppliedType);
    Preconditions.checkState(isEqual(expectedType, suppliedType), message);
  }

  private String graphTypeMessage(GraphType expectedType, GraphType suppliedType) {
    return "'emptyGraphFactory' provided a graph of type "
        + suppliedType
        + ", which is different than "
        + expectedType;
  }

  private boolean isEqual(GraphType type1, GraphType type2) {
    return type1.isUndirected() == type2.isUndirected()
        && type1.isDirected() == type2.isDirected()
        && type1.isModifiable() == type2.isModifiable()
        && type1.isWeighted() == type2.isWeighted()
        && type1.isAllowingMultipleEdges() == type2.isAllowingMultipleEdges()
        && type1.isAllowingSelfLoops() == type2.isAllowingSelfLoops()
        && type1.isAllowingCycles() == type2.isAllowingCycles();
  }

  public List<Integer> getPathCounts(V source) {
    return getPathCounts(source, Integer.MAX_VALUE);
  }

  public List<Integer> getPathCounts(V source, int maxFind) {
    return getPathCounts(source, maxFind, true);
  }

  public List<Integer> getPathCounts(V source, int maxFind, boolean allowParallel) {
    return vertices(allowParallel)
        .filter(sink -> isValidPair(source, sink))
        .map(sink -> getPathCount(source, sink, maxFind))
        .collect(countCollector(nVertices() - 1));
  }

  private Stream<V> vertices(boolean allowParallel) {
    return nVertices() > MIN_PARALLEL_VERTICES && allowParallel
        ? graph.vertexSet().parallelStream()
        : graph.vertexSet().stream();
  }

  private static Collector<Integer, ?, List<Integer>> countCollector(int size) {
    return Collectors.toCollection(() -> new IntArrayList(size));
  }

  private int nVertices() {
    return graph.vertexSet().size();
  }

  public List<Integer> getPathCounts(V source, boolean allowParallel) {
    return getPathCounts(source, Integer.MAX_VALUE, allowParallel);
  }

  public List<Integer> getAllPathCounts(int maxFind) {
    return getAllPathCounts(maxFind, true);
  }

  public List<Integer> getAllPathCounts(int maxFind, boolean allowParallel) {
    int nPairs = nVertices() * (nVertices() - 1);
    return vertices(allowParallel)
        .map(source -> getPathCounts(source, maxFind, false))
        .flatMap(Collection::stream)
        .collect(countCollector(nPairs));
  }

  public List<Integer> getAllPathCounts() {
    return getAllPathCounts(true);
  }

  public List<Integer> getAllPathCounts(boolean allowParallel) {
    return getAllPathCounts(Integer.MAX_VALUE, allowParallel);
  }
}
