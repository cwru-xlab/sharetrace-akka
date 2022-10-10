package io.sharetrace.graph;

import io.sharetrace.util.Checks;
import java.time.Instant;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseContact {

  public static final Instant MIN_TIME = Instant.EPOCH;

  @Value.Check
  protected void check() {
    Checks.isAtLeast(time(), MIN_TIME, "time");
    Checks.isTrue(user1() != user2(), "Users must be distinct");
  }

  public abstract Instant time();

  public abstract int user1();

  public abstract int user2();
}
