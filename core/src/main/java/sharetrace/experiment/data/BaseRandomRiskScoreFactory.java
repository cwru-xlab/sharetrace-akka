package sharetrace.experiment.data;

import java.time.Duration;
import org.immutables.value.Value;
import sharetrace.model.RiskScore;
import sharetrace.util.DistributedRandom;

@Value.Immutable
abstract class BaseRandomRiskScoreFactory<K> implements RiskScoreFactory<K> {

  @Override
  public RiskScore getScore(K key) {
    return RiskScore.builder()
        .value(random().nextFloat(RiskScore.RANGE))
        .timestamp(timestampFactory().getTimestamp())
        .expiry(scoreExpiry())
        .build();
  }

  protected abstract Duration scoreExpiry();

  protected abstract DistributedRandom random();

  protected abstract TimestampFactory timestampFactory();
}
