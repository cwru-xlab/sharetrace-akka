package org.sharetrace.data;

import static org.sharetrace.util.Preconditions.checkArgument;
import java.time.Instant;
import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.model.graph.ContactGraph;
import org.sharetrace.model.graph.Edge;
import org.sharetrace.model.graph.TemporalGraph;
import org.sharetrace.model.message.RiskScore;

public abstract class DatasetFactory implements GraphGenerator<Integer, Edge<Integer>, Integer> {

  private static <T> Graph<T, Edge<T>> checkGraphType(Graph<T, Edge<T>> graph) {
    GraphType type = graph.getType();
    checkArgument(type.isSimple(), () -> "Graph must be simple; got " + type);
    return graph;
  }

  @Override
  public void generateGraph(Graph<Integer, Edge<Integer>> target) {
    generateGraph(checkGraphType(target), null);
  }

  public Dataset<Integer> createDataset() {
    return new Dataset<>() {

      private final TemporalGraph<Integer> graph = ContactGraph.create(DatasetFactory.this);

      @Override
      public RiskScore scoreOf(Integer node) {
        return DatasetFactory.this.scoreOf(node);
      }

      @Override
      public Instant contactedAt(Integer node1, Integer node2) {
        return DatasetFactory.this.contactedAt(node1, node2);
      }

      @Override
      public TemporalGraph<Integer> graph() {
        return graph;
      }

      @Override
      public String toString() {
        return "Dataset{" + "nNodes=" + graph.nNodes() + ", " + "nEdges=" + graph.nEdges() + '}';
      }
    };
  }

  protected abstract RiskScore scoreOf(int node);

  protected abstract Instant contactedAt(int node1, int node2);
}
