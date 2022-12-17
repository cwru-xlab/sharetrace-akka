package io.sharetrace.experiment.data.sampler;

@FunctionalInterface
public interface Sampler<T> {

    T sample();
}
