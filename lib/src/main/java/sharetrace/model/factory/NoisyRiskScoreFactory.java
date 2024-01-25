package sharetrace.model.factory;

import sharetrace.model.RiskScore;
import sharetrace.util.DistributedRandom;

public record NoisyRiskScoreFactory(RiskScoreFactory scoreFactory, DistributedRandom random)
    implements RiskScoreFactory {

  @Override
  public RiskScore getRiskScore(int key) {
    return scoreFactory.getRiskScore(key).mapValue(this::distort);
  }

  private double distort(double value) {
    return value + random.nextDouble(RiskScore.MIN_VALUE - value, RiskScore.MAX_VALUE - value);
  }
}
