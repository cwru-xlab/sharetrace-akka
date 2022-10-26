package io.sharetrace.graph;

import com.google.common.collect.Range;
import io.sharetrace.util.Checks;
import java.time.Instant;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseContact {

  public static final Instant MIN_TIME = Instant.EPOCH;

  private static final String TIME = "time";
  private static final String USER_MSG = "Users must be distinct";
  private static final Range<Instant> TIME_RANGE = Range.atLeast(MIN_TIME);

  @Value.Check
  protected void check() {
    Checks.inRange(time(), TIME_RANGE, TIME);
    Checks.isTrue(user1() != user2(), USER_MSG);
  }

  public abstract Instant time();

  public abstract int user1();

  public abstract int user2();
}
