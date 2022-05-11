package org.sharetrace.data.factory;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.immutables.builder.Builder;
import org.jgrapht.Graph;
import org.sharetrace.data.Dataset;
import org.sharetrace.graph.Edge;
import org.sharetrace.logging.Loggable;

class FileDatasetFactory extends DatasetFactory {

  private final Map<Set<Integer>, Instant> contacts;
  private final Set<Class<? extends Loggable>> loggable;
  private final ScoreFactory scoreFactory;
  private final Path path;
  private final String delimiter;
  private final Instant referenceTime;
  private Instant lastContact;
  private Duration offset;

  private FileDatasetFactory(
      Set<Class<? extends Loggable>> loggable,
      ScoreFactory scoreFactory,
      Path path,
      String delimiter,
      Instant referenceTime) {
    this.contacts = new Object2ObjectOpenHashMap<>();
    this.loggable = loggable;
    this.scoreFactory = scoreFactory;
    this.path = path;
    this.delimiter = delimiter;
    this.referenceTime = referenceTime;
    this.lastContact = Instant.MIN;
  }

  @Builder.Factory
  public static Dataset fileDataset(
      Set<Class<? extends Loggable>> loggable,
      ScoreFactory scoreFactory,
      Path path,
      String delimiter,
      Instant referenceTime) {
    return new FileDatasetFactory(loggable, scoreFactory, path, delimiter, referenceTime).create();
  }

  @Override
  protected void createTemporalGraph(Graph<Integer, Edge<Integer>> target) {
    try (BufferedReader reader = Files.newBufferedReader(path)) {
      reader.lines().forEach(line -> processLine(line, target));
      adjustTimestamps();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  @Override
  protected Set<Class<? extends Loggable>> loggable() {
    return loggable;
  }

  @Override
  protected ScoreFactory scoreFactory() {
    return scoreFactory;
  }

  @Override
  protected ContactTimeFactory contactTimeFactory() {
    return (node1, node2) -> contacts.get(key(node1, node2));
  }

  private void processLine(String line, Graph<Integer, Edge<Integer>> target) {
    Parsed parsed = new Parsed(line, delimiter);
    addToGraph(target, parsed);
    addContact(parsed);
  }

  private void adjustTimestamps() {
    offset = Duration.between(lastContact, referenceTime);
    contacts.forEach((nodes, timestamp) -> contacts.computeIfPresent(nodes, this::adjustTimestamp));
  }

  private void addToGraph(Graph<Integer, Edge<Integer>> target, Parsed parsed) {
    target.addVertex(parsed.node1);
    target.addVertex(parsed.node2);
    target.addEdge(parsed.node1, parsed.node2);
  }

  private void addContact(Parsed parsed) {
    lastContact = newer(lastContact, parsed.timestamp);
    contacts.merge(key(parsed.node1, parsed.node2), parsed.timestamp, FileDatasetFactory::newer);
  }

  private Instant adjustTimestamp(Set<Integer> nodes, Instant timestamp) {
    return timestamp.plus(offset);
  }

  private static Instant newer(Instant oldValue, Instant newValue) {
    return newValue.isAfter(oldValue) ? newValue : oldValue;
  }

  private static Set<Integer> key(int node1, int node2) {
    return Set.of(node1, node2);
  }

  private static final class Parsed {

    private final int node1;
    private final int node2;
    private final Instant timestamp;

    private Parsed(String string, String delimiter) {
      String[] args = string.split(delimiter);
      this.node1 = Integer.parseInt(args[1]);
      this.node2 = Integer.parseInt(args[2]);
      this.timestamp = Instant.ofEpochSecond(Long.parseLong(args[0]));
    }

    @Override
    public String toString() {
      return "Parsed{" + "node1=" + node1 + ", node2=" + node2 + ", timestamp=" + timestamp + '}';
    }
  }
}
