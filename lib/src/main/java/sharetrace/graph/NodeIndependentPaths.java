package sharetrace.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.BidirectionalDijkstraShortestPath;
import org.jgrapht.alg.shortestpath.SuurballeKDisjointShortestPaths;
import org.jgrapht.graph.DefaultEdge;
import org.jheaps.tree.FibonacciHeap;

/**
 * Computes the approximate number of node-independent paths (<a
 * href=https://dx.doi.org/10.2139/ssrn.1831790>White and Newman 2011</a>).
 */
public record NodeIndependentPaths(
    Graph<Integer, DefaultEdge> graph, ExecutorService executorService) {

  public NodeIndependentPaths(Graph<Integer, DefaultEdge> graph) {
    this(graph, Executors.newWorkStealingPool());
  }

  public int compute(int source, int target) {
    return compute(source, target, Integer.MAX_VALUE);
  }

  public int compute(int source, int target, int maxFind) {
    var task = newTask(source, target, maxFind);
    return getResult(executorService.submit(task));
  }

  public List<Integer> computeForSource(int source) {
    return computeForSource(source, Integer.MAX_VALUE);
  }

  public List<Integer> computeForSource(int source, int maxFind) {
    return getResults(newTasks(source, maxFind));
  }

  public List<Integer> computeForAll() {
    return computeForAll(Integer.MAX_VALUE);
  }

  public List<Integer> computeForAll(int maxFind) {
    return getResults(newTasks(maxFind));
  }

  private List<Callable<Integer>> newTasks(int source, int maxFind) {
    return graph.vertexSet().stream()
        .filter(target -> source != target)
        .map(target -> newTask(source, target, maxFind))
        .toList();
  }

  private List<Callable<Integer>> newTasks(int maxFind) {
    var tasks = new ArrayList<Callable<Integer>>();
    var isDirected = graph.getType().isDirected();
    for (int source : graph.vertexSet()) {
      for (int target : graph.vertexSet()) {
        if ((isDirected && source != target) || (!isDirected && source < target)) {
          tasks.add(newTask(source, target, maxFind));
        }
      }
    }
    return tasks;
  }

  private Callable<Integer> newTask(int source, int target, int maxFind) {
    return new Task(graph, source, target, maxFind);
  }

  private List<Integer> getResults(List<Callable<Integer>> tasks) {
    try {
      return executorService.invokeAll(tasks).stream().map(this::getResult).toList();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(exception);
    }
  }

  private int getResult(Future<Integer> result) {
    try {
      return result.get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(exception);
    } catch (ExecutionException exception) {
      throw new RuntimeException(exception);
    }
  }

  private record Task(Graph<Integer, DefaultEdge> graph, int source, int target, int maxFind)
      implements Callable<Integer> {

    @Override
    public Integer call() {
      var stopAt = Math.min(maxFind, maxPaths());
      return stopAt > 0 ? compute(stopAt) : 0;
    }

    private int maxPaths() {
      if (source == target) {
        return 0;
      } else if (graph.getType().isDirected()) {
        return Math.min(graph.outDegreeOf(source), graph.inDegreeOf(target));
      } else {
        return Math.min(graph.degreeOf(source), graph.degreeOf(target));
      }
    }

    private int compute(int maxFind) {
      var isAdjacent = graph.getEdge(source, target) != null;
      return isAdjacent ? computeAdjacent(maxFind) : computeNonadjacent(maxFind);
    }

    private int computeAdjacent(int maxFind) {
      var graph = getDirectedCopy();
      var paths = shortestPaths(graph);
      var found = 1; // Trivial path along the edge incident to the source and target
      while (paths.size() > 1 && found < maxFind) {
        found++;
        var nontrivialPath = paths.get(1);
        graph.removeAllVertices(withoutEndpoints(nontrivialPath));
        paths = shortestPaths(graph);
      }
      return found;
    }

    private int computeNonadjacent(int maxFind) {
      var graph = getCopy();
      var shortestPaths = shortestPathsAlgorithm(graph);
      var path = shortestPaths.getPath(source, target);
      var found = 0;
      while (path != null && found < maxFind) {
        found++;
        graph.removeAllVertices(withoutEndpoints(path));
        path = shortestPaths.getPath(source, target);
      }
      return found;
    }

    private Graph<Integer, ?> getDirectedCopy() {
      return graph.getType().isDirected() ? Graphs.copyDirected(graph) : Graphs.asDirected(graph);
    }

    private Graph<Integer, ?> getCopy() {
      return Graphs.copy(graph);
    }

    @SuppressWarnings("SpellCheckingInspection")
    private List<? extends GraphPath<Integer, ?>> shortestPaths(Graph<Integer, ?> graph) {
      // Suurballe provides a simpler implementation since it ensures no loops.
      // Suurballe copies internally, so we must pass in the graph on every call.
      return new SuurballeKDisjointShortestPaths<>(graph).getPaths(source, target, 2);
    }

    private <V, E> ShortestPathAlgorithm<V, E> shortestPathsAlgorithm(Graph<V, E> graph) {
      // Fibonacci heap provides O(1) insert vs. pairing heap O(log n).
      return new BidirectionalDijkstraShortestPath<>(graph, FibonacciHeap::new);
    }

    private <V> List<V> withoutEndpoints(GraphPath<V, ?> path) {
      var nodes = path.getVertexList();
      return nodes.size() < 3 ? List.of() : nodes.subList(1, nodes.size() - 1);
    }
  }
}
