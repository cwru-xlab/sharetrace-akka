package org.sharetrace.data;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.graph.Edge;
import org.sharetrace.logging.Loggable;
import org.sharetrace.message.RiskScore;

@Value.Immutable
abstract class BaseFileDataset implements Dataset {

  @Override
  public Instant getContactTime(int user1, int user2) {
    return contacts().get(key(user1, user2));
  }

  @Value.Derived
  protected Map<Set<Integer>, Instant> contacts() {
    LastContactTime lastContactTime = new LastContactTime();
    Map<Set<Integer>, Instant> contacts = new HashMap<>(); // Supports Map.Entry.setValue()
    IdIndexer indexer = new IdIndexer();
    try (BufferedReader reader = Files.newBufferedReader(path())) {
      reader.lines().forEach(line -> processLine(line, contacts, indexer, lastContactTime));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    adjustTimestamps(contacts, lastContactTime);
    return contacts;
  }

  private static Set<Integer> key(int user1, int user2) {
    return Set.of(user1, user2);
  }

  protected abstract Path path();

  private void processLine(
      String line,
      Map<Set<Integer>, Instant> contacts,
      IdIndexer indexer,
      LastContactTime lastContactTime) {
    Parsed parsed = new Parsed(line, delimiter(), indexer);
    if (parsed.user1 != parsed.user2) {
      contacts.merge(key(parsed.user1, parsed.user2), parsed.timestamp, BaseFileDataset::newer);
      lastContactTime.update(parsed.timestamp);
    }
  }

  private void adjustTimestamps(
      Map<Set<Integer>, Instant> contacts, LastContactTime lastContactTime) {
    Duration offset = Duration.between(lastContactTime.value, referenceTime());
    contacts.entrySet().forEach(entry -> entry.setValue(entry.getValue().plus(offset)));
  }

  protected abstract String delimiter();

  private static Instant newer(Instant timestamp1, Instant timestamp2) {
    return timestamp1.isAfter(timestamp2) ? timestamp1 : timestamp2;
  }

  protected abstract Instant referenceTime();

  @Override
  public RiskScore getRiskScore(int user) {
    return riskScoreFactory().getRiskScore(user);
  }

  protected abstract RiskScoreFactory riskScoreFactory();

  @Override
  @Value.Derived
  public ContactNetwork getContactNetwork() {
    return ContactNetwork.create(graphGenerator(), loggable());
  }

  private GraphGenerator<Integer, Edge<Integer>, Integer> graphGenerator() {
    return (target, x) -> generateGraph(target);
  }

  protected abstract Set<Class<? extends Loggable>> loggable();

  private void generateGraph(Graph<Integer, Edge<Integer>> target) {
    List<Integer> users;
    for (Entry<Set<Integer>, Instant> entry : contacts().entrySet()) {
      users = List.copyOf(entry.getKey());
      Graphs.addEdgeWithVertices(target, users.get(0), users.get(1));
    }
  }

  private static final class Parsed {

    private final int user1;
    private final int user2;
    private final Instant timestamp;

    private Parsed(String string, String delimiter, IdIndexer indexer) {
      String[] args = string.split(delimiter);
      this.user1 = indexer.index(Integer.parseInt(args[1].strip()));
      this.user2 = indexer.index(Integer.parseInt(args[2].strip()));
      this.timestamp = Instant.ofEpochSecond(Long.parseLong(args[0].strip()));
    }

    @Override
    public String toString() {
      return "Parsed{" + "user1=" + user1 + ", user2=" + user2 + ", timestamp=" + timestamp + '}';
    }
  }

  private static final class LastContactTime {
    private Instant value = Instant.MIN;

    public void update(Instant timestamp) {
      value = newer(value, timestamp);
    }
  }

  private static final class IdIndexer {

    private final Map<Integer, Integer> index = new Int2IntOpenHashMap();
    private int currentId = 0;

    public int index(int id) {
      Integer indexed = index.putIfAbsent(id, currentId);
      return (indexed == null) ? currentId++ : indexed;
    }
  }
}
