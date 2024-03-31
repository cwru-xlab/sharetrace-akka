package sharetrace.graph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.factory.GeneratedContactNetworkFactory;
import sharetrace.model.factory.TimeFactory;
import sharetrace.model.graph.TemporalEdge;

@JsonTypeName("WattsStrogatz")
@Buildable
public record WattsStrogatzContactNetworkFactory(
    int nodes,
    int nearestNeighbors,
    double rewiringProbability,
    boolean addInsteadOfRewire,
    TimeFactory timeFactory,
    RandomGenerator randomGenerator)
    implements GeneratedContactNetworkFactory {

  @Override
  public GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    var random = RandomAdaptor.createAdaptor(randomGenerator);
    return new WattsStrogatzGraphGenerator<>(
        nodes, nearestNeighbors, rewiringProbability, addInsteadOfRewire, random);
  }
}
