package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonValue;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.model.graph.ContactNetwork;
import sharetrace.model.graph.TemporalEdge;

public final class CachedContactNetworkFactory implements ContactNetworkFactory {

  @JsonValue private final ContactNetworkFactory factory;

  private ContactNetwork cached;

  public CachedContactNetworkFactory(ContactNetworkFactory factory) {
    this.factory = factory;
  }

  @Override
  public String type() {
    return factory.type();
  }

  @Override
  public GraphGenerator<Integer, TemporalEdge, ?> graphGenerator() {
    return factory.graphGenerator();
  }

  @Override
  public ContactNetwork newContactNetwork(Graph<Integer, TemporalEdge> target) {
    return factory.newContactNetwork(target);
  }

  @Override
  public ContactNetwork getContactNetwork() {
    return cached == null ? cached = factory.getContactNetwork() : cached;
  }
}
