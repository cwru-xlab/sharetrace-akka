package org.sharetrace.model.graph;

import java.util.function.Supplier;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.SimpleGraph;
import org.sharetrace.RiskPropagation;

/**
 * A simple graph in which a node represents a person and an edge between two nodes indicates that
 * the associated persons of the incident nodes came in contact. Nodes identifiers are zero-based
 * contiguous natural numbers. In an instance of {@link RiskPropagation}, the topology of this graph
 * is mapped to a collection {@link Node} actors.
 *
 * @see Node
 * @see Edge
 */
public class ContactGraph extends SimpleGraph<Number, Edge<Number>> {

  private ContactGraph() {
    super(new NodeIdSupplier(), Edge::new, false);
  }

  /** Creates an empty contact graph. */
  public static ContactGraph create() {
    return new ContactGraph();
  }

  /** Creates a contact graph that is generated from a {@link GraphGenerator}. */
  public static ContactGraph create(GraphGenerator<Number, Edge<Number>, ?> generator) {
    ContactGraph graph = create();
    generator.generateGraph(graph);
    return graph;
  }

  private static final class NodeIdSupplier implements Supplier<Number> {

    private int id = 0;

    @Override
    public Number get() {
      return id++;
    }
  }
}
