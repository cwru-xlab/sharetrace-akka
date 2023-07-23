package sharetrace.graph;

import sharetrace.model.factory.TimeFactory;

public interface GeneratedTemporalNetworkFactory<V> extends TemporalNetworkFactory<V> {

  TimeFactory timeFactory();

  @Override
  default TemporalNetwork<V> getNetwork() {
    var network = TemporalNetworkFactory.super.getNetwork();
    for (var edge : network.edgeSet()) {
      edge.setTimestamp(timeFactory().getTime());
      network.setEdgeWeight(edge, edge.weight());
    }
    return network;
  }
}
