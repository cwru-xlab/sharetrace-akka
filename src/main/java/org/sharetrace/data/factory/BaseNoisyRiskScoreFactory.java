package org.sharetrace.data.factory;

import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;
import org.sharetrace.model.RiskScore;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseNoisyRiskScoreFactory implements RiskScoreFactory {

  private static float constrain(float value) {
    return Math.max(RiskScore.MIN_VALUE, Math.min(RiskScore.MAX_VALUE, value));
  }

  @Override
  public RiskScore riskScore(int user) {
    RiskScore score = scoreFactory().riskScore(user);
    float noisy = constrain(score.value() + (float) noise().sample());
    return score.withValue(noisy);
  }

  @Value.Parameter
  protected abstract RealDistribution noise();

  @Value.Parameter
  protected abstract RiskScoreFactory scoreFactory();
}
