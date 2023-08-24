package sharetrace.graph;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.graph.GraphDelegator;

public final class SimpleContactNetwork extends GraphDelegator<Integer, TemporalEdge>
    implements ContactNetwork {

  private final String type;
  private final Map<String, ?> props;

  public SimpleContactNetwork(
      Graph<Integer, TemporalEdge> target, String type, Map<String, ?> props) {
    super(target);
    this.type = type;
    this.props = withGraphSize(target, props);
  }

  private Map<String, ?> withGraphSize(Graph<?, ?> target, Map<String, ?> props) {
    var newProps = new HashMap<String, Object>(props);
    newProps.put("nodes", target.vertexSet().size());
    newProps.put("edges", target.edgeSet().size());
    return Map.copyOf(newProps);
  }

  public SimpleContactNetwork(Graph<Integer, TemporalEdge> target, String type) {
    this(target, type, Map.of());
  }

  @JsonProperty
  public String type() {
    return type;
  }

  @JsonAnyGetter
  public Map<String, ?> props() {
    return props;
  }

  @Override
  @JsonIgnore
  public GraphType getType() {
    return super.getType();
  }

  @Override
  @JsonIgnore
  public Supplier<Integer> getVertexSupplier() {
    return super.getVertexSupplier();
  }

  @Override
  @JsonIgnore
  public Supplier<TemporalEdge> getEdgeSupplier() {
    return super.getEdgeSupplier();
  }
}
