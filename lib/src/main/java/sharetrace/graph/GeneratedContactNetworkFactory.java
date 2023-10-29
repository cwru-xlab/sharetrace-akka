package sharetrace.graph;

import sharetrace.model.factory.TimeFactory;

public interface GeneratedContactNetworkFactory extends ContactNetworkFactory {

  TimeFactory timeFactory();

  @Override
  default ContactNetwork getContactNetwork() {
    var network = ContactNetworkFactory.super.getContactNetwork();
    network.edgeSet().forEach(e -> e.setTime(timeFactory().getTime()));
    return network;
  }
}
