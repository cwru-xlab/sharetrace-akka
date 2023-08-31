package sharetrace.model;

import java.util.Comparator;

public interface TemporalScore extends Expirable, Timestamped, Comparable<TemporalScore> {

  Comparator<TemporalScore> COMPARATOR =
      Comparator.comparingDouble(TemporalScore::value)
          .thenComparing(TemporalScore::timestamp)
          .thenComparing(TemporalScore::expiresAt);

  double value();

  @Override
  @SuppressWarnings("NullableProblems")
  default int compareTo(TemporalScore score) {
    return COMPARATOR.compare(this, score);
  }
}
