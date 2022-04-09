package org.sharetrace.experiment;

import akka.actor.typed.Behavior;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Random;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.FileDatasetBuilder;
import org.sharetrace.model.message.AlgorithmMessage;
import org.sharetrace.model.message.Parameters;

public class SocioPatternsExperiment extends Experiment<Integer> {

  private final Path path;
  private final String delimiter;
  private final int nRepeats;
  private final long seed;

  public SocioPatternsExperiment(Path path, String delimiter, int nRepeats, long seed) {
    this.path = path;
    this.delimiter = delimiter;
    this.nRepeats = nRepeats;
    this.seed = seed;
  }

  @Override
  public void run() {
    Parameters parameters = parameters();
    Dataset<Integer> dataset;
    Behavior<AlgorithmMessage> algorithm;
    for (int iRepeat = 0; iRepeat < nRepeats; iRepeat++) {
      dataset = newDataset(parameters);
      algorithm = newAlgorithm(dataset, parameters);
      Runner.run(algorithm);
    }
  }

  @Override
  protected Dataset<Integer> newDataset(Parameters parameters) {
    return FileDatasetBuilder.create()
        .clock(clock())
        .scoreTtl(parameters.scoreTtl())
        .random(new Random(seed))
        .delimiter(delimiter)
        .path(path)
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
