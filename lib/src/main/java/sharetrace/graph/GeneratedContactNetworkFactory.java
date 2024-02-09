package sharetrace.graph;

import sharetrace.model.factory.TimeFactory;

public interface GeneratedContactNetworkFactory extends ContactNetworkFactory {

  TimeFactory timeFactory();

  @Override
  default ContactNetwork getContactNetwork() {
    var network = ContactNetworkFactory.super.getContactNetwork();
    for (var edge : network.edgeSet()) {
      int source = network.getEdgeSource(edge);
      int target = network.getEdgeTarget(edge);
      Graphs.addTemporalEdge(network, source, target, timeFactory().getTime());
    }
    return network;
  }
}
