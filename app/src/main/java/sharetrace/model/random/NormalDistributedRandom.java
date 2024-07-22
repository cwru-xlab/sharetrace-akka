package sharetrace.model.random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.math3.distribution.NormalDistribution;
import sharetrace.model.DistributedRandom;

@SuppressWarnings("unused")
public record NormalDistributedRandom(@JsonIgnore NormalDistribution distribution)
    implements DistributedRandom {

  @Override
  public double nextDouble() {
    return RandomSupport.nextDouble(distribution);
  }

  @Override
  public String type() {
    return "Normal";
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
