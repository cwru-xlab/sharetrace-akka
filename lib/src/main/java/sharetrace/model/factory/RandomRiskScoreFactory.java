package sharetrace.model.factory;

import sharetrace.Buildable;
import sharetrace.model.DistributedRandom;
import sharetrace.model.RiskScore;

@Buildable
public record RandomRiskScoreFactory(
    String id, long scoreExpiry, DistributedRandom random, TimeFactory timeFactory)
    implements RiskScoreFactory {

  @Override
  public RiskScore getRiskScore(int key) {
    return RiskScore.fromExpiry(random.nextDouble(), timeFactory.getTime(), scoreExpiry);
  }

  @Override
  public String type() {
    return "Random";
  }
}
