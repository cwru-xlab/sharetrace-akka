// package sharetrace.graph;
//
// import java.util.concurrent.atomic.AtomicInteger;
// import java.util.function.Supplier;
// import org.jgrapht.Graph;
// import org.jgrapht.GraphTests;
// import org.jgrapht.GraphType;
// import org.jgrapht.graph.DefaultEdge;
// import org.jgrapht.graph.DefaultGraphType;
// import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph;
//
// public final class Graphs {
//
//  private Graphs() {}
//
//  public static <V, E> Graph<Integer, DefaultEdge> copy(Graph<V, E> graph) {
//    return graph.getType().isDirected() ? copyDirected(graph) : copyUndirected(graph);
//  }
//
//  public static <V, E> Graph<Integer, DefaultEdge> copyDirected(Graph<V, E> directed) {
//    return copy(GraphTests.requireDirected(directed), newDirectedGraph());
//  }
//
//  public static <V, E> Graph<Integer, DefaultEdge> copyUndirected(Graph<V, E> undirected) {
//    return copy(GraphTests.requireUndirected(undirected), newUndirectedGraph());
//  }
//
//  private static <V, E> Graph<Integer, DefaultEdge> copy(Graph<V, E> source, Graph<Integer,
// DefaultEdge> target) {
//    org.jgrapht.Graphs.addGraph(target, source);
//    return target;
//  }
//
//  public static Graph<Integer, DefaultEdge> newDirectedGraph() {
//    return newGraph(DefaultGraphType.directedSimple());
//  }
//
//  public static Graph<Integer, DefaultEdge> newUndirectedGraph() {
//    return newGraph(DefaultGraphType.simple());
//  }
//
//  private static Graph<Integer, DefaultEdge> newGraph(GraphType graphType) {
//    Supplier<Integer> vertexFactory = new AtomicInteger(0)::getAndIncrement;
//    return new FastutilMapIntVertexGraph<>(vertexFactory, DefaultEdge::new, graphType, false);
//  }
//
//  public static <E> Graph<Integer, E> asDirected(Graph<Integer, E> graph) {
//    Graph<Integer, E> directed;
//    if (graph.getType().isDirected()) {
//      directed = graph;
//    } else {
//      directed = newDirectedGraph();
//      for (E edge : graph.edgeSet()) {
//        int source = graph.getEdgeSource(edge);
//        int target = graph.getEdgeTarget(edge);
//        directed.addVertex(source);
//        directed.addVertex(target);
//        directed.addEdge(source, target);
//        directed.addEdge(target, source);
//      }
//    }
//    return directed;
//  }
// }
