package sharetrace.model.random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.math3.distribution.UniformRealDistribution;

@SuppressWarnings("unused")
public record UniformDistributedRandom(@JsonIgnore UniformRealDistribution distribution)
    implements DistributedRandom {

  @Override
  public double nextDouble() {
    return RandomSupport.nextDouble(distribution);
  }

  @Override
  public String type() {
    return "Uniform";
  }

  @JsonProperty
  public double lowerBound() {
    return distribution.getSupportLowerBound();
  }

  @JsonProperty
  public double upperBound() {
    return distribution.getSupportUpperBound();
  }
}
