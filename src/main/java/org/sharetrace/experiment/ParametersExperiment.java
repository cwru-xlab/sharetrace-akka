package org.sharetrace.experiment;

import akka.actor.typed.Behavior;
import java.time.Duration;
import java.util.Random;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.SyntheticDatasetBuilder;
import org.sharetrace.model.message.AlgorithmMessage;
import org.sharetrace.model.message.Parameters;

public class ParametersExperiment extends AbstractExperiment<Integer> {

  public static void main(String[] args) {
    new ParametersExperiment().run();
  }

  @Override
  public void run() {
    Parameters parameters;
    Dataset<Integer> dataset;
    Behavior<AlgorithmMessage> algorithm;
    for (double transmissionRate = 0.1d; transmissionRate < 1d; transmissionRate += 0.1d) {
      for (double sendTolerance = 0.1d; sendTolerance <= 1d; sendTolerance += 0.1d) {
        for (int repeat = 0; repeat < 10; repeat++) {
          parameters =
              Parameters.builder()
                  .sendTolerance(sendTolerance)
                  .transmissionRate(transmissionRate)
                  .timeBuffer(Duration.ofDays(2L))
                  .scoreTtl(DEFAULT_TTL)
                  .contactTtl(DEFAULT_TTL)
                  .build();
          dataset = newDataset(parameters);
          algorithm = newAlgorithm(dataset, parameters);
          Runner.run(algorithm);
        }
      }
    }
  }

  @Override
  protected Dataset<Integer> newDataset(Parameters parameters) {
    return SyntheticDatasetBuilder.create()
        .generator(new GnmRandomGraphGenerator<>(10000, 50000))
        .clock(clock())
        .scoreTtl(parameters.scoreTtl())
        .random(new Random(DEFAULT_SEED))
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
