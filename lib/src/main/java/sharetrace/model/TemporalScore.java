package sharetrace.model;

import java.util.Comparator;

public interface TemporalScore extends Expirable, Comparable<TemporalScore> {

  Comparator<TemporalScore> COMPARATOR =
      Comparator.comparingDouble(TemporalScore::value).thenComparing(Expirable::compare);

  double value();

  @Override
  @SuppressWarnings("NullableProblems")
  default int compareTo(TemporalScore score) {
    return COMPARATOR.compare(this, score);
  }
}
