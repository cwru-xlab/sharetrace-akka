package io.sharetrace.experiment.data;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import io.sharetrace.util.Identifiers;
import org.immutables.value.Value;

@JsonIgnoreType
abstract class AbstractDataset implements Dataset {

  @Value.Lazy
  public String id() {
    return Identifiers.ofIntString();
  }
}
