package sharetrace.graph;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import org.jgrapht.Graph;
import org.jgrapht.graph.GraphDelegator;

@JsonIgnoreProperties({"type", "vertexSupplier", "edgeSupplier"})
public final class SimpleContactNetwork extends GraphDelegator<Integer, TemporalEdge>
    implements ContactNetwork {

  private final Map<String, ?> props;

  public SimpleContactNetwork(
      Graph<Integer, TemporalEdge> target, String type, Map<String, ?> props) {
    super(target);
    this.props = addDefaultProps(target, type, props);
  }

  private Map<String, ?> addDefaultProps(Graph<?, ?> target, String type, Map<String, ?> props) {
    var newProps = new HashMap<String, Object>(props);
    newProps.put("type", type);
    newProps.put("nodes", target.vertexSet().size());
    newProps.put("edges", target.edgeSet().size());
    return Map.copyOf(newProps);
  }

  public SimpleContactNetwork(Graph<Integer, TemporalEdge> target, String type) {
    this(target, type, Map.of());
  }

  @JsonAnyGetter
  public Map<String, ?> props() {
    return props;
  }
}
