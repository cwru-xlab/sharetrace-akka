package io.sharetrace.graph;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.opt.graph.fastutil.FastutilMapGraph;
import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph;

public final class TemporalNetworkFactoryHelper {

  public static Graph<String, TemporalEdge> newStringTarget() {
    return newTarget(stringVertexFactory());
  }

  public static <V> Graph<V, TemporalEdge> newTarget(Supplier<V> vertexFactory) {
    return new FastutilMapGraph<>(
        vertexFactory, TemporalEdge::new, DefaultGraphType.simple(), false);
  }

  public static Graph<Integer, TemporalEdge> newIntTarget() {
    return new FastutilMapIntVertexGraph<>(
        intVertexFactory(), TemporalEdge::new, DefaultGraphType.simple(), false);
  }

  public static Supplier<String> stringVertexFactory() {
    Supplier<Integer> intVertexFactory = intVertexFactory();
    return () -> String.valueOf(intVertexFactory.get());
  }

  public static Supplier<Integer> intVertexFactory() {
    return new AtomicInteger(0)::getAndIncrement;
  }
}
