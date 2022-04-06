package org.sharetrace.model.graph;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph;
import org.jgrapht.sux4j.SuccinctUndirectedGraph;
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
public class ContactGraph implements TemporalGraph<Number, Edge<Number>> {

  private final Graph<? extends Number, ? extends IntIntPair> graph;

  private ContactGraph(Graph<? extends Number, ? extends IntIntPair> graph) {
    this.graph = graph;
  }

  /** Creates a contact graph that is generated from a {@link GraphGenerator}. */
  public static ContactGraph create(GraphGenerator<Integer, Edge<Number>, ?> generator) {
    Graph<Integer, Edge<Number>> graph = newGraph();
    generator.generateGraph(graph);
    return new ContactGraph(new SuccinctUndirectedGraph(graph));
  }

  private static Graph<Integer, Edge<Number>> newGraph() {
    return new FastutilMapIntVertexGraph<>(
        new NodeIdFactory(), Edge::new, DefaultGraphType.simple());
  }

  @Override
  public Stream<Edge<Number>> edgeStream() {
    return graph.edgeSet().stream().map(EdgeAdapter::new);
  }

  @Override
  public Stream<Number> nodeStream() {
    return graph.vertexSet().stream().map(node -> (Number) node);
  }

  @Override
  public long nNodes() {
    return graph.iterables().vertexCount();
  }

  private static final class EdgeAdapter extends Edge<Number> {

    private final IntIntPair adapted;

    public EdgeAdapter(IntIntPair adapted) {
      this.adapted = adapted;
    }

    @Override
    public Number source() {
      return adapted.leftInt();
    }

    @Override
    public Number target() {
      return adapted.rightInt();
    }
  }

  private static final class NodeIdFactory implements Supplier<Integer> {

    private int id = 0;

    @Override
    public Integer get() {
      return id++;
    }
  }
}
