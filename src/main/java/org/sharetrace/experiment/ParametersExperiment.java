package org.sharetrace.experiment;

import akka.actor.typed.Behavior;
import java.time.Duration;
import java.util.Random;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.SyntheticDatasetBuilder;
import org.sharetrace.model.message.AlgorithmMessage;
import org.sharetrace.model.message.Parameters;

public class ParametersExperiment extends Experiment<Integer> {

  private final GraphType graphType;
  private final double minTransmissionRate;
  private final double maxTransmissionRate;
  private final double stepTransmissionRate;
  private final double minSendTolerance;
  private final double maxSendTolerance;
  private final double stepSendTolerance;
  private final int nRepeats;
  private final long seed;

  public ParametersExperiment(
      GraphType graphType,
      double minTransmissionRate,
      double maxTransmissionRate,
      double stepTransmissionRate,
      double minSendTolerance,
      double maxSendTolerance,
      double stepSendTolerance,
      int nRepeats,
      long seed) {
    this.graphType = graphType;
    this.minTransmissionRate = minTransmissionRate;
    this.maxTransmissionRate = maxTransmissionRate;
    this.stepTransmissionRate = stepTransmissionRate;
    this.minSendTolerance = minSendTolerance;
    this.maxSendTolerance = maxSendTolerance;
    this.stepSendTolerance = stepSendTolerance;
    this.nRepeats = nRepeats;
    this.seed = seed;
  }

  @Override
  public void run() {
    Parameters parameters;
    Dataset<Integer> dataset;
    Behavior<AlgorithmMessage> algorithm;
    for (double transmissionRate = minTransmissionRate;
        transmissionRate < maxTransmissionRate;
        transmissionRate += stepTransmissionRate) {
      for (double sendTolerance = minSendTolerance;
          sendTolerance < maxSendTolerance;
          sendTolerance += stepSendTolerance) {
        for (int iRepeat = 0; iRepeat < nRepeats; iRepeat++) {
          parameters =
              Parameters.builder()
                  .sendTolerance(sendTolerance)
                  .transmissionRate(transmissionRate)
                  .timeBuffer(DEFAULT_TIME_BUFFER)
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
        .generator(GraphGeneratorFactory.create(graphType, new Random(seed)))
        .clock(clock())
        .scoreTtl(parameters.scoreTtl())
        .contactTtl(parameters.contactTtl())
        .random(new Random(seed))
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
