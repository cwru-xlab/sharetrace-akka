package sharetrace.graph;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import java.util.function.Supplier;
import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.graph.GraphDelegator;
import sharetrace.util.IdFactory;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public final class SimpleContactNetwork extends GraphDelegator<Integer, TemporalEdge>
    implements ContactNetwork {

  private final String id;
  private final String type;
  private final Map<String, ?> props;

  public SimpleContactNetwork(
      Graph<Integer, TemporalEdge> target, String type, Map<String, ?> props) {
    super(target);
    this.id = IdFactory.nextUlid();
    this.type = type;
    this.props = props;
  }

  public SimpleContactNetwork(Graph<Integer, TemporalEdge> target, String type) {
    this(target, type, Map.of());
  }

  @Override
  @JsonProperty
  public String id() {
    return id;
  }

  @JsonTypeId
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
