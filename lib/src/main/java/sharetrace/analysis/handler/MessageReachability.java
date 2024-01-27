package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Objects;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.IntVertexDijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import sharetrace.analysis.results.Results;
import sharetrace.graph.Graphs;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.user.ReceiveEvent;

public final class MessageReachability implements EventHandler {

  private final Int2ReferenceMap<Set<IntIntPair>> edges;

  public MessageReachability() {
    edges = new Int2ReferenceOpenHashMap<>();
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof ReceiveEvent receive) {
      var source = receive.contact();
      var target = receive.self();
      if (source != target) {
        var origin = receive.message().origin();
        edges
            .computeIfAbsent(origin, x -> new ObjectOpenHashSet<>())
            .add(IntIntPair.of(source, target));
      }
    }
  }

  @Override
  public void onComplete(Results results) {
    var influence = new Int2IntOpenHashMap(edges.size());
    var source = new Int2IntOpenHashMap(edges.size());
    var reachability = new Int2IntOpenHashMap(edges.size());
    for (var entry : edges.int2ReferenceEntrySet()) {
      var origin = entry.getIntKey();
      var edges = entry.getValue();
      var targets = targetsOfOrigin(origin, edges);
      var graph = reachabilityGraph(edges);
      influence.put(origin, targets.size());
      targets.forEach(target -> source.addTo(target, 1));
      reachability.put(origin, messageReachability(origin, graph));
    }
    results
        .withScope("reachability")
        .put("influence", influence)
        .put("source", source)
        .put("message", reachability)
        .put("ratio", reachabilityRatio(influence));
  }

  private IntSet targetsOfOrigin(int origin, Iterable<IntIntPair> edges) {
    var targets = new IntOpenHashSet();
    for (var edge : edges) {
      targets.add(edge.leftInt());
      targets.add(edge.rightInt());
    }
    targets.remove(origin);
    return targets;
  }

  private Graph<Integer, DefaultEdge> reachabilityGraph(Iterable<IntIntPair> edges) {
    var graph = Graphs.newDirectedGraph();
    for (var edge : edges) {
      var source = edge.leftInt();
      var target = edge.rightInt();
      graph.addVertex(source);
      graph.addVertex(target);
      graph.addEdge(source, target);
    }
    return graph;
  }

  private int messageReachability(int origin, Graph<Integer, ?> graph) {
    return graph.vertexSet().stream()
        .map(new IntVertexDijkstraShortestPath<>(graph).getPaths(origin)::getPath)
        .filter(Objects::nonNull)
        .mapToInt(GraphPath::getLength)
        .max()
        .orElse(0);
  }

  private double reachabilityRatio(Int2IntMap influence) {
    return influence.values().intStream().summaryStatistics().getAverage();
  }
}
