package sharetrace.model.factory;

import sharetrace.Buildable;
import sharetrace.model.RiskScore;
import sharetrace.util.DistributedRandom;

@Buildable
public record RandomRiskScoreFactory(
    long scoreExpiry, DistributedRandom random, TimeFactory timeFactory)
    implements RiskScoreFactory {

  @Override
  public RiskScore getRiskScore(int key) {
    return RiskScore.fromExpiry(random.nextDouble(), timeFactory.getTime(), scoreExpiry);
  }
}
