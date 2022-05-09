package org.sharetrace.data;

import static org.sharetrace.util.Preconditions.checkArgument;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.graph.ContactGraph;
import org.sharetrace.graph.Edge;
import org.sharetrace.graph.TemporalGraph;
import org.sharetrace.logging.Loggable;
import org.sharetrace.message.RiskScore;

public abstract class DatasetFactory implements GraphGenerator<Integer, Edge<Integer>, Integer> {

  protected final Set<Class<? extends Loggable>> loggable;
  protected final ScoreFactory scoreFactory;
  protected final ContactTimeFactory contactTimeFactory;

  protected DatasetFactory(
      Set<Class<? extends Loggable>> loggable,
      ScoreFactory scoreFactory,
      ContactTimeFactory contactTimeFactory) {
    this.loggable = loggable;
    this.scoreFactory = scoreFactory;
    this.contactTimeFactory = contactTimeFactory;
  }

  @Override
  public final void generateGraph(
      Graph<Integer, Edge<Integer>> target, Map<String, Integer> resultMap) {
    createTemporalGraph(checkGraphType(target));
  }

  @Override
  public final void generateGraph(Graph<Integer, Edge<Integer>> target) {
    createTemporalGraph(checkGraphType(target));
  }

  protected abstract void createTemporalGraph(Graph<Integer, Edge<Integer>> target);

  private static <T> Graph<T, Edge<T>> checkGraphType(Graph<T, Edge<T>> graph) {
    GraphType type = graph.getType();
    checkArgument(type.isSimple(), () -> "Graph must be simple; got " + type);
    return graph;
  }

  public Dataset create() {
    return new Dataset() {

      private final TemporalGraph graph = ContactGraph.create(DatasetFactory.this, loggable);

      @Override
      public RiskScore getScore(int node) {
        return scoreFactory.create(node);
      }

      @Override
      public Instant getContactTime(int node1, int node2) {
        return contactTimeFactory.create(node1, node2);
      }

      @Override
      public TemporalGraph graph() {
        return graph;
      }

      @Override
      public String toString() {
        return "Dataset{" + "nNodes=" + graph.nNodes() + ", " + "nEdges=" + graph.nEdges() + '}';
      }
    };
  }
}
