package io.sharetrace.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import io.sharetrace.experiment.data.factory.ContactTimeFactory;
import io.sharetrace.util.Collecting;
import io.sharetrace.util.Uid;
import io.sharetrace.util.logging.Logger;
import io.sharetrace.util.logging.Logging;
import io.sharetrace.util.logging.metric.*;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;

import java.time.Instant;
import java.util.Set;
import java.util.function.Supplier;

@JsonIgnoreType
abstract class AbstractContactNetwork implements ContactNetwork {

    private static final Logger LOGGER = Logging.metricsLogger();

    @Override
    @Value.Derived
    public Set<Integer> users() {
        return Collecting.immutable(graph().vertexSet());
    }

    @Override
    @Value.Derived
    public Set<Contact> contacts() {
        Set<DefaultEdge> edges = graph().edgeSet();
        return edges.stream().map(this::contactFrom).collect(Collecting.toImmutableSet(edges.size()));
    }

    @Override
    public void logMetrics() {
        GraphStats<?, ?> stats = GraphStats.of(graph());
        logMetric(GraphSize.class, stats::graphSize);
        logMetric(GraphCycles.class, stats::graphCycles);
        logMetric(GraphEccentricity.class, stats::graphEccentricity);
        logMetric(GraphScores.class, stats::graphScores);
        if (logMetric(GraphTopology.class, this::graphTopology)) {
            Exporter.export(graph(), id());
        }
    }

    private <T extends LoggableMetric> boolean logMetric(Class<T> type, Supplier<T> metric) {
        return LOGGER.log(LoggableMetric.KEY, type, metric);
    }

    private GraphTopology graphTopology() {
        return GraphTopology.of(id());
    }

    @Value.Lazy
    public String id() {
        return Uid.ofIntString();
    }

    private Contact contactFrom(DefaultEdge edge) {
        int user1 = graph().getEdgeSource(edge);
        int user2 = graph().getEdgeTarget(edge);
        Instant contactTime = contactTimeFactory().get(user1, user2);
        return Contact.builder().user1(user1).user2(user2).time(contactTime).build();
    }

    @Value.Lazy
    protected Graph<Integer, DefaultEdge> graph() {
        return Graphs.newUndirectedGraph(graphGenerator());
    }

    protected abstract GraphGenerator<Integer, DefaultEdge, ?> graphGenerator();

    protected abstract ContactTimeFactory contactTimeFactory();
}
