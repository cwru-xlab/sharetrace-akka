package sharetrace.analysis.handler;

import org.jgrapht.Graph;
import org.jgrapht.GraphMetrics;
import org.jgrapht.alg.scoring.ClusteringCoefficient;
import org.jgrapht.alg.scoring.Coreness;
import org.jgrapht.alg.scoring.EigenvectorCentrality;
import org.jgrapht.alg.scoring.HarmonicCentrality;
import org.jgrapht.alg.scoring.KatzCentrality;
import org.jgrapht.alg.shortestpath.GraphMeasurer;
import org.jgrapht.alg.shortestpath.IntVertexDijkstraShortestPath;
import sharetrace.analysis.model.Context;
import sharetrace.analysis.model.EventRecord;
import sharetrace.analysis.model.Results;
import sharetrace.logging.event.user.ContactEvent;
import sharetrace.model.graph.Graphs;
import sharetrace.model.graph.TemporalEdge;

public final class GraphHandler implements EventHandler {

  private final Graph<Integer, TemporalEdge> graph;

  public GraphHandler() {
    this.graph = Graphs.newTemporalGraph();
  }

  @Override
  public void onNext(EventRecord record, Context context) {
    if (record.event() instanceof ContactEvent e) {
      Graphs.addTemporalEdge(graph, e.self(), e.contact(), e.contactTime());
    }
  }

  @Override
  public void onComplete(Results results, Context context) {
    var measurer = new GraphMeasurer<>(graph, new IntVertexDijkstraShortestPath<>(graph));
    results = results.withScope("graph");
    results
        .put("girth", GraphMetrics.getGirth(graph))
        .put("triangles", GraphMetrics.getNumberOfTriangles(graph))
        .put("radius", measurer.getRadius())
        .put("diameter", measurer.getDiameter())
        .put("center", measurer.getGraphCenter().size())
        .put("periphery", measurer.getGraphPeriphery().size())
        .put("degeneracy", new Coreness<>(graph).getDegeneracy());
    results
        .withScope("clustering")
        .put("global", new ClusteringCoefficient<>(graph).getGlobalClusteringCoefficient())
        .put("local", new ClusteringCoefficient<>(graph).getScores());
    results
        .withScope("centrality")
        .put("harmonic", new HarmonicCentrality<>(graph).getScores())
        .put("katz", new KatzCentrality<>(graph).getScores())
        .put("eigenvector", new EigenvectorCentrality<>(graph).getScores());
  }
}
