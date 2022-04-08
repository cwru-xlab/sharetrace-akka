package org.sharetrace.experiment;

import akka.actor.typed.Behavior;
import java.time.Duration;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.SyntheticDatasetBuilder;
import org.sharetrace.model.message.AlgorithmMessage;
import org.sharetrace.model.message.Parameters;

public class ParametersExperiment extends AbstractExperiment<Integer> {

  public static void main(String[] args) {
    new ParametersExperiment().run();
  }

  @Override
  protected Dataset<Integer> newDataset(Parameters parameters) {
    return SyntheticDatasetBuilder.create()
        .generator(new GnmRandomGraphGenerator<>(10000, 50000))
        .clock(clock())
        .scoreTtl(parameters.scoreTtl())
        .build();
  }

  @Override
  protected Behavior<AlgorithmMessage> newAlgorithm(
      Dataset<Integer> dataset, Parameters parameters) {
    return RiskPropagationBuilder.<Integer>create()
        .graph(dataset.graph())
        .parameters(parameters)
        .clock(clock())
        .scoreFactory(dataset::scoreOf)
        .timeFactory(dataset::contactedAt)
        .cacheFactory(cacheFactory())
        .nodeTimeout(Duration.ofSeconds(5L))
        .nodeRefreshRate(Duration.ofHours(1L))
        .build();
  }
}
