package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.model.graph.ContactNetwork;
import sharetrace.model.graph.TemporalEdge;

@JsonTypeName("Cached")
public final class CachedContactNetworkFactory implements ContactNetworkFactory {

  @JsonProperty private final ContactNetworkFactory factory;

  private ContactNetwork cached;

  public CachedContactNetworkFactory(ContactNetworkFactory factory) {
    this.factory = factory;
  }

  @Override
  public GraphGenerator<Integer, TemporalEdge, ?> graphGenerator() {
    return factory.graphGenerator();
  }

  @Override
  public ContactNetwork newContactNetwork(String id, Graph<Integer, TemporalEdge> target) {
    return factory.newContactNetwork(id, target);
  }

  @Override
  public ContactNetwork getContactNetwork() {
    return cached == null ? cached = factory.getContactNetwork() : cached;
  }
}
