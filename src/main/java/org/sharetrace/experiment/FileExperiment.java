package org.sharetrace.experiment;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.FileDataset;
import org.sharetrace.data.sampling.RiskScoreSampler;
import org.sharetrace.data.sampling.Sampler;
import org.sharetrace.data.sampling.TimeSampler;
import org.sharetrace.message.RiskScore;

public class FileExperiment extends Experiment {

  private static final String WHITESPACE_DELIMITER = "\\s+";
  private final Path path;
  private final String delimiter;
  private final Sampler<RiskScore> riskScoreSampler;

  private FileExperiment(Builder builder) {
    super(builder);
    this.path = builder.path;
    this.delimiter = builder.delimiter;
    this.riskScoreSampler = newRiskScoreSampler();
  }

  private Sampler<RiskScore> newRiskScoreSampler() {
    return RiskScoreSampler.builder().timeSampler(newTimeSampler()).seed(seed).build();
  }

  private Sampler<Instant> newTimeSampler() {
    return TimeSampler.builder().seed(seed).referenceTime(referenceTime).ttl(scoreTtl()).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  protected Dataset newDataset() {
    return FileDataset.builder()
        .delimiter(delimiter)
        .path(path)
        .addAllLoggable(loggable())
        .referenceTime(referenceTime)
        .riskScoreFactory(x -> riskScoreSampler.sample())
        .build();
  }

  public static class Builder extends Experiment.Builder {
    private Path path;
    private String delimiter = WHITESPACE_DELIMITER;

    public Builder path(Path path) {
      this.path = Objects.requireNonNull(path);
      return this;
    }

    public Builder delimiter(String delimiter) {
      this.delimiter = Objects.requireNonNull(delimiter);
      return this;
    }

    @Override
    public Experiment build() {
      preBuild();
      return new FileExperiment(this);
    }
  }
}
