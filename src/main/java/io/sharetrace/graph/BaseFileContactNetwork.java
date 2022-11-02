package io.sharetrace.graph;

import io.sharetrace.experiment.data.factory.ContactTimeFactory;
import io.sharetrace.model.TimeRef;
import io.sharetrace.util.Collections;
import io.sharetrace.util.Indexer;
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
  protected ContactTimeFactory contactTimeFactory() {
    return (user1, user2) -> contactMap().get(key(user1, user2));
  }

  @Override
  protected GraphGenerator<Integer, DefaultEdge, ?> graphGenerator() {
    return (target, x) -> generate(target);
  }

  private void generate(Graph<Integer, DefaultEdge> target) {
    contactMap().keySet().stream()
        .map(List::copyOf)
        .forEach(users -> Graphs.addEdgeWithVertices(target, users.get(0), users.get(1)));
  }

  @Value.Lazy
  protected Map<Set<Integer>, Instant> contactMap() {
    try (BufferedReader reader = Files.newBufferedReader(path())) {
      return contactsFrom(reader.lines()::iterator);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private Map<Set<Integer>, Instant> contactsFrom(Iterable<String> lines) {
    Instant lastContactTime = Instant.MIN;
    Indexer<String> indexer = new Indexer<>();
    Map<Set<Integer>, Instant> contacts = Collections.newHashMap();
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

  protected abstract Path path();

  private static Set<Integer> key(int user1, int user2) {
    return Collections.ofInts(user1, user2);
  }

  protected abstract String delimiter();

  private static Instant newer(Instant time1, Instant time2) {
    return time1.isAfter(time2) ? time1 : time2;
  }
}
