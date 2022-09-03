package org.sharetrace.data.sampling;

@FunctionalInterface
public interface Sampler<T> {

  T sample();
}
