package io.sharetrace.experiment.data;

import io.sharetrace.model.RiskScore;
import io.sharetrace.util.DistributedRandom;
import org.immutables.value.Value;

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
