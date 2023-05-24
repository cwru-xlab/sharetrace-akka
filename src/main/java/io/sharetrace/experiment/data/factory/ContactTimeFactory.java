package io.sharetrace.experiment.data.factory;

import java.time.Instant;
import java.util.function.Supplier;

@FunctionalInterface
public interface ContactTimeFactory {

  static ContactTimeFactory from(Supplier<Instant> supplier) {
    return (x, xx) -> supplier.get();
  }

  Instant get(int self, int contact);
}
