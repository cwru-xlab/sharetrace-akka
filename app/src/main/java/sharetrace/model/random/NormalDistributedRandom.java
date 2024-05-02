package sharetrace.model.random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.commons.math3.distribution.NormalDistribution;

@SuppressWarnings("unused")
@JsonTypeName("Normal")
public record NormalDistributedRandom(@JsonIgnore NormalDistribution distribution)
    implements DistributedRandom {

  @Override
  public double nextDouble() {
    return RandomSupport.nextDouble(distribution);
  }

  @JsonProperty
  public double location() {
    return distribution.getMean();
  }

  @JsonProperty
  public double scale() {
    return distribution.getStandardDeviation();
  }
}
