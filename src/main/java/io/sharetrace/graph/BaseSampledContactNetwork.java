package io.sharetrace.graph;

import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseSampledContactNetwork extends AbstractContactNetwork {

  @Override
  @Value.Lazy
  public Set<Contact> contacts() {
    return super.contacts();
  }
}
