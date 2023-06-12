package sharetrace.model;

import java.time.Instant;
import java.util.Comparator;

public interface TemporalScore extends Comparable<TemporalScore> {

  Comparator<TemporalScore> COMPARATOR =
      Comparator.comparingDouble(TemporalScore::value).thenComparing(TemporalScore::timestamp);

  float value();

  Instant timestamp();

  @Override
  @SuppressWarnings("NullableProblems")
  default int compareTo(TemporalScore score) {
    return COMPARATOR.compare(this, score);
  }
}
