package io.sharetrace.graph;

import io.sharetrace.util.Collecting;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.BidirectionalDijkstraShortestPath;
import org.jgrapht.alg.shortestpath.SuurballeKDisjointShortestPaths;
import org.jgrapht.graph.DefaultEdge;
import org.jheaps.tree.FibonacciHeap;

import java.util.List;
import java.util.concurrent.*;

/**
 * Computes the approximate number of vertex-independent paths (<a
 * href=https://dx.doi.org/10.2139/ssrn.1831790>White and Newman 2011</a>). The API allows for
 * serial or parallel (default) execution when finding the paths between a single source and
 * multiple target vertices or when finding the paths between all pairs of vertices.
 */
public final class VertexIndependentPaths {

    private final Graph<Integer, DefaultEdge> graph;
    private final ExecutorService executorService;
    private final boolean isDirected;
    private final int numVertices;
    private final int numPairs;

    public VertexIndependentPaths(Graph<Integer, DefaultEdge> graph) {
        this(graph, Executors.newWorkStealingPool());
    }

    public VertexIndependentPaths(
            Graph<Integer, DefaultEdge> graph, ExecutorService executorService) {
        this.graph = graph;
        this.executorService = executorService;
        this.isDirected = graph.getType().isDirected();
        this.numVertices = graph.vertexSet().size();
        this.numPairs = numVertices * (numVertices - 1) / (isDirected ? 1 : 2);
    }

    public int compute(int source, int target) {
        return compute(source, target, Integer.MAX_VALUE);
    }

    public int compute(int source, int target, int maxFind) {
        Callable<Integer> task = newTask(source, target, maxFind);
        return getResult(executorService.submit(task));
    }

    private Callable<Integer> newTask(int source, int target, int maxFind) {
        return new Task(graph, isDirected, source, target, maxFind);
    }

    private static int getResult(Future<Integer> result) {
        try {
            return result.get();
        } catch (InterruptedException | ExecutionException exception) {
            throw new RuntimeException(exception);
        }
    }

    public List<Integer> computeForSource(int source) {
        return computeForSource(source, Integer.MAX_VALUE);
    }

    public List<Integer> computeForSource(int source, int maxFind) {
        return getResult(newTasks(source, maxFind));
    }

    private List<Integer> getResult(List<Callable<Integer>> tasks) {
        try {
            return executorService.invokeAll(tasks).stream()
                    .map(VertexIndependentPaths::getResult)
                    .collect(Collecting.toImmutableIntList(tasks.size()));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Callable<Integer>> newTasks(int source, int maxFind) {
        return graph.vertexSet().stream()
                .filter(target -> source != target)
                .map(target -> newTask(source, target, maxFind))
                .collect(Collecting.toImmutableList(numVertices));
    }

    public List<Integer> computeForAll() {
        return computeForAll(Integer.MAX_VALUE);
    }

    public List<Integer> computeForAll(int maxFind) {
        return getResult(newTasks(maxFind));
    }

    private List<Callable<Integer>> newTasks(int maxFind) {
        List<Callable<Integer>> tasks = Collecting.newArrayList(numPairs);
        for (int source : graph.vertexSet()) {
            for (int target : graph.vertexSet()) {
                if ((isDirected && source != target) || (!isDirected && source < target)) {
                    tasks.add(newTask(source, target, maxFind));
                }
            }
        }
        return tasks;
    }

    private static final class Task implements Callable<Integer> {

        private static final int ADJACENT_PATHS = 2;

        private final Graph<Integer, DefaultEdge> graph;
        private final boolean isDirected;
        private final int source;
        private final int target;
        private final int maxFind;

        public Task(
                Graph<Integer, DefaultEdge> graph,
                boolean isDirected,
                int source,
                int target,
                int maxFind) {
            this.graph = graph;
            this.isDirected = isDirected;
            this.source = source;
            this.target = target;
            this.maxFind = maxFind;
        }

        @Override
        public Integer call() throws Exception {
            int stopAt = Math.min(maxFind, maxPaths());
            return (stopAt > 0) ? compute(stopAt) : 0;
        }

        private int maxPaths() {
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

        private int compute(int maxFind) {
            return isAdjacent() ? computeForAdjacent(maxFind) : computeForNonadjacent(maxFind);
        }

        private boolean isAdjacent() {
            return graph.getEdge(source, target) != null;
        }

        private int computeForAdjacent(int maxFind) {
            Graph<Integer, ?> graph = getDirectedCopy();
            List<? extends GraphPath<Integer, ?>> paths = kShortestPaths(graph);
            int numFound = 1; // Trivial path along edge incident to source and target.
            while (paths.size() > 1 && numFound < maxFind) {
                numFound++;
                GraphPath<Integer, ?> nontrivialPath = paths.get(1);
                graph.removeAllVertices(withoutEndpoints(nontrivialPath));
                paths = kShortestPaths(graph);
            }
            return numFound;
        }

        private int computeForNonadjacent(int maxFind) {
            Graph<Integer, ?> graph = Graphs.copy(this.graph);
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

        private Graph<Integer, DefaultEdge> getDirectedCopy() {
            return isDirected ? Graphs.copyDirected(graph) : Graphs.asDirected(graph);
        }

        private List<? extends GraphPath<Integer, ?>> kShortestPaths(Graph<Integer, ?> graph) {
            // Suurballe provides a simpler implementation since it ensures no loops.
            // Suurballe copies internally, so we must pass in the graph on every call.
            return new SuurballeKDisjointShortestPaths<>(graph).getPaths(source, target, ADJACENT_PATHS);
        }

        private static <V> List<V> withoutEndpoints(GraphPath<V, ?> path) {
            List<V> vertices = path.getVertexList();
            return (vertices.size() < 3) ? List.of() : vertices.subList(1, vertices.size() - 1);
        }

        private static <V, E> ShortestPathAlgorithm<V, E> newShortestPaths(Graph<V, E> graph) {
            // Fibonacci heap provides O(1) insert vs. pairing heap O(log n).
            return new BidirectionalDijkstraShortestPath<>(graph, FibonacciHeap::new);
        }
    }
}
