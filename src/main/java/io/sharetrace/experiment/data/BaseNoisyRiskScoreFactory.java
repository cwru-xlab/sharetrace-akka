package io.sharetrace.experiment.data;

import io.sharetrace.model.RiskScore;
import io.sharetrace.util.DistributedRandom;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseNoisyRiskScoreFactory<K> implements RiskScoreFactory<K> {

  private static float constrain(double value) {
    return (float) Math.max(RiskScore.MIN_VALUE, Math.min(RiskScore.MAX_VALUE, value));
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
