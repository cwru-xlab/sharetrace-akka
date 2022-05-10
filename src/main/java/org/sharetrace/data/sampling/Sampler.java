package org.sharetrace.data.sampling;

import java.util.stream.Stream;

@FunctionalInterface
public interface Sampler<T> {

  default Stream<T> stream() {
    return Stream.generate(this::sample);
  }

  T sample();
}
