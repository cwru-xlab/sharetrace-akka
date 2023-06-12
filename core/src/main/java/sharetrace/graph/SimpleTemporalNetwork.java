package sharetrace.graph;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Map;
import java.util.function.Supplier;
import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.graph.GraphDelegator;

@JsonSerialize
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public final class SimpleTemporalNetwork<V> extends GraphDelegator<V, TemporalEdge>
    implements TemporalNetwork<V> {

  private final String id;
  private final String type;
  private final Map<String, ?> properties;

  public SimpleTemporalNetwork(
      Graph<V, TemporalEdge> delegate, String id, String type, Map<String, ?> properties) {
    super(delegate);
    this.id = id;
    this.type = type;
    this.properties = properties;
  }

  public SimpleTemporalNetwork(Graph<V, TemporalEdge> delegate, String id, String type) {
    this(delegate, id, type, Map.of());
  }

  @Override
  @JsonProperty
  public String id() {
    return id;
  }

  @Override
  @JsonTypeId
  @JsonProperty
  public String type() {
    return type;
  }

  @JsonAnyGetter
  public Map<String, ?> properties() {
    return properties;
  }

  @Override
  @JsonIgnore
  public GraphType getType() {
    return super.getType();
  }

  @Override
  @JsonIgnore
  public Supplier<V> getVertexSupplier() {
    return super.getVertexSupplier();
  }

  @Override
  @JsonIgnore
  public Supplier<TemporalEdge> getEdgeSupplier() {
    return super.getEdgeSupplier();
  }
}
