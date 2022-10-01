package io.sharetrace.data.factory;

import io.sharetrace.model.RiskScore;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseNoisyRiskScoreFactory implements RiskScoreFactory {

  @Override
  public RiskScore riskScore(int user) {
    RiskScore score = scoreFactory().riskScore(user);
    float noisy = constrain(score.value() + (float) noise().sample());
    return score.withValue(noisy);
  }

  @Value.Parameter
  protected abstract RiskScoreFactory scoreFactory();

  private static float constrain(float value) {
    return Math.max(RiskScore.MIN_VALUE, Math.min(RiskScore.MAX_VALUE, value));
  }

  @Value.Parameter
  protected abstract RealDistribution noise();
}
