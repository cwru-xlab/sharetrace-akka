package sharetrace.graph;

import sharetrace.model.factory.TimeFactory;

public interface GeneratedContactNetworkFactory<V> extends ContactNetworkFactory<V> {

  TimeFactory timeFactory();

  @Override
  default ContactNetwork<V> getContactNetwork() {
    var network = ContactNetworkFactory.super.getContactNetwork();
    for (var edge : network.edgeSet()) {
      edge.setTimestamp(timeFactory().getTime());
      network.setEdgeWeight(edge, edge.weight());
    }
    return network;
  }
}
