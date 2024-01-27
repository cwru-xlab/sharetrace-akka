package sharetrace.model.factory;

import sharetrace.model.DistributedRandom;
import sharetrace.model.RiskScore;

@FunctionalInterface
public interface RiskScoreFactory {

  RiskScore getRiskScore(int key);

  default RiskScoreFactory cached() {
    return this instanceof CachedRiskScoreFactory ? this : new CachedRiskScoreFactory(this);
  }

  default RiskScoreFactory withNoise(DistributedRandom noise) {
    return new NoisyRiskScoreFactory(this, noise);
  }
}
