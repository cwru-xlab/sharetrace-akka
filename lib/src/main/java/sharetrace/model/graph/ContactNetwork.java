package sharetrace.model.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jgrapht.Graph;

public interface ContactNetwork extends Graph<Integer, TemporalEdge> {

  @JsonProperty
  String id();
}
