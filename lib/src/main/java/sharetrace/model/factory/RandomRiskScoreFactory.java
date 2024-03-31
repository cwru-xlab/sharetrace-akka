package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonTypeName;
import sharetrace.Buildable;
import sharetrace.model.RiskScore;
import sharetrace.model.random.DistributedRandom;

@JsonTypeName("Random")
@Buildable
public record RandomRiskScoreFactory(
    long scoreExpiry, DistributedRandom distribution, TimeFactory timeFactory)
    implements RiskScoreFactory {

  @Override
  public RiskScore getRiskScore(int key) {
    return RiskScore.fromExpiry(distribution.nextDouble(), timeFactory.getTime(), scoreExpiry);
  }
}
