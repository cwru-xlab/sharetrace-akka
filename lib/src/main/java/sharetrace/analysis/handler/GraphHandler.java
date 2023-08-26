package sharetrace.analysis.handler;

import java.nio.file.Path;
import org.jgrapht.Graph;
import org.jgrapht.GraphMetrics;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.alg.scoring.ClusteringCoefficient;
import org.jgrapht.alg.scoring.Coreness;
import org.jgrapht.alg.scoring.EigenvectorCentrality;
import org.jgrapht.alg.scoring.KatzCentrality;
import org.jgrapht.alg.shortestpath.GraphMeasurer;
import org.jgrapht.alg.shortestpath.IntVertexDijkstraShortestPath;
import sharetrace.analysis.appender.ResultsCollector;
import sharetrace.analysis.model.NumericResult;
import sharetrace.analysis.model.Result;
import sharetrace.analysis.model.StatisticsResult;
import sharetrace.graph.Graphs;
import sharetrace.graph.TemporalEdge;
import sharetrace.graph.TemporalGraphExporterBuilder;
import sharetrace.logging.event.ContactEvent;
import sharetrace.logging.event.Event;

public final class GraphHandler implements EventHandler {

  private final String graphId;
  private final Graph<Integer, TemporalEdge> graph;

  public GraphHandler(String graphId) {
    this.graphId = graphId;
    this.graph = Graphs.newTemporalGraph();
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof ContactEvent e)
      Graphs.addTemporalEdge(graph, e.self(), e.contact(), e.contactTime());
  }

  @Override
  public void onComplete(ResultsCollector collector) {
    var shortestPathsAlgorithm = new IntVertexDijkstraShortestPath<>(graph);
    var measurer = new GraphMeasurer<>(graph, shortestPathsAlgorithm);
    collector
        .add(new NumericResult("Girth", GraphMetrics.getGirth(graph)))
        .add(new NumericResult("Triangles", GraphMetrics.getNumberOfTriangles(graph)))
        .add(new NumericResult("Radius", measurer.getRadius()))
        .add(new NumericResult("Diameter", measurer.getDiameter()))
        .add(new NumericResult("Center", measurer.getGraphCenter().size()))
        .add(new NumericResult("Periphery", measurer.getGraphPeriphery().size()))
        .add(new NumericResult("Degeneracy", new Coreness<>(graph).getDegeneracy()))
        .add(globalClusteringCoefficient())
        .add(StatisticsResult.from(new ClusteringCoefficient<>(graph)))
        .add(StatisticsResult.from(new HarmonicCentrality<>(graph, shortestPathsAlgorithm)))
        .add(StatisticsResult.from(new KatzCentrality<>(graph)))
        .add(StatisticsResult.from(new EigenvectorCentrality<>(graph)));
    TemporalGraphExporterBuilder.create()
        .directory(Path.of("."))
        .filename(graphId)
        .build()
        .export(graph);
  }

  private Result<?> globalClusteringCoefficient() {
    var coefficient = new ClusteringCoefficient<>(graph).getGlobalClusteringCoefficient();
    return new NumericResult("GlobalClusteringCoefficient", coefficient);
  }

  private static final class HarmonicCentrality<E> extends ClosenessCentrality<Integer, E> {

    private final ShortestPathAlgorithm<Integer, E> shortestPathAlgorithm;

    public HarmonicCentrality(
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
