package sharetrace.model.factory;

import sharetrace.model.RiskScore;
import sharetrace.util.DistributedRandom;
import sharetrace.util.Ranges;

public record NoisyRiskScoreFactory<K>(RiskScoreFactory<K> scoreFactory, DistributedRandom random)
    implements RiskScoreFactory<K> {

  @Override
  public RiskScore getScore(K key) {
    return scoreFactory.getScore(key).mapValue(value -> constrain(value + random.nextFloat()));
  }

  private float constrain(float value) {
    return Ranges.boundedFloat(value, RiskScore.MIN_VALUE, RiskScore.MAX_VALUE);
  }
}
