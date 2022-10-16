package io.sharetrace.data;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import io.sharetrace.util.Uid;
import org.immutables.value.Value;

@JsonIgnoreType
abstract class AbstractDataset implements Dataset {

  @Value.Derived
  public String id() {
    return Uid.ofIntString();
  }
}
