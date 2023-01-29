package io.sharetrace.graph;

import com.google.common.collect.Range;
import io.sharetrace.util.Checks;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
abstract class BaseContact {

    public static final Instant MIN_TIME = Instant.EPOCH;

    private static final Range<Instant> TIME_RANGE = Range.atLeast(MIN_TIME);

    @Value.Check
    protected void check() {
        Checks.inRange(time(), TIME_RANGE, "time");
        Checks.isTrue(user1() != user2(), "Users must be distinct; got %s == %s", user1(), user2());
    }

    public abstract Instant time();

    public abstract int user1();

    public abstract int user2();
}
