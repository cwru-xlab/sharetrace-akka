package sharetrace.graph;

import sharetrace.model.factory.TimeFactory;

public interface GeneratedContactNetworkFactory extends ContactNetworkFactory {

  TimeFactory timeFactory();

  @Override
  default ContactNetwork getContactNetwork() {
    var network = ContactNetworkFactory.super.getContactNetwork();
    for (var edge : network.edgeSet()) {
      edge.setTime(timeFactory().getTime());
    }
    return network;
  }
}
