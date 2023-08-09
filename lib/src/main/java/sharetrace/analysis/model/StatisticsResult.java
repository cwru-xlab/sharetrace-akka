package sharetrace.analysis.model;

import org.jgrapht.alg.interfaces.VertexScoringAlgorithm;
import sharetrace.util.Statistics;

public record StatisticsResult(String key, Statistics value) implements Result<Statistics> {

  public static StatisticsResult from(VertexScoringAlgorithm<?, Double> algorithm) {
    var key = algorithm.getClass().getSimpleName();
    var value = Statistics.of(algorithm.getScores().values());
    return new StatisticsResult(key, value);
  }
}
