package sharetrace.model.factory;

import sharetrace.model.RiskScore;
import sharetrace.util.DistributedRandom;

@FunctionalInterface
public interface RiskScoreFactory {

  RiskScore getRiskScore(Object key);

  default RiskScoreFactory cached() {
    return this instanceof CachedRiskScoreFactory ? this : new CachedRiskScoreFactory(this);
  }

  default RiskScoreFactory withNoise(DistributedRandom noise) {
    return new NoisyRiskScoreFactory(this, noise);
  }
}
