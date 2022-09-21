package io.sharetrace.data.factory;

import java.time.Instant;
import java.util.function.Supplier;

@FunctionalInterface
public interface ContactTimeFactory {

  static ContactTimeFactory from(Supplier<Instant> supplier) {
    return (x, xx) -> supplier.get();
  }

  Instant contactTime(int user1, int user2);
}