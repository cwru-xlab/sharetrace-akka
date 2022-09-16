package org.sharetrace.data.factory;

import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;
import org.sharetrace.model.RiskScore;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseNoisyRiskScoreFactory implements RiskScoreFactory {

  @Override
  public RiskScore riskScore(int user) {
    RiskScore score = scoreFactory().riskScore(user);
    return score.withValue((float) (score.value() + noise().sample()));
  }

  @Value.Parameter
  protected abstract RealDistribution noise();

  @Value.Parameter
  protected abstract RiskScoreFactory scoreFactory();
}
