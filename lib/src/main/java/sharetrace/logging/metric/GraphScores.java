package sharetrace.logging.metric;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.VertexScoringAlgorithm;
import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.alg.scoring.ClusteringCoefficient;
import org.jgrapht.alg.scoring.Coreness;
import org.jgrapht.alg.scoring.EigenvectorCentrality;
import org.jgrapht.alg.scoring.KatzCentrality;
import sharetrace.Buildable;
import sharetrace.util.Statistics;

@Buildable
public record GraphScores(
    Statistics closenessCentrality,
    Statistics katzCentrality,
    Statistics eigenvectorCentrality,
    double degeneracy,
    double globalClusteringCoefficient,
    Statistics localClusteringCoefficient)
    implements Metric {

  public static GraphScores of(Graph<?, ?> graph) {
    return GraphScoresBuilder.create()
        .degeneracy(degeneracy(graph))
        .globalClusteringCoefficient(globalClusteringCoefficient(graph))
        .localClusteringCoefficient(localClusteringCoefficients(graph))
        .closenessCentrality(closenessCentrality(graph))
        .katzCentrality(katzCentrality(graph))
        .eigenvectorCentrality(eigenvectorCentrality(graph))
        .build();
  }

  private static long degeneracy(Graph<?, ?> graph) {
    return new Coreness<>(graph).getDegeneracy();
  }

  private static double globalClusteringCoefficient(Graph<?, ?> graph) {
    return new ClusteringCoefficient<>(graph).getGlobalClusteringCoefficient();
  }

  private static Statistics localClusteringCoefficients(Graph<?, ?> graph) {
    return scoreStatistics(new ClusteringCoefficient<>(graph));
  }

  private static Statistics closenessCentrality(Graph<?, ?> graph) {
    return scoreStatistics(new ClosenessCentrality<>(graph));
  }

  private static Statistics katzCentrality(Graph<?, ?> graph) {
    return scoreStatistics(new KatzCentrality<>(graph));
  }

  private static Statistics eigenvectorCentrality(Graph<?, ?> graph) {
    return scoreStatistics(new EigenvectorCentrality<>(graph));
  }

  private static Statistics scoreStatistics(VertexScoringAlgorithm<?, Double> algorithm) {
    return Statistics.of(algorithm.getScores().values());
  }
}
