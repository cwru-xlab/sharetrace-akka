package sharetrace.model.random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.commons.math3.distribution.BetaDistribution;

@SuppressWarnings("unused")
@JsonTypeName("Beta")
public record BetaDistributedRandom(@JsonIgnore BetaDistribution distribution) implements DistributedRandom {

  @Override
  public double nextDouble() {
    return RandomSupport.nextDouble(distribution);
  }

  @JsonProperty
  public double alpha() {
    return distribution.getAlpha();
  }

  @JsonProperty
  public double beta() {
    return distribution.getBeta();
  }
}
