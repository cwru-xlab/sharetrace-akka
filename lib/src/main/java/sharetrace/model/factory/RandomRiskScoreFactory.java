package sharetrace.model.factory;

import java.time.Duration;
import sharetrace.Buildable;
import sharetrace.model.RiskScore;
import sharetrace.util.DistributedRandom;

@Buildable
public record RandomRiskScoreFactory<K>(
    Duration scoreExpiry, DistributedRandom random, TimeFactory timeFactory)
    implements RiskScoreFactory<K> {

  @Override
  public RiskScore getScore(K key) {
    return new RiskScore(random.nextFloat(), timeFactory.getTime(), scoreExpiry);
  }
}
