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

  private float distort(float v) {
    return Ranges.boundedFloat(v + random.nextFloat(), RiskScore.MIN_VALUE, RiskScore.MAX_VALUE);
  }
}
