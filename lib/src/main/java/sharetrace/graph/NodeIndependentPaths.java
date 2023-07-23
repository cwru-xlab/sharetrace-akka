// package sharetrace.graph;
//
// import java.util.List;
// import java.util.concurrent.Callable;
// import java.util.concurrent.ExecutionException;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.Future;
// import org.jgrapht.Graph;
// import org.jgrapht.GraphPath;
// import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
// import org.jgrapht.alg.shortestpath.BidirectionalDijkstraShortestPath;
// import org.jgrapht.alg.shortestpath.SuurballeKDisjointShortestPaths;
// import org.jgrapht.graph.DefaultEdge;
// import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph;
// import org.jheaps.tree.FibonacciHeap;
// import sharetrace.util.Collecting;
//
/// **
// * Computes the approximate number of node-independent paths (<a
// * href=https://dx.doi.org/10.2139/ssrn.1831790>White and Newman 2011</a>).
// */
// public final class NodeIndependentPaths<V> {
//
//  private final Graph<V, ?> graph;
//  private final ExecutorService executorService;
//  private final boolean isDirected;
//  private final int nodeCount;
//  private final int pairCount;
//
//  public NodeIndependentPaths(Graph<V, ?> graph) {
//    this(graph, Executors.newWorkStealingPool());
//  }
//
//  public NodeIndependentPaths(Graph<V, ?> graph, ExecutorService executorService) {
//    this.graph = graph;
//    this.executorService = executorService;
//    this.isDirected = graph.getType().isDirected();
//    this.nodeCount = graph.vertexSet().size();
//    this.pairCount = nodeCount * (nodeCount - 1) / (isDirected ? 1 : 2);
//  }
//
//  public int compute(V source, V target, int maxFind) {
//    Callable<Integer> task = newTask(source, target, maxFind);
//    return getResult(executorService.submit(task));
//  }
//
//  public int compute(V source, V target) {
//    return compute(source, target, Integer.MAX_VALUE);
//  }
//
//  public List<Integer> computeForSource(V source) {
//    return computeForSource(source, Integer.MAX_VALUE);
//  }
//
//  public List<Integer> computeForSource(V source, int maxFind) {
//    return getResult(newTasks(source, maxFind));
//  }
//
//  public List<Integer> computeForAll() {
//    return computeForAll(Integer.MAX_VALUE);
//  }
//
//  public List<Integer> computeForAll(int maxFind) {
//    return getResult(newTasks(maxFind));
//  }
//
//  private static int getResult(Future<Integer> result) {
//    try {
//      return result.get();
//    } catch (InterruptedException exception) {
//      Thread.currentThread().interrupt();
//      throw new RuntimeException(exception);
//    } catch (ExecutionException exception) {
//      throw new RuntimeException(exception);
//    }
//  }
//
//  private Callable<Integer> newTask(V source, V target, int maxFind) {
//    return new Task<>(graph, isDirected, source, target, maxFind);
//  }
//
//  private List<Integer> getResult(List<Callable<Integer>> tasks) {
//    try {
//      return executorService.invokeAll(tasks).stream()
//          .map(NodeIndependentPaths::getResult)
//          .collect(Collecting.toUnmodifiableIntList(tasks.size()));
//    } catch (InterruptedException exception) {
//      Thread.currentThread().interrupt();
//      throw new RuntimeException(exception);
//    }
//  }
//
//  private List<Callable<Integer>> newTasks(V source, int maxFind) {
//    return graph.vertexSet().stream()
//        .filter(target -> !source.equals(target))
//        .map(target -> newTask(source, target, maxFind))
//        .collect(Collecting.toUnmodifiableList(nodeCount));
//  }
//
//  @SuppressWarnings("unchecked")
//  private List<Callable<Integer>> newTasks(int maxFind) {
//    V[] nodes = (V[]) graph.vertexSet().toArray();
//    List<Callable<Integer>> tasks = Collecting.newArrayList(pairCount);
//    for (int source = 0; source < nodes.length; source++) {
//      for (int target = 0; target < nodes.length; target++) {
//        if ((isDirected && source != target) || (!isDirected && source < target)) {
//          tasks.add(newTask(nodes[source], nodes[target], maxFind));
//        }
//      }
//    }
//    return tasks;
//  }
//
//  private static final class Task<V> implements Callable<Integer> {
//
//    private final Graph<V, ?> graph;
//    private final boolean isDirected;
//    private final V source;
//    private final V target;
//    private final int maxFind;
//
//    public Task(Graph<V, ?> graph, boolean isDirected, V source, V target, int maxFind) {
//      this.graph = graph;
//      this.isDirected = isDirected;
//      this.source = source;
//      this.target = target;
//      this.maxFind = maxFind;
//    }
//
//    @Override
//    public Integer call() {
//      int stopAt = Math.min(maxFind, maxPaths());
//      return stopAt > 0 ? compute(stopAt) : 0;
//    }
//
//    private static <V> List<V> withoutEndpoints(GraphPath<V, ?> path) {
//      List<V> nodes = path.getVertexList();
//      return nodes.size() < 3 ? List.of() : nodes.subList(1, nodes.size() - 1);
//    }
//
//    private static <V, E> ShortestPathAlgorithm<V, E> newShortestPaths(Graph<V, E> graph) {
//      // Fibonacci heap provides O(1) insert vs. pairing heap O(log n).
//      return new BidirectionalDijkstraShortestPath<>(graph, FibonacciHeap::new);
//    }
//
//    private int maxPaths() {
//      if (source.equals(target)) {
//        return 0;
//      } else if (isDirected) {
//        return Math.min(graph.outDegreeOf(source), graph.inDegreeOf(target));
//      } else {
//        return Math.min(graph.degreeOf(source), graph.degreeOf(target));
//      }
//    }
//
//    private int compute(int maxFind) {
//      return isAdjacent() ? computeAdjacent(maxFind) : computeNonadjacent(maxFind);
//    }
//
//    private boolean isAdjacent() {
//      return graph.getEdge(source, target) != null;
//    }
//
//    private int computeAdjacent(int maxFind) {
//      Graph<V, ?> graph = getDirectedCopy();
//      List<? extends GraphPath<V, ?>> paths = shortestPaths(graph);
//      int found = 1; // Trivial path along edge incident to source and target.
//      while (paths.size() > 1 && found < maxFind) {
//        found++;
//        GraphPath<V, ?> nontrivialPath = paths.get(1);
//        graph.removeAllVertices(withoutEndpoints(nontrivialPath));
//        paths = shortestPaths(graph);
//      }
//      return found;
//    }
//
//    private int computeNonadjacent(int maxFind) {
//      Graph<Integer, DefaultEdge> graph = Graphs.copy(this.graph);
//      ShortestPathAlgorithm<V, ?> shortestPaths = newShortestPaths(graph);
//      GraphPath<V, ?> path = shortestPaths.getPath(source, target);
//      int found = 0;
//      while (path != null && found < maxFind) {
//        found++;
//        graph.removeAllVertices(withoutEndpoints(path));
//        path = shortestPaths.getPath(source, target);
//      }
//      return found;
//    }
//
//    private Graph<V, ?> getDirectedCopy() {
//      return isDirected ? Graphs.copyDirected(graph) : Graphs.asDirected(graph);
//    }
//
//    private List<? extends GraphPath<V, ?>> shortestPaths(Graph<V, ?> graph) {
//      // Suurballe provides a simpler implementation since it ensures no loops.
//      // Suurballe copies internally, so we must pass in the graph on every call.
//      return new SuurballeKDisjointShortestPaths<>(graph).getPaths(source, target, 2);
//    }
//  }
// }
