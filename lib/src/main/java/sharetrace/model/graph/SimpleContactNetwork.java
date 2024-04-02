package sharetrace.model.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jgrapht.Graph;
import org.jgrapht.graph.GraphDelegator;

@SuppressWarnings("unused")
@JsonIgnoreProperties({"type", "vertexSupplier", "edgeSupplier"})
public final class SimpleContactNetwork extends GraphDelegator<Integer, TemporalEdge>
    implements ContactNetwork {

  private final String id;

  public SimpleContactNetwork(String id, Graph<Integer, TemporalEdge> target) {
    super(target);
    this.id = id;
  }

  @Override
  public String id() {
    return id;
  }

  @JsonProperty
  public int nodes() {
    return vertexSet().size();
  }

  @JsonProperty
  public int edges() {
    return edgeSet().size();
  }
}
