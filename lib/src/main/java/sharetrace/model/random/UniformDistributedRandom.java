package sharetrace.model.random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.commons.math3.distribution.UniformRealDistribution;

@SuppressWarnings("unused")
@JsonTypeName("Uniform")
public record UniformDistributedRandom(@JsonIgnore UniformRealDistribution distribution)
    implements DistributedRandom {

  @Override
  public double nextDouble() {
    return RandomSupport.nextDouble(distribution);
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
