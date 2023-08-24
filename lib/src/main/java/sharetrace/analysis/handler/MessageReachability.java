package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.IntVertexDijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import sharetrace.analysis.appender.ResultsCollector;
import sharetrace.analysis.model.ReachabilityResult;
import sharetrace.analysis.model.ReachabilityResultBuilder;
import sharetrace.graph.Graphs;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.ReceiveEvent;

@SuppressWarnings("SpellCheckingInspection")
public final class MessageReachability implements EventHandler {

  private final Long2IntMap origins;
  private final Int2ObjectMap<List<int[]>> knowns;
  private final Long2ObjectMap<List<int[]>> unknowns;

  public MessageReachability() {
    origins = new Long2IntOpenHashMap();
    knowns = new Int2ObjectOpenHashMap<>();
    unknowns = new Long2ObjectOpenHashMap<>();
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof ReceiveEvent receive) {
      var source = receive.contact();
      var target = receive.self();
      var id = receive.message().id();
      if (source == target) {
        addNewOrigin(id, source);
      } else if (isOriginKnown(id)) {
        addEdgeOfKnownOrigin(id, source, target);
      } else {
        addEdgeOfUnknownOrigin(id, source, target);
      }
    }
  }

  @Override
  public void onComplete() {
    resolveUnknowns();
    computeReachability();
  }

  private void addNewOrigin(long id, int origin) {
    origins.put(id, origin);
    knowns.put(origin, new ArrayList<>());
  }

  private boolean isOriginKnown(long id) {
    return origins.containsKey(id);
  }

  private List<int[]> getEdgesOfKnownOrigin(long id) {
    return knowns.get(origins.get(id));
  }

  private void addEdgeOfKnownOrigin(long id, int source, int target) {
    getEdgesOfKnownOrigin(id).add(new int[] {source, target});
  }

  private void addEdgeOfUnknownOrigin(long id, int source, int target) {
    unknowns.computeIfAbsent(id, x -> new ArrayList<>()).add(new int[] {source, target});
  }

  private void resolveUnknowns() {
    for (var entry : unknowns.long2ObjectEntrySet()) {
      var id = entry.getLongKey();
      if (isOriginKnown(id)) {
        var newEdges = entry.getValue();
        getEdgesOfKnownOrigin(id).addAll(newEdges);
      }
    }
  }

  private ReachabilityResult computeReachability() {
    var influenceCounts = new Int2IntOpenHashMap(knowns.size());
    var sourceCounts = new Int2IntOpenHashMap(knowns.size());
    var reachabilities = new Int2IntOpenHashMap(knowns.size());
    for (var entry : knowns.int2ObjectEntrySet()) {
      var origin = entry.getIntKey();
      var edges = entry.getValue();
      var targets = targetsOfOrigin(origin, edges);
      var graph = buildReachabilityGraph(edges);
      influenceCounts.put(origin, targets.size());
      targets.forEach(target -> sourceCounts.mergeInt(target, 1, Integer::sum));
      reachabilities.put(origin, computeMessageReachability(origin, graph));
    }
    return ReachabilityResultBuilder.create()
        .influenceCounts(influenceCounts)
        .sourceCounts(sourceCounts)
        .messageReachabilities(reachabilities)
        .reachabilityRatio(computeReachabilityRatio(influenceCounts))
        .build();
  }

  private IntSet targetsOfOrigin(int origin, Collection<int[]> edges) {
    var targets = new IntOpenHashSet(edges.size());
    for (var edge : edges) {
      addIfNotOrigin(edge[0], origin, targets);
      addIfNotOrigin(edge[1], origin, targets);
    }
    return targets;
  }

  private void addIfNotOrigin(int target, int origin, Collection<Integer> targets) {
    if (target != origin) {
      targets.add(target);
    }
  }

  private Graph<Integer, DefaultEdge> buildReachabilityGraph(Iterable<int[]> edges) {
    var graph = Graphs.newDirectedGraph();
    for (var edge : edges) {
      graph.addVertex(edge[0]);
      graph.addVertex(edge[1]);
      graph.addEdge(edge[0], edge[1]);
    }
    return graph;
  }

  private int computeMessageReachability(int origin, Graph<Integer, ?> graph) {
    if (!graph.containsVertex(origin)) {
      return 0;
    }
    var shortestPaths = new IntVertexDijkstraShortestPath<>(graph).getPaths(origin);
    return graph.vertexSet().stream()
        .map(shortestPaths::getPath)
        .filter(Objects::nonNull)
        .mapToInt(GraphPath::getLength)
        .max()
        .orElse(0);
  }

  private double computeReachabilityRatio(Int2IntMap influenceCounts) {
    return influenceCounts.values().intStream().summaryStatistics().getAverage();
  }
}
