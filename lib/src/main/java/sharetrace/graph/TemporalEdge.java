package sharetrace.graph;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import org.jgrapht.graph.DefaultWeightedEdge;
import sharetrace.model.Timestamp;

public final class TemporalEdge extends DefaultWeightedEdge {

  private Timestamp time;

  public void updateTime(Timestamp time, BinaryOperator<Timestamp> merger) {
    updateTime(t -> t == null ? time : merger.apply(t, time));
  }

  public void updateTime(UnaryOperator<Timestamp> mapper) {
    setTime(mapper.apply(time));
  }

  public double weight() {
    return time.toEpochMillis();
  }

  public Timestamp getTime() {
    return time;
  }

  public void setTime(Timestamp time) {
    this.time = time;
  }
}
