package org.sharetrace.data.factory;

import static org.sharetrace.util.Preconditions.checkArgument;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.data.Dataset;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.graph.Edge;
import org.sharetrace.logging.Loggable;
import org.sharetrace.message.RiskScore;

public abstract class DatasetFactory
    implements Dataset, GraphGenerator<Integer, Edge<Integer>, Integer> {

  @Override
  public final void generateGraph(
      Graph<Integer, Edge<Integer>> target, Map<String, Integer> resultMap) {
    createContactNetwork(checkGraphType(target));
  }

  @Override
  public final void generateGraph(Graph<Integer, Edge<Integer>> target) {
    createContactNetwork(checkGraphType(target));
  }

  protected abstract void createContactNetwork(Graph<Integer, Edge<Integer>> target);

  private static <T> Graph<T, Edge<T>> checkGraphType(Graph<T, Edge<T>> graph) {
    GraphType type = graph.getType();
    checkArgument(type.isSimple(), () -> "Graph must be simple; got " + type);
    return graph;
  }

  public Dataset create() {
    return new Dataset() {

      private final ContactNetwork contactNetwork = DatasetFactory.this.contactNetwork();

      @Override
      public RiskScore getRiskScore(int user) {
        return DatasetFactory.this.getRiskScore(user);
      }

      @Override
      public Instant getContactTime(int user1, int user2) {
        return DatasetFactory.this.getContactTime(user1, user2);
      }

      @Override
      public ContactNetwork contactNetwork() {
        return contactNetwork;
      }

      @Override
      public String toString() {
        return "Dataset{"
            + "nUsers="
            + contactNetwork.nUsers()
            + ", "
            + "nContacts="
            + contactNetwork.nContacts()
            + '}';
      }
    };
  }

  @Override
  public ContactNetwork contactNetwork() {
    return ContactNetwork.create(this, loggable());
  }

  protected abstract Set<Class<? extends Loggable>> loggable();
}
