package org.sharetrace.data;

import java.time.Instant;
import java.util.Map;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.model.graph.Edge;
import org.sharetrace.model.message.RiskScore;

public class SyntheticDatasetFactory extends DatasetFactory {

  private final GraphGenerator<Integer, Edge<Integer>, ?> generator;

  public SyntheticDatasetFactory(GraphGenerator<Integer, Edge<Integer>, ?> generator) {
    this.generator = generator;
  }

  @Override
  public void generateGraph(Graph<Integer, Edge<Integer>> target, Map<String, Integer> resultMap) {
    generator.generateGraph(target);
  }

  @Override
  protected RiskScore score(int node) {
    // TODO Ttl
    return RiskScore.builder().value(Math.random()).timestamp(Instant.now()).build();
  }

  @Override
  protected Instant timestamp(int node1, int node2) {
    // TODO Ttl
    return Instant.now();
  }
}
