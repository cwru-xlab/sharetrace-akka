package org.sharetrace.experiment;

import java.nio.file.Path;
import java.time.Instant;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.factory.FileDatasetBuilder;
import org.sharetrace.data.sampling.Sampler;
import org.sharetrace.data.sampling.ScoreSampler;
import org.sharetrace.data.sampling.TimestampSampler;
import org.sharetrace.message.Parameters;
import org.sharetrace.message.RiskScore;

public class FileExperiment extends Experiment {

  private static final String WHITESPACE_DELIMITER = "\\s+";
  private static final String COMMA_DELIMITER = ",";
  private final Path path;
  private final String delimiter;
  private final Sampler<RiskScore> scoreSampler;

  public FileExperiment(Path path, String delimiter, int nRepeats, long seed) {
    super(nRepeats, seed);
    this.path = path;
    this.delimiter = delimiter;
    this.scoreSampler = newScoreSampler();
  }

  protected Sampler<RiskScore> newScoreSampler() {
    return ScoreSampler.builder().timestampSampler(newTimestampSampler()).seed(seed).build();
  }

  protected Sampler<Instant> newTimestampSampler() {
    return TimestampSampler.builder()
        .seed(seed)
        .referenceTime(referenceTime)
        .ttl(scoreTtl())
        .build();
  }

  public static void runWhitespaceDelimited(Path path, int nRepeats, long seed) {
    new FileExperiment(path, WHITESPACE_DELIMITER, nRepeats, seed).run();
  }

  public static void runCommaDelimited(Path path, int nRepeats, long seed) {
    new FileExperiment(path, COMMA_DELIMITER, nRepeats, seed).run();
  }

  @Override
  protected Dataset dataset(Parameters parameters) {
    return FileDatasetBuilder.create()
        .delimiter(delimiter)
        .path(path)
        .addAllLoggable(loggable())
        .referenceTime(referenceTime)
        .scoreFactory(x -> scoreSampler.sample())
        .build();
  }
}
