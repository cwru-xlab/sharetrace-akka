package sharetrace.model.graph;

import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;
import org.jgrapht.graph.DefaultWeightedEdge;

public final class TemporalEdge extends DefaultWeightedEdge {

  private static final long UNSET = -1;

  private long time = UNSET;

  public void updateTime(long time, LongBinaryOperator merger) {
    updateTime(t -> t == UNSET ? time : merger.applyAsLong(t, time));
  }

  public void updateTime(LongUnaryOperator mapper) {
    setTime(mapper.applyAsLong(time));
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }
}
