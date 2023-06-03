package io.sharetrace.graph;

@FunctionalInterface
public interface TemporalNetworkFactory<V> {

  TemporalNetwork<V> getNetwork();
}
