package sharetrace.experiment.data;

import org.immutables.value.Value;
import sharetrace.model.RiskScore;
import sharetrace.util.DistributedRandom;
import sharetrace.util.Ranges;

@Value.Immutable
abstract class BaseNoisyRiskScoreFactory<K> implements RiskScoreFactory<K> {

  private static float constrain(double value) {
    return Ranges.boundedFloat(value, RiskScore.MIN_VALUE, RiskScore.MAX_VALUE);
  }

  @Override
  public RiskScore getScore(K key) {
    RiskScore score = scoreFactory().getScore(key);
    return score.mapValue(value -> constrain(value + random().nextDouble()));
  }

  @Value.Parameter
  protected abstract RiskScoreFactory<K> scoreFactory();

  @Value.Parameter
  protected abstract DistributedRandom random();
}
