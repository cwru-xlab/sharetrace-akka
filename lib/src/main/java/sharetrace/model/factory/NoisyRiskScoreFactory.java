package sharetrace.model.factory;

import sharetrace.model.RiskScore;
import sharetrace.util.DistributedRandom;
import sharetrace.util.Doubles;

public record NoisyRiskScoreFactory(RiskScoreFactory scoreFactory, DistributedRandom random)
    implements RiskScoreFactory {

  @Override
  public RiskScore getRiskScore(int key) {
    return scoreFactory.getRiskScore(key).mapValue(this::distort);
  }

  private double distort(double v) {
    return Doubles.bounded(v + random.nextDouble(), RiskScore.MIN_VALUE, RiskScore.MAX_VALUE);
  }
}
