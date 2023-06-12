package sharetrace.graph;

import java.time.Instant;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import org.jgrapht.graph.DefaultEdge;

public final class TemporalEdge extends DefaultEdge {

  private Instant timestamp;

  public void mergeTimestamp(Instant timestamp, BinaryOperator<Instant> merger) {
    mapTimestamp(t -> t == null ? timestamp : merger.apply(t, timestamp));
  }

  public void mapTimestamp(UnaryOperator<Instant> mapper) {
    setTimestamp(mapper.apply(timestamp));
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }
}
