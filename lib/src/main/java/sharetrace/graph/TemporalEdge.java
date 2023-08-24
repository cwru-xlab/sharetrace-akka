package sharetrace.graph;

import java.time.Instant;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import org.jgrapht.graph.DefaultWeightedEdge;

public final class TemporalEdge extends DefaultWeightedEdge {

  private Instant time;

  public void updateTime(Instant time, BinaryOperator<Instant> merger) {
    updateTime(t -> t == null ? time : merger.apply(t, time));
  }

  public void updateTime(UnaryOperator<Instant> mapper) {
    setTime(mapper.apply(time));
  }

  public double weight() {
    return time.getEpochSecond();
  }

  public Instant getTime() {
    return time;
  }

  public void setTime(Instant time) {
    this.time = time;
  }
}
