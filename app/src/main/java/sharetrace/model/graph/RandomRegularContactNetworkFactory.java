package sharetrace.model.graph;

import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.RandomRegularGraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.factory.GeneratedContactNetworkFactory;
import sharetrace.model.factory.TimeFactory;

@Buildable
public record RandomRegularContactNetworkFactory(
    int nodes, int degree, TimeFactory timeFactory, RandomGenerator randomGenerator)
    implements GeneratedContactNetworkFactory {

  @Override
  public String type() {
    return "RandomRegular";
  }

  @Override
  public GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    var random = RandomAdaptor.createAdaptor(randomGenerator);
    return new RandomRegularGraphGenerator<>(nodes, degree, random);
  }
}
