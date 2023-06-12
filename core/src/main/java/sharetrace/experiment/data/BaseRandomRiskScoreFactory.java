package sharetrace.experiment.data;

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
        .build();
  }

  protected abstract DistributedRandom random();

  protected abstract TimestampFactory timestampFactory();
}
