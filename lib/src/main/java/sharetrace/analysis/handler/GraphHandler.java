package sharetrace.analysis.handler;

import java.nio.file.Path;
import org.jgrapht.Graph;
import org.jgrapht.GraphMetrics;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.VertexScoringAlgorithm;
import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.alg.scoring.ClusteringCoefficient;
import org.jgrapht.alg.scoring.Coreness;
import org.jgrapht.alg.scoring.EigenvectorCentrality;
import org.jgrapht.alg.scoring.KatzCentrality;
import org.jgrapht.alg.shortestpath.GraphMeasurer;
import org.jgrapht.alg.shortestpath.IntVertexDijkstraShortestPath;
import sharetrace.graph.Graphs;
import sharetrace.graph.TemporalEdge;
import sharetrace.graph.TemporalGraphExporterBuilder;
import sharetrace.logging.event.ContactEvent;
import sharetrace.logging.event.Event;
import sharetrace.util.Statistics;

public final class GraphHandler implements EventHandler {

  private final String graphId;
  private final Graph<Integer, TemporalEdge> graph;

  public GraphHandler(String graphId) {
    this.graphId = graphId;
    this.graph = Graphs.newTemporalGraph();
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof ContactEvent e) {
      Graphs.addEdgeWithNodes(graph, e.self(), e.contact(), e.contactTime());
    }
  }

  @Override
  public void onComplete() {
    var girth = GraphMetrics.getGirth(graph);
    var triangleCount = GraphMetrics.getNumberOfTriangles(graph);
    var shortestPathsAlgorithm = new IntVertexDijkstraShortestPath<>(graph);
    var measurer = new GraphMeasurer<>(graph, shortestPathsAlgorithm);
    var radius = (long) measurer.getRadius();
    var diameter = (long) measurer.getDiameter();
    var centerCount = measurer.getGraphCenter().size();
    var peripheryCount = measurer.getGraphPeriphery().size();
    var degeneracy = degeneracy();
    var globalClusteringCoefficient = globalClusteringCoefficient();
    var localClusteringCoefficient = localClusteringCoefficients();
    var harmonicCentrality = harmonicCentrality(shortestPathsAlgorithm);
    var katzCentrality = katzCentrality();
    var eigenvectorCentrality = eigenvectorCentrality();
    TemporalGraphExporterBuilder.create()
        .directory(Path.of("."))
        .filename(graphId)
        .build()
        .export(graph);
  }

  private long degeneracy() {
    return new Coreness<>(graph).getDegeneracy();
  }

  private double globalClusteringCoefficient() {
    return new ClusteringCoefficient<>(graph).getGlobalClusteringCoefficient();
  }

  private Statistics localClusteringCoefficients() {
    return scoreStatistics(new ClusteringCoefficient<>(graph));
  }

  private Statistics harmonicCentrality(
      ShortestPathAlgorithm<Integer, TemporalEdge> shortestPathAlgorithm) {
    return scoreStatistics(new IntHarmonicCentrality<>(graph, shortestPathAlgorithm));
  }

  private Statistics katzCentrality() {
    return scoreStatistics(new KatzCentrality<>(graph));
  }

  private Statistics eigenvectorCentrality() {
    return scoreStatistics(new EigenvectorCentrality<>(graph));
  }

  private Statistics scoreStatistics(VertexScoringAlgorithm<?, Double> algorithm) {
    return Statistics.of(algorithm.getScores().values());
  }

  private static final class IntHarmonicCentrality<E> extends ClosenessCentrality<Integer, E> {

    private final ShortestPathAlgorithm<Integer, E> shortestPathAlgorithm;

    public IntHarmonicCentrality(
        Graph<Integer, E> graph, ShortestPathAlgorithm<Integer, E> shortestPathAlgorithm) {
      super(graph);
      this.shortestPathAlgorithm = shortestPathAlgorithm;
    }

    @Override
    protected ShortestPathAlgorithm<Integer, E> getShortestPathAlgorithm() {
      return shortestPathAlgorithm;
    }
  }
}
