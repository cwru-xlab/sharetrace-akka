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
import org.jgrapht.Graph;
import org.sharetrace.model.graph.Edge;
import org.sharetrace.model.message.RiskScore;

public class SocioPatternsDatasetFactory extends DatasetFactory {

  private static final String DEFAULT_DELIMITER = ",";
  private final Map<Set<Integer>, Instant> contacts;
  private final Supplier<Instant> clock;
  private final Path path;

  private SocioPatternsDatasetFactory(Supplier<Instant> clock, Path path) {
    this.contacts = new HashMap<>();
    this.clock = clock;
    this.path = path;
  }

  public static Dataset<Integer> newDataset(Supplier<Instant> clock, Path path) {
    return new SocioPatternsDatasetFactory(clock, path).createDataset();
  }

  private static Instant merge(Instant oldValue, Instant newValue) {
    return newValue.isAfter(oldValue) ? newValue : oldValue;
  }

  @Override
  public void generateGraph(Graph<Integer, Edge<Integer>> target, Map<String, Integer> resultMap) {
    Parsed parsed;
    try (BufferedReader reader = Files.newBufferedReader(path)) {
      parsed = new Parsed(reader.readLine(), DEFAULT_DELIMITER);
      addToGraph(target, parsed);
      addContact(parsed);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  @Override
  protected RiskScore score(int node) {
    // TODO Ttl
    Duration lookBack = Duration.ofDays(Math.round(Math.random() * 13));
    return RiskScore.builder().value(Math.random()).timestamp(clock.get().minus(lookBack)).build();
  }

  @Override
  protected Instant timestamp(int node1, int node2) {
    return contacts.get(Set.of(node1, node2));
  }

  private void addToGraph(Graph<Integer, Edge<Integer>> target, Parsed parsed) {
    target.addVertex(parsed.node1);
    target.addVertex(parsed.node2);
    target.addEdge(parsed.node1, parsed.node2);
  }

  private void addContact(Parsed parsed) {
    Set<Integer> nodes = Set.of(parsed.node1, parsed.node2);
    contacts.merge(nodes, parsed.timestamp, SocioPatternsDatasetFactory::merge);
  }

  private static final class Parsed {

    private final int node1;
    private final int node2;
    private final Instant timestamp;

    private Parsed(String string, String delimiter) {
      String[] args = string.split(delimiter);
      node1 = Integer.parseInt(args[0]);
      node2 = Integer.parseInt(args[1]);
      timestamp = Instant.ofEpochSecond(Long.parseLong(args[2]));
    }
  }
}
