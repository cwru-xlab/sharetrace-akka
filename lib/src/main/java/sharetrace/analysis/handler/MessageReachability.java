package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.IntVertexDijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import sharetrace.analysis.results.Results;
import sharetrace.graph.Graphs;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.ReceiveEvent;

public final class MessageReachability implements EventHandler {

  private final Int2ObjectMap<Set<IntList>> edges;

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
        edges.computeIfAbsent(origin, x -> new HashSet<>()).add(IntArrayList.of(source, target));
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
      reachability.put(origin, messageReachability(origin, graph));
    }
    results
        .withScope("reachability")
        .put("influence", influence)
        .put("source", source)
        .put("message", reachability)
        .put("ratio", reachabilityRatio(influence));
  }

  private IntSet targetsOfOrigin(int origin, Iterable<IntList> edges) {
    var targets = new IntOpenHashSet();
    edges.forEach(targets::addAll);
    targets.remove(origin);
    return targets;
  }

  private Graph<Integer, DefaultEdge> reachabilityGraph(Iterable<IntList> edges) {
    var graph = Graphs.newDirectedGraph();
    for (var edge : edges) {
      var source = edge.getInt(0);
      var target = edge.getInt(1);
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
