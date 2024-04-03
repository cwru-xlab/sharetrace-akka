package sharetrace.model.factory;

import sharetrace.Buildable;
import sharetrace.model.RiskScore;
import sharetrace.model.random.DistributedRandom;

@Buildable
public record RandomRiskScoreFactory(
    String id, long scoreExpiry, DistributedRandom distribution, TimeFactory timeFactory)
    implements RiskScoreFactory {

  @Override
  public RiskScore getRiskScore(int key) {
    return RiskScore.fromExpiry(distribution.nextDouble(), timeFactory.getTime(), scoreExpiry);
  }

  @Override
  public String type() {
    return "Random";
  }
}
