package sharetrace.model.random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.math3.distribution.BetaDistribution;
import sharetrace.model.DistributedRandom;

@SuppressWarnings("unused")
public record BetaDistributedRandom(@JsonIgnore BetaDistribution distribution)
    implements DistributedRandom {

  @Override
  public double nextDouble() {
    return RandomSupport.nextDouble(distribution);
  }

  @Override
  public String type() {
    return "Beta";
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
