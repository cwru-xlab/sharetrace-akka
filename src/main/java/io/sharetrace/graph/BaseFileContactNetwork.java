package io.sharetrace.graph;

import io.sharetrace.experiment.data.factory.ContactTimeFactory;
import io.sharetrace.model.TimeRef;
import io.sharetrace.util.Collecting;
import io.sharetrace.util.Indexer;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;

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
import java.util.function.BinaryOperator;

@Value.Immutable
abstract class BaseFileContactNetwork extends AbstractContactNetwork implements TimeRef {

    private static final BinaryOperator<Instant> NEWER = BinaryOperator.maxBy(Instant::compareTo);

    private static Set<Integer> key(int user1, int user2) {
        return Collecting.ofInts(user1, user2);
    }

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
            return newContactMap(reader.lines()::iterator);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    protected abstract Path path();

    private Map<Set<Integer>, Instant> newContactMap(Iterable<String> contacts) {
        Instant lastContactTime = Instant.MIN;
        Indexer<String> indexer = new Indexer<>();
        Map<Set<Integer>, Instant> contactMap = Collecting.newHashMap();
        for (String contact : contacts) {
            String[] args = contact.split(delimiter());
            int user1 = indexer.index(args[1].strip());
            int user2 = indexer.index(args[2].strip());
            if (user1 != user2) {
                Instant contactTime = Instant.ofEpochSecond(Long.parseLong(args[0].strip()));
                contactMap.merge(key(user1, user2), contactTime, NEWER);
                lastContactTime = NEWER.apply(lastContactTime, contactTime);
            }
        }
        Duration offset = Duration.between(lastContactTime, refTime());
        contactMap.replaceAll((x, contactTime) -> contactTime.plus(offset));
        return contactMap;
    }

    protected abstract String delimiter();
}
