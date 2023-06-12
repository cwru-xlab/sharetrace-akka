package sharetrace.graph;

import java.time.Instant;
import org.jgrapht.graph.DefaultEdge;

public final class TemporalEdge extends DefaultEdge {

  private Instant timestamp;

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }
}
