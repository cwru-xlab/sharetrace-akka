package org.sharetrace.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.immutables.builder.Builder;
import org.jgrapht.Graph;
import org.sharetrace.model.graph.Edge;
import org.sharetrace.model.message.RiskScore;

public class SocioPatternsDatasetFactory extends DatasetFactory {

  private final Map<Set<Integer>, Instant> contacts;
  private final Path path;
  private final Supplier<Instant> clock;
  private final long scoreTtlInSeconds;
  private final String delimiter;
  private Instant firstContact;
  private Instant lastContact;
  private Duration offset;

  private SocioPatternsDatasetFactory(
      Path path, String delimiter, Supplier<Instant> clock, Duration scoreTtl) {
    this.contacts = new HashMap<>();
    this.path = path;
    this.delimiter = delimiter;
    this.clock = clock;
    this.scoreTtlInSeconds = scoreTtl.toSeconds();
    this.firstContact = Instant.MAX;
    this.lastContact = Instant.MIN;
  }

  @Builder.Factory
  protected static Dataset<Integer> socioPatternsDataset(
      Path path, String delimiter, Supplier<Instant> clock, Duration scoreTtl) {
    return new SocioPatternsDatasetFactory(path, delimiter, clock, scoreTtl).createDataset();
  }

  private static Instant merge(Instant oldValue, Instant newValue) {
    return newValue.isAfter(oldValue) ? newValue : oldValue;
  }

  @Override
  public void generateGraph(Graph<Integer, Edge<Integer>> target, Map<String, Integer> resultMap) {
    try (BufferedReader reader = Files.newBufferedReader(path)) {
      reader.lines().forEach(line -> onLine(line, target));
      calibrateTime();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  @Override
  protected RiskScore score(int node) {
    Duration lookBack = Duration.ofSeconds(Math.round(Math.random() * scoreTtlInSeconds));
    return RiskScore.builder().value(Math.random()).timestamp(lastContact.minus(lookBack)).build();
  }

  @Override
  protected Instant timestamp(int node1, int node2) {
    return contacts.get(Set.of(node1, node2));
  }

  private void onLine(String line, Graph<Integer, Edge<Integer>> target) {
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
    // Encourage max propagation: the earliest contact timestamp > the latest risk score timestamp.
    firstContact = timestamp.isBefore(firstContact) ? timestamp : firstContact;
    lastContact = timestamp.isAfter(lastContact) ? timestamp : lastContact;
    Set<Integer> nodes = Set.of(parsed.node1, parsed.node2);
    contacts.merge(nodes, timestamp, SocioPatternsDatasetFactory::merge);
  }

  private void calibrateTime() {
    offset = Duration.between(lastContact, clock.get());
    lastContact = lastContact.plus(offset);
    firstContact = firstContact.plus(offset);
    contacts.forEach((nodes, timestamp) -> contacts.computeIfPresent(nodes, this::addOffset));
  }

  private Instant addOffset(Set<Integer> nodes, Instant timestamp) {
    return timestamp.plus(offset);
  }

  private static final class Parsed {

    private final int node1;
    private final int node2;
    private final Instant timestamp;

    private Parsed(String string, String delimiter) {
      String[] args = string.split(delimiter);
      timestamp = Instant.ofEpochSecond(Long.parseLong(args[0]));
      node1 = Integer.parseInt(args[1]);
      node2 = Integer.parseInt(args[2]);
    }
  }
}
