package org.sharetrace.model.graph;

import java.util.function.Supplier;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.SimpleGraph;

public class ContactGraph extends SimpleGraph<Long, Edge<Long>> {

  private ContactGraph() {
    super(new NodeIdSupplier(), Edge::new, false);
  }

  public static ContactGraph create() {
    return new ContactGraph();
  }

  public static ContactGraph create(GraphGenerator<Long, Edge<Long>, Long> generator) {
    ContactGraph graph = create();
    generator.generateGraph(graph);
    return graph;
  }

  private static final class NodeIdSupplier implements Supplier<Long> {

    private long id = 0L;

    @Override
    public Long get() {
      return id++;
    }
  }
}
