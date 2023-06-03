package io.sharetrace.experiment;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.sharetrace.model.TimestampReference;
import io.sharetrace.util.logging.ToClassNameSerializer;
import java.time.Clock;
import org.apache.commons.math3.random.RandomGenerator;
import org.immutables.value.Value;

@Value.Immutable
interface BaseContext extends TimestampReference {

  @JsonSerialize(using = ToStringSerializer.class)
  Clock clock();

  long seed();

  @JsonSerialize(using = ToClassNameSerializer.class)
  RandomGenerator random();
}
