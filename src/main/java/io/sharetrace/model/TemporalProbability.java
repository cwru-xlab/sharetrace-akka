package io.sharetrace.model;

import java.time.Instant;
import java.util.Comparator;
import javax.annotation.Nonnull;

public interface TemporalProbability extends Comparable<TemporalProbability> {

  Comparator<TemporalProbability> COMPARATOR =
      Comparator.comparingDouble(TemporalProbability::value)
          .thenComparing(TemporalProbability::time);

  float value();

  Instant time();

  @Override
  default int compareTo(@Nonnull TemporalProbability probability) {
    return COMPARATOR.compare(this, probability);
  }
}
