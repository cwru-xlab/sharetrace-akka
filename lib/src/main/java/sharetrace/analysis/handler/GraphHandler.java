package sharetrace.analysis.handler;

import org.jgrapht.Graph;
import org.jgrapht.GraphMetrics;
import org.jgrapht.alg.interfaces.VertexScoringAlgorithm;
import org.jgrapht.alg.scoring.ClusteringCoefficient;
import org.jgrapht.alg.scoring.Coreness;
import org.jgrapht.alg.scoring.EigenvectorCentrality;
import org.jgrapht.alg.scoring.HarmonicCentrality;
import org.jgrapht.alg.scoring.KatzCentrality;
import org.jgrapht.alg.shortestpath.GraphMeasurer;
import org.jgrapht.alg.shortestpath.IntVertexDijkstraShortestPath;
import sharetrace.analysis.collector.ResultsCollector;
import sharetrace.graph.Graphs;
import sharetrace.graph.TemporalEdge;
import sharetrace.logging.event.ContactEvent;
import sharetrace.logging.event.Event;
import sharetrace.util.Statistics;

public final class GraphHandler implements EventHandler {

  private final Graph<Integer, TemporalEdge> graph;

  public GraphHandler() {
    this.graph = Graphs.newTemporalGraph();
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof ContactEvent e)
      Graphs.addTemporalEdge(graph, e.self(), e.contact(), e.contactTime());
  }

  @Override
  public void onComplete(ResultsCollector collector) {
    var measurer = new GraphMeasurer<>(graph, new IntVertexDijkstraShortestPath<>(graph));
    collector = collector.withScope("graph");
    collector
        .put("girth", GraphMetrics.getGirth(graph))
        .put("triangles", GraphMetrics.getNumberOfTriangles(graph))
        .put("radius", measurer.getRadius())
        .put("diameter", measurer.getGraphCenter().size())
        .put("periphery", measurer.getGraphPeriphery().size())
        .put("degeneracy", new Coreness<>(graph).getDegeneracy());
    collector
        .withScope("clustering")
        .put("global", globalClusteringCoefficient())
        .put("local", scoreStats(new ClusteringCoefficient<>(graph)));
    collector
        .withScope("centrality")
        .put("harmonic", scoreStats(new HarmonicCentrality<>(graph)))
        .put("katz", scoreStats(new KatzCentrality<>(graph)))
        .put("eigenvector", scoreStats(new EigenvectorCentrality<>(graph)));
  }

  private double globalClusteringCoefficient() {
    return new ClusteringCoefficient<>(graph).getGlobalClusteringCoefficient();
  }

  private Statistics scoreStats(VertexScoringAlgorithm<?, ? extends Number> algorithm) {
    return Statistics.of(algorithm.getScores().values());
  }
}
