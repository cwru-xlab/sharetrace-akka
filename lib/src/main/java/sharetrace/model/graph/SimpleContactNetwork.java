package sharetrace.model.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jgrapht.Graph;
import org.jgrapht.graph.GraphDelegator;

@SuppressWarnings("unused")
@JsonIgnoreProperties({"type", "vertexSupplier", "edgeSupplier"})
public final class SimpleContactNetwork extends GraphDelegator<Integer, TemporalEdge>
    implements ContactNetwork {

  public SimpleContactNetwork(Graph<Integer, TemporalEdge> target) {
    super(target);
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
