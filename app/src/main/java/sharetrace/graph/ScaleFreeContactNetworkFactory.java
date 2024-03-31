package sharetrace.graph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.ScaleFreeGraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.factory.GeneratedContactNetworkFactory;
import sharetrace.model.factory.TimeFactory;
import sharetrace.model.graph.TemporalEdge;

@JsonTypeName("ScaleFree")
@Buildable
public record ScaleFreeContactNetworkFactory(
    int nodes, TimeFactory timeFactory, RandomGenerator randomGenerator)
    implements GeneratedContactNetworkFactory {

  @Override
  public GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    return new ScaleFreeGraphGenerator<>(nodes, RandomAdaptor.createAdaptor(randomGenerator));
  }
}
