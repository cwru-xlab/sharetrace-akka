package sharetrace.graph;

import java.time.Instant;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import org.jgrapht.graph.DefaultWeightedEdge;

public final class TemporalEdge extends DefaultWeightedEdge {

  private Instant timestamp;

  public void mergeTimestamp(Instant timestamp, BinaryOperator<Instant> merger) {
    mapTimestamp(t -> t == null ? timestamp : merger.apply(t, timestamp));
  }

  public void mapTimestamp(UnaryOperator<Instant> mapper) {
    setTimestamp(mapper.apply(timestamp));
  }

  public double weight() {
    return timestamp.getEpochSecond();
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TemporalEdge edge) {
      return Objects.equals(getSource(), edge.getSource())
          && Objects.equals(getTarget(), edge.getTarget())
          && Objects.equals(getTimestamp(), edge.getTimestamp());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getSource(), getTarget(), getTimestamp());
  }
}
