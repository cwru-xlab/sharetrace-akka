package sharetrace.graph;

import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.GraphMetrics;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.VertexScoringAlgorithm;
import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.alg.scoring.ClusteringCoefficient;
import org.jgrapht.alg.scoring.Coreness;
import org.jgrapht.alg.scoring.EigenvectorCentrality;
import org.jgrapht.alg.scoring.KatzCentrality;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.alg.shortestpath.GraphMeasurer;
import sharetrace.util.Statistics;
import sharetrace.util.logging.metric.GraphCycles;
import sharetrace.util.logging.metric.GraphEccentricity;
import sharetrace.util.logging.metric.GraphScores;
import sharetrace.util.logging.metric.GraphSize;

@Value.Immutable
abstract class BaseGraphStatistics<V, E> {

  private static Statistics scoreStatistics(VertexScoringAlgorithm<?, Double> algorithm) {
    return Statistics.of(algorithm.getScores().values());
  }

  @Value.Lazy
  public GraphSize graphSize() {
    return GraphSize.builder()
        .vertices(graph().vertexSet().size())
        .edges(graph().edgeSet().size())
        .build();
  }

  @Value.Lazy
  public GraphCycles graphCycles() {
    return GraphCycles.builder()
        .triangles(GraphMetrics.getNumberOfTriangles(graph()))
        .girth(GraphMetrics.getGirth(graph()))
        .build();
  }

  @Value.Lazy
  public GraphEccentricity graphEccentricity() {
    GraphMeasurer<?, ?> measurer = new GraphMeasurer<>(graph(), shortestPath());
    return GraphEccentricity.builder()
        .radius((long) measurer.getRadius())
        .diameter((long) measurer.getDiameter())
        .center(measurer.getGraphCenter().size())
        .periphery(measurer.getGraphPeriphery().size())
        .build();
  }

  @Value.Lazy
  public GraphScores graphScores() {
    return GraphScores.builder()
        .degeneracy(degeneracy())
        .globalClusteringCoefficient(globalClusteringCoefficient())
        .localClusteringCoefficient(localClusteringCoefficients())
        .harmonicCentrality(harmonicCentrality())
        .katzCentrality(katzCentrality())
        .eigenvectorCentrality(eigenvectorCentrality())
        .build();
  }

  @Value.Parameter
  protected abstract Graph<V, E> graph();

  @Value.Lazy
  protected ShortestPathAlgorithm<V, E> shortestPath() {
    return new FloydWarshallShortestPaths<>(graph());
  }

  private long degeneracy() {
    return new Coreness<>(graph()).getDegeneracy();
  }

  private double globalClusteringCoefficient() {
    return new ClusteringCoefficient<>(graph()).getGlobalClusteringCoefficient();
  }

  private Statistics localClusteringCoefficients() {
    return scoreStatistics(new ClusteringCoefficient<>(graph()));
  }

  private Statistics harmonicCentrality() {
    return scoreStatistics(new HarmonicCentrality<>(graph(), shortestPath()));
  }

  private Statistics katzCentrality() {
    return scoreStatistics(new KatzCentrality<>(graph()));
  }

  private Statistics eigenvectorCentrality() {
    return scoreStatistics(new EigenvectorCentrality<>(graph()));
  }

  private static final class HarmonicCentrality<V, E> extends ClosenessCentrality<V, E> {

    private final ShortestPathAlgorithm<V, E> shortestPaths;

    public HarmonicCentrality(Graph<V, E> graph, ShortestPathAlgorithm<V, E> shortestPaths) {
      super(graph);
      this.shortestPaths = shortestPaths;
    }

    @Override
    protected ShortestPathAlgorithm<V, E> getShortestPathAlgorithm() {
      return shortestPaths;
    }
  }
}
