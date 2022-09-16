package org.sharetrace.data.factory;

import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;
import org.sharetrace.model.RiskScore;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseNoisyRiskScoreFactory implements RiskScoreFactory {

  private static float constrain(double value) {
    return (float) Math.max(RiskScore.MIN_VALUE, Math.min(RiskScore.MAX_VALUE, value));
  }

  @Override
  public RiskScore riskScore(int user) {
    RiskScore score = scoreFactory().riskScore(user);
    return score.withValue(constrain(score.value() + noise().sample()));
  }

  @Value.Parameter
  protected abstract RealDistribution noise();

  @Value.Parameter
  protected abstract RiskScoreFactory scoreFactory();
}
