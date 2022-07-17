package org.sharetrace.graph;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.GraphType;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.BidirectionalDijkstraShortestPath;
import org.sharetrace.util.Preconditions;

public class VertexIndependentPaths<E> {

  private static final int MIN_PARALLEL_VERTICES = 50;
  private final Graph<Integer, E> graph;
  private final int nVertices;
  private final boolean isDirected;
  private final EmptyGraphFactory<Integer, E> emptyGraphFactory;
  private final ShortestPathsFactory<Integer, E> shortestPathsFactory;

  public VertexIndependentPaths(
      Graph<Integer, E> graph, EmptyGraphFactory<Integer, E> emptyGraphFactory) {
    this(graph, emptyGraphFactory, BidirectionalDijkstraShortestPath::new);
  }

  public VertexIndependentPaths(
      Graph<Integer, E> graph,
      EmptyGraphFactory<Integer, E> emptyGraphFactory,
      ShortestPathsFactory<Integer, E> shortestPathsFactory) {
    this.graph = graph;
    this.nVertices = graph.vertexSet().size();
    this.isDirected = graph.getType().isDirected();
    this.emptyGraphFactory = emptyGraphFactory;
    this.shortestPathsFactory = shortestPathsFactory;
  }

  public int getPathCount(int source, int sink) {
    return getPathCount(source, sink, Integer.MAX_VALUE);
  }

  public int getPathCount(int source, int sink, int maxFind) {
    int stopAt = Math.min(maxFind, maxPossiblePaths(source, sink));
    return stopAt > 0 ? nontrivialPathCount(source, sink, stopAt) : 0;
  }

  private int maxPossiblePaths(int source, int sink) {
    int nPaths = 0;
    if (isPairValid(source, sink)) {
      if (isDirected) {
        nPaths = Math.min(graph.outDegreeOf(source), graph.inDegreeOf(sink));
      } else {
        nPaths = Math.min(graph.degreeOf(source), graph.degreeOf(sink));
      }
    }
    return nPaths;
  }

  private int nontrivialPathCount(int source, int sink, int maxFind) {
    Graph<Integer, E> graph = copyGraph();
    ShortestPathAlgorithm<Integer, E> shortestPaths = shortestPathsFactory.newShortestPaths(graph);
    GraphPath<Integer, E> path;
    int nFound = 0;
    do {
      if ((path = shortestPaths.getPath(source, sink)) != null) {
        graph.removeAllVertices(withoutSourceAndSink(path));
        nFound++;
      }
    } while (path != null && nFound < maxFind);
    return nFound;
  }

  private boolean isPairValid(int source, int sink) {
    return source != sink && graph.getEdge(source, sink) == null;
  }

  private Graph<Integer, E> copyGraph() {
    Graph<Integer, E> copy = newEmptyGraph();
    Graphs.addGraph(copy, graph);
    return copy;
  }

  private List<Integer> withoutSourceAndSink(GraphPath<Integer, E> path) {
    List<Integer> vertices = path.getVertexList();
    return vertices.size() < 3 ? List.of() : vertices.subList(1, vertices.size() - 1);
  }

  private Graph<Integer, E> newEmptyGraph() {
    Graph<Integer, E> graph = emptyGraphFactory.newEmptyGraph();
    checkIsEmpty(graph);
    checkGraphType(graph);
    return graph;
  }

  private void checkIsEmpty(Graph<?, ?> supplied) {
    Preconditions.checkState(
        supplied.vertexSet().isEmpty(), () -> "'emptyGraphFactory' provided a non-empty graph");
  }

  private void checkGraphType(Graph<?, ?> supplied) {
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

  public List<Integer> getPathCounts(int source) {
    return getPathCounts(source, Integer.MAX_VALUE);
  }

  public List<Integer> getPathCounts(int source, int maxFind) {
    return getPathCounts(source, maxFind, true);
  }

  public List<Integer> getPathCounts(int source, int maxFind, boolean allowParallel) {
    int nCounts = nVertices - graph.degreeOf(source) - 1; // Ignore adjacent vertices and source.
    return vertices(allowParallel)
        .filter(sink -> isPairValid(source, sink))
        .map(sink -> getPathCount(source, sink, maxFind))
        .collect(newCounts(nCounts), Collection::add, Collection::addAll);
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
    int nPairs = isDirected ? nVertices * (nVertices - 1) : nVertices * (nVertices - 1) / 2;
    return vertices(allowParallel)
        .flatMap(source -> uniqueSourceCounts(source, maxFind))
        .collect(newCounts(nPairs), Collection::add, Collection::addAll);
  }

  private IntStream uniqueSourceCounts(int source, int maxFind) {
    return vertices(false)
        .filter(sink -> isPairUnique(source, sink) && isPairValid(source, sink))
        .map(sink -> getPathCount(source, sink, maxFind));
  }

  private boolean isPairUnique(int source, int sink) {
    return isDirected ? source != sink : source < sink;
  }

  public List<Integer> getAllPathCounts() {
    return getAllPathCounts(true);
  }

  public List<Integer> getAllPathCounts(boolean allowParallel) {
    return getAllPathCounts(Integer.MAX_VALUE, allowParallel);
  }
}
