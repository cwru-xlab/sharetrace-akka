package org.sharetrace.experiment;

import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.IntStream;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.factory.FileDatasetBuilder;
import org.sharetrace.data.sampling.Sampler;
import org.sharetrace.data.sampling.ScoreSampler;
import org.sharetrace.data.sampling.TimestampSampler;
import org.sharetrace.message.Parameters;
import org.sharetrace.message.RiskScore;

public final class FileExperiment extends Experiment {

  private static final String TAB_DELIMITER = "\t";
  private static final String SPACE_DELIMITER = " ";
  private static final String COMMA_DELIMITER = ",";
  private final Path path;
  private final String delimiter;
  private final int nRepeats;
  private final Sampler<RiskScore> scoreSampler;

  public FileExperiment(Path path, String delimiter, int nRepeats, long seed) {
    super(seed);
    this.path = path;
    this.delimiter = delimiter;
    this.nRepeats = nRepeats;
    this.scoreSampler = newScoreSampler();
  }

  private Sampler<RiskScore> newScoreSampler() {
    return ScoreSampler.builder().timestampSampler(newTimestampSampler()).seed(seed).build();
  }

  private Sampler<Instant> newTimestampSampler() {
    return TimestampSampler.builder()
        .seed(seed)
        .referenceTime(referenceTime)
        .ttl(scoreTtl())
        .build();
  }

  public static void runTabDelimited(Path path, int nRepeats, long seed) {
    new FileExperiment(path, TAB_DELIMITER, nRepeats, seed).run();
  }

  @Override
  public void run() {
    IntStream.range(0, nRepeats).forEach(x -> super.run());
  }

  @Override
  protected Dataset newDataset(Parameters parameters) {
    return FileDatasetBuilder.create()
        .delimiter(delimiter)
        .path(path)
        .addAllLoggable(loggable())
        .referenceTime(referenceTime)
        .scoreFactory(x -> scoreSampler.sample())
        .build();
  }

  public static void runSpaceDelimited(Path path, int nRepeats, long seed) {
    new FileExperiment(path, SPACE_DELIMITER, nRepeats, seed).run();
  }

  public static void runCommaDelimited(Path path, int nRepeats, long seed) {
    new FileExperiment(path, COMMA_DELIMITER, nRepeats, seed).run();
  }
}
