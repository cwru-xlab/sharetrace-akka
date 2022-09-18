package io.sharetrace.data.sampler;

@FunctionalInterface
public interface Sampler<T> {

  T sample();
}
