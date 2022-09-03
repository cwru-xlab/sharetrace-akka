package org.sharetrace.data.factory;

import java.time.Instant;
import java.util.function.Supplier;

@FunctionalInterface
public interface ContactTimeFactory {

  static ContactTimeFactory fromSupplier(Supplier<Instant> supplier) {
    return (x, xx) -> supplier.get();
  }

  Instant getContactTime(int user1, int user2);
}
