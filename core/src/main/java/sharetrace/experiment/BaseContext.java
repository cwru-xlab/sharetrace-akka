package sharetrace.experiment;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.Clock;
import org.apache.commons.math3.random.RandomGenerator;
import org.immutables.value.Value;
import sharetrace.model.TimestampReference;
import sharetrace.util.logging.ToClassNameSerializer;

@Value.Immutable
interface BaseContext extends TimestampReference {

  @JsonSerialize(using = ToStringSerializer.class)
  Clock clock();

  long seed();

  @JsonSerialize(using = ToClassNameSerializer.class)
  RandomGenerator random();
}
