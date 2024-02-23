package sharetrace.graph;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;
import org.jgrapht.Graph;
import org.jgrapht.graph.GraphDelegator;

public final class SimpleContactNetwork extends GraphDelegator<Integer, TemporalEdge>
    implements ContactNetwork {

  private final Map<String, Object> properties;

  public SimpleContactNetwork(
      Graph<Integer, TemporalEdge> target, String type, Map<String, ?> properties) {
    super(target);
    this.properties = addDefaultProperties(target, type, properties);
  }

  private Map<String, Object> addDefaultProperties(
      Graph<?, ?> target, String type, Map<String, ?> properties) {
    var props = new HashMap<String, Object>(properties);
    props.put("type", type);
    props.put("nodes", target.vertexSet().size());
    props.put("edges", target.edgeSet().size());
    return Map.copyOf(props);
  }

  public SimpleContactNetwork(Graph<Integer, TemporalEdge> target, String type) {
    this(target, type, Map.of());
  }

  @JsonValue
  @SuppressWarnings("unused")
  private Map<String, Object> properties() {
    return properties;
  }
}
