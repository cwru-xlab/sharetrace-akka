package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Comparator;

public interface TemporalScore extends Expirable, Comparable<TemporalScore> {

  Comparator<TemporalScore> COMPARATOR =
      Comparator.comparingDouble(TemporalScore::value).thenComparing(Expirable.COMPARATOR);

  @JsonProperty("v")
  double value();

  @Override
  @SuppressWarnings("NullableProblems")
  default int compareTo(TemporalScore score) {
    return COMPARATOR.compare(this, score);
  }
}
