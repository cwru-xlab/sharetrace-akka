package sharetrace.graph;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.opt.graph.fastutil.FastutilMapGraph;
import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph;

public final class TemporalNetworkFactoryHelper {

  public static <V> Graph<V, TemporalEdge> newTarget() {
    return new FastutilMapGraph<>(null, TemporalEdge::new, DefaultGraphType.simple(), false);
  }

  public static Graph<Integer, TemporalEdge> newIntTarget() {
    return new FastutilMapIntVertexGraph<>(
        intVertexFactory(), TemporalEdge::new, DefaultGraphType.simple(), false);
  }

  private static Supplier<Integer> intVertexFactory() {
    return new AtomicInteger(0)::getAndIncrement;
  }
}
