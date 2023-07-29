package sharetrace.model.factory;

import java.time.Duration;
import sharetrace.Buildable;
import sharetrace.model.RiskScore;
import sharetrace.util.DistributedRandom;

@Buildable
public record RandomRiskScoreFactory(
    Duration scoreExpiry, DistributedRandom random, TimeFactory timeFactory)
    implements RiskScoreFactory {

  @Override
  public RiskScore getRiskScore(Object key) {
    return new RiskScore(random.nextFloat(), timeFactory.getTime(), scoreExpiry);
  }
}
