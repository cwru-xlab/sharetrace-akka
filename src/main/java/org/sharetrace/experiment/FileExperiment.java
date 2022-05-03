package org.sharetrace.experiment;

import java.nio.file.Path;
import java.util.Random;
import java.util.stream.IntStream;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.FileDatasetBuilder;
import org.sharetrace.message.Parameters;

public class FileExperiment extends Experiment {

  private static final String TAB_DELIMITER = "\t";
  private static final String SPACE_DELIMITER = " ";
  private static final String COMMA_DELIMITER = ",";
  private final Path path;
  private final String delimiter;
  private final int nRepeats;
  private final long seed;

  public FileExperiment(Path path, String delimiter, int nRepeats, long seed) {
    this.path = path;
    this.delimiter = delimiter;
    this.nRepeats = nRepeats;
    this.seed = seed;
  }

  public static void runTabDelimited(Path path, int nRepeats, long seed) {
    new FileExperiment(path, TAB_DELIMITER, nRepeats, seed).run();
  }

  @Override
  public void run() {
    IntStream.range(0, nRepeats).forEach(x -> super.run());
  }

  @Override
  protected Dataset<Integer> newDataset(Parameters parameters) {
    return FileDatasetBuilder.create()
        .time(clock().get())
        .scoreTtl(parameters.scoreTtl())
        .random(new Random(seed))
        .delimiter(delimiter)
        .path(path)
        .build();
  }

  public static void runSpaceDelimited(Path path, int nRepeats, long seed) {
    new FileExperiment(path, SPACE_DELIMITER, nRepeats, seed).run();
  }

  public static void runCommaDelimited(Path path, int nRepeats, long seed) {
    new FileExperiment(path, COMMA_DELIMITER, nRepeats, seed).run();
  }
}
