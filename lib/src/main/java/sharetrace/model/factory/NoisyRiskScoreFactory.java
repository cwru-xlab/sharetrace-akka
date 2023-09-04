package sharetrace.model.factory;

import sharetrace.model.RiskScore;
import sharetrace.util.DistributedRandom;
import sharetrace.util.Ranges;

public record NoisyRiskScoreFactory(RiskScoreFactory scoreFactory, DistributedRandom random)
    implements RiskScoreFactory {

  @Override
  public RiskScore getRiskScore(int key) {
    return scoreFactory.getRiskScore(key).mapValue(this::distort);
  }

  private double distort(double value) {
    return Ranges.bounded(value + random.nextDouble(), RiskScore.MIN_VALUE, RiskScore.MAX_VALUE);
  }
}
