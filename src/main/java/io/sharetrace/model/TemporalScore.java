package io.sharetrace.model;

import java.time.Instant;
import java.util.Comparator;
import javax.annotation.Nonnull;

public interface TemporalScore extends Comparable<TemporalScore> {

  Comparator<TemporalScore> COMPARATOR =
      Comparator.comparingDouble(TemporalScore::value).thenComparing(TemporalScore::timestamp);

  float value();

  Instant timestamp();

  @Override
  default int compareTo(@Nonnull TemporalScore score) {
    return COMPARATOR.compare(this, score);
  }
}
