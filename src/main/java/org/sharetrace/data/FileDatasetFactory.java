package org.sharetrace.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import org.immutables.builder.Builder;
import org.jgrapht.Graph;
import org.sharetrace.model.graph.Edge;
import org.sharetrace.model.message.RiskScore;

class FileDatasetFactory extends DatasetFactory {

  private final Map<Set<Integer>, Instant> contacts;
  private final Path path;
  private final String delimiter;
  private final Supplier<Instant> clock;
  private final long scoreTtlInSeconds;
  private final Random random;
  private Instant lastContact;
  private Duration offset;

  private FileDatasetFactory(
      Path path, String delimiter, Supplier<Instant> clock, Duration scoreTtl, Random random) {
    this.contacts = new Object2ObjectOpenHashMap<>();
    this.path = path;
    this.delimiter = delimiter;
    this.clock = clock;
    this.scoreTtlInSeconds = scoreTtl.toSeconds();
    this.random = random;
    this.lastContact = Instant.MIN;
  }

  @Builder.Factory
  protected static Dataset<Integer> fileDataset(
      Path path, String delimiter, Supplier<Instant> clock, Duration scoreTtl, Random random) {
    return new FileDatasetFactory(path, delimiter, clock, scoreTtl, random).createDataset();
  }

  private static Instant newer(Instant oldValue, Instant newValue) {
    return newValue.isAfter(oldValue) ? newValue : oldValue;
  }

  @Override
  public void generateGraph(Graph<Integer, Edge<Integer>> target, Map<String, Integer> resultMap) {
    try (BufferedReader reader = Files.newBufferedReader(path)) {
      reader.lines().forEach(line -> parseLine(line, target));
      adjustTimestamps();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  @Override
  protected RiskScore scoreOf(int node) {
    Duration lookBack = Duration.ofSeconds(Math.round(random.nextDouble() * scoreTtlInSeconds));
    Instant timestamp = clock.get().minus(lookBack);
    return RiskScore.builder().value(random.nextDouble()).timestamp(timestamp).build();
  }

  @Override
  protected Instant contactedAt(int node1, int node2) {
    return contacts.get(nodes(node1, node2));
  }

  private void parseLine(String line, Graph<Integer, Edge<Integer>> target) {
    Parsed parsed = new Parsed(line, delimiter);
    addToGraph(target, parsed);
    addContact(parsed);
  }

  private void addToGraph(Graph<Integer, Edge<Integer>> target, Parsed parsed) {
    target.addVertex(parsed.node1);
    target.addVertex(parsed.node2);
    target.addEdge(parsed.node1, parsed.node2);
  }

  private void addContact(Parsed parsed) {
    Instant timestamp = parsed.timestamp;
    lastContact = newer(lastContact, timestamp);
    contacts.merge(nodes(parsed.node1, parsed.node2), timestamp, FileDatasetFactory::newer);
  }

  private Set<Integer> nodes(int node1, int node2) {
    return Set.of(node1, node2);
  }

  private void adjustTimestamps() {
    offset = Duration.between(lastContact, clock.get());
    contacts.forEach((nodes, timestamp) -> contacts.computeIfPresent(nodes, this::adjustTimestamp));
  }

  private Instant adjustTimestamp(Set<Integer> nodes, Instant timestamp) {
    return timestamp.plus(offset);
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
