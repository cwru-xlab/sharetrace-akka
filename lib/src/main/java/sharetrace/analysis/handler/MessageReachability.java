package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.IntVertexDijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import sharetrace.analysis.results.Results;
import sharetrace.graph.Graphs;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.ReceiveEvent;

public final class MessageReachability implements EventHandler {

  private final Int2ObjectMap<List<int[]>> edges;

  public MessageReachability() {
    edges = new Int2ObjectOpenHashMap<>();
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof ReceiveEvent receive) {
      var source = receive.contact();
      var target = receive.self();
      if (source != target) {
        var origin = receive.message().id();
        edges.computeIfAbsent(origin, x -> new ArrayList<>()).add(new int[] {source, target});
      }
    }
  }

  @Override
  public void onComplete(Results results) {
    var influence = new Int2IntOpenHashMap(edges.size());
    var source = new Int2IntOpenHashMap(edges.size());
    var reachability = new Int2IntOpenHashMap(edges.size());
    for (var entry : edges.int2ObjectEntrySet()) {
      var origin = entry.getIntKey();
      var edges = entry.getValue();
      var targets = targetsOfOrigin(origin, edges);
      var graph = reachabilityGraph(edges);
      influence.put(origin, targets.size());
      targets.forEach(target -> source.mergeInt(target, 1, Integer::sum));
      reachability.put(origin, computeMessageReachability(origin, graph));
    }
    results
        .withScope("reachability")
        .put("influence", influence)
        .put("source", source)
        .put("message", reachability)
        .put("ratio", computeReachabilityRatio(influence));
  }

  private IntSet targetsOfOrigin(int origin, Collection<int[]> edges) {
    var targets = new IntOpenHashSet(edges.size());
    for (var edge : edges) {
      if (edge[0] != origin) targets.add(edge[0]);
      if (edge[1] != origin) targets.add(edge[1]);
    }
    return targets;
  }

  private Graph<Integer, DefaultEdge> reachabilityGraph(Iterable<int[]> edges) {
    var graph = Graphs.newDirectedGraph();
    for (var edge : edges) {
      graph.addVertex(edge[0]);
      graph.addVertex(edge[1]);
      graph.addEdge(edge[0], edge[1]);
    }
    return graph;
  }

  private int computeMessageReachability(int origin, Graph<Integer, ?> graph) {
    return graph.vertexSet().stream()
        .map(new IntVertexDijkstraShortestPath<>(graph).getPaths(origin)::getPath)
        .filter(Objects::nonNull)
        .mapToInt(GraphPath::getLength)
        .max()
        .orElse(0);
  }

  private double computeReachabilityRatio(Int2IntMap influence) {
    return influence.values().intStream().summaryStatistics().getAverage();
  }
}
