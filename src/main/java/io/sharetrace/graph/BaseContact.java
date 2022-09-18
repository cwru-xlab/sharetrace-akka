package io.sharetrace.graph;

import java.time.Instant;
import org.immutables.value.Value;

@Value.Immutable
interface BaseContact {

  int user1();

  int user2();

  Instant time();
}
