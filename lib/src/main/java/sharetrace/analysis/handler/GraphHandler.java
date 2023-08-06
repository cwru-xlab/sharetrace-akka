package sharetrace.analysis.handler;

import java.nio.file.Path;
import java.time.Instant;
import java.util.function.BinaryOperator;
import org.jgrapht.Graph;
import org.jgrapht.GraphMetrics;
import org.jgrapht.alg.interfaces.VertexScoringAlgorithm;
import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.alg.scoring.ClusteringCoefficient;
import org.jgrapht.alg.scoring.Coreness;
import org.jgrapht.alg.scoring.EigenvectorCentrality;
import org.jgrapht.alg.scoring.KatzCentrality;
import org.jgrapht.alg.shortestpath.GraphMeasurer;
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
    if (event instanceof ContactEvent contact) {
      var source = contact.self();
      var target = contact.contact();
      graph.addVertex(source);
      graph.addVertex(target);
      var edge = graph.getEdge(source, target);
      if (edge != null) {
        edge.mergeTimestamp(contact.contactTime(), BinaryOperator.maxBy(Instant::compareTo));
      } else {
        edge = graph.addEdge(source, target);
      }
      graph.setEdgeWeight(edge, edge.weight());
    }
  }

  @Override
  public void onComplete() {
    var girth = GraphMetrics.getGirth(graph);
    var triangleCount = GraphMetrics.getNumberOfTriangles(graph);
    var measurer = new GraphMeasurer<>(graph);
    var radius = (long) measurer.getRadius();
    var diameter = (long) measurer.getDiameter();
    var centerCount = measurer.getGraphCenter().size();
    var peripheryCount = measurer.getGraphPeriphery().size();
    var degeneracy = degeneracy();
    var globalClusteringCoefficient = globalClusteringCoefficient();
    var localClusteringCoefficient = localClusteringCoefficients();
    var closenessCentrality = closenessCentrality();
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

  private Statistics closenessCentrality() {
    return scoreStatistics(new ClosenessCentrality<>(graph));
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
}
