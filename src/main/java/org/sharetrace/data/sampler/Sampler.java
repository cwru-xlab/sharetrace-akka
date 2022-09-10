package org.sharetrace.data.sampler;

@FunctionalInterface
public interface Sampler<T> {

  T sample();
}
