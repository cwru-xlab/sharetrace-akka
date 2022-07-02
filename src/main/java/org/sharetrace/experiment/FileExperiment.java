package org.sharetrace.experiment;

import java.nio.file.Path;
import java.util.Objects;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.FileDataset;

public class FileExperiment extends Experiment {

  private static final String WHITESPACE_DELIMITER = "\\s+";
  private final Path path;
  private final String delimiter;

  private FileExperiment(Builder builder) {
    super(builder);
    this.path = builder.path;
    this.delimiter = builder.delimiter;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  protected Dataset getDataset() {
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
