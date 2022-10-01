package io.sharetrace.graph;

import io.sharetrace.data.factory.ContactTimeFactory;
import io.sharetrace.model.TimeRef;
import io.sharetrace.util.Indexer;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;

@Value.Immutable
abstract class BaseFileContactNetwork extends AbstractContactNetwork implements TimeRef {

  @Override
  protected GraphGenerator<Integer, DefaultEdge, ?> graphGenerator() {
    return (target, x) -> generate(target);
  }

  @Override
  protected ContactTimeFactory contactTimeFactory() {
    return (user1, user2) -> contactMap().get(key(user1, user2));
  }

  private void generate(Graph<Integer, DefaultEdge> target) {
    List<Integer> users;
    for (Set<Integer> contact : contactMap().keySet()) {
      users = List.copyOf(contact);
      Graphs.addEdgeWithVertices(target, users.get(0), users.get(1));
    }
  }

  @Value.Lazy
  protected Map<Set<Integer>, Instant> contactMap() {
    try (BufferedReader reader = Files.newBufferedReader(path())) {
      return toContacts(reader.lines()::iterator);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  protected abstract Path path();

  private Map<Set<Integer>, Instant> toContacts(Iterable<String> lines) {
    Instant lastContactTime = Instant.MIN;
    Indexer<String> indexer = new Indexer<>();
    Map<Set<Integer>, Instant> contacts = new Object2ObjectOpenHashMap<>();
    for (String line : lines) {
      String[] args = line.split(delimiter());
      int user1 = indexer.index(args[1].strip());
      int user2 = indexer.index(args[2].strip());
      if (user1 != user2) {
        Instant time = Instant.ofEpochSecond(Long.parseLong(args[0].strip()));
        contacts.merge(key(user1, user2), time, BaseFileContactNetwork::newer);
        lastContactTime = newer(lastContactTime, time);
      }
    }
    Duration offset = Duration.between(lastContactTime, refTime());
    contacts.replaceAll((x, time) -> time.plus(offset));
    return contacts;
  }

  protected abstract String delimiter();

  private static Set<Integer> key(int user1, int user2) {
    return IntSet.of(user1, user2);
  }

  private static Instant newer(Instant time1, Instant time2) {
    return time1.isAfter(time2) ? time1 : time2;
  }
}
