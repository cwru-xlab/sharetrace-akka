package sharetrace.graph;

@FunctionalInterface
public interface TemporalNetworkFactory<V> {

  TemporalNetwork<V> getNetwork();
}
