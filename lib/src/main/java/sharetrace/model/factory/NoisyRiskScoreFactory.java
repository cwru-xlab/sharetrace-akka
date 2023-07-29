package sharetrace.model.factory;

import sharetrace.model.RiskScore;
import sharetrace.util.DistributedRandom;
import sharetrace.util.Ranges;

public record NoisyRiskScoreFactory(RiskScoreFactory scoreFactory, DistributedRandom random)
    implements RiskScoreFactory {

  @Override
  public RiskScore getRiskScore(Object key) {
    return scoreFactory.getRiskScore(key).mapValue(value -> constrain(value + random.nextFloat()));
  }

  private float constrain(float value) {
    return Ranges.boundedFloat(value, RiskScore.MIN_VALUE, RiskScore.MAX_VALUE);
  }
}
