package io.sharetrace.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class Clocks {

  private static final Instant NOW = Instant.now();

  private Clocks() {}

  public static Clock nowFixed() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }

  public static MutableClock mutableNowFixed() {
    return new MutableClock(nowFixed());
  }

  public static final class MutableClock extends Clock {

    private Clock delegate;

    public MutableClock(Clock delegate) {
      this.delegate = delegate;
    }

    public void tick(Duration duration) {
      delegate = Clock.offset(delegate, duration);
    }

    @Override
    public ZoneId getZone() {
      return delegate.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return new MutableClock(delegate.withZone(zone));
    }

    @Override
    public Instant instant() {
      return delegate.instant();
    }
  }
}
