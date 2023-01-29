package io.sharetrace.graph;

import io.sharetrace.util.Stats;
import io.sharetrace.util.logging.metric.GraphCycles;
import io.sharetrace.util.logging.metric.GraphEccentricity;
import io.sharetrace.util.logging.metric.GraphScores;
import io.sharetrace.util.logging.metric.GraphSize;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.GraphMetrics;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.VertexScoringAlgorithm;
import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.alg.scoring.ClusteringCoefficient;
import org.jgrapht.alg.scoring.Coreness;
import org.jgrapht.alg.scoring.EigenvectorCentrality;
import org.jgrapht.alg.scoring.KatzCentrality;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.alg.shortestpath.GraphMeasurer;

@Value.Immutable
abstract class BaseGraphStats<V, E> {

    private static Stats computeScoreStats(VertexScoringAlgorithm<?, ? extends Number> algorithm) {
        return Stats.of(algorithm.getScores().values());
    }

    @Value.Lazy
    public GraphSize graphSize() {
        return GraphSize.builder()
                .numNodes(graph().vertexSet().size())
                .numEdges(graph().edgeSet().size())
                .build();
    }

    @Value.Parameter
    protected abstract Graph<V, E> graph();

    @Value.Lazy
    public GraphCycles graphCycles() {
        return GraphCycles.builder()
                .numTriangles(GraphMetrics.getNumberOfTriangles(graph()))
                .girth(GraphMetrics.getGirth(graph()))
                .build();
    }

    @Value.Lazy
    public GraphEccentricity graphEccentricity() {
        GraphMeasurer<?, ?> measurer = new GraphMeasurer<>(graph(), shortestPath());
        return GraphEccentricity.builder()
                .radius((int) measurer.getRadius())
                .diameter((int) measurer.getDiameter())
                .center(measurer.getGraphCenter().size())
                .periphery(measurer.getGraphPeriphery().size())
                .build();
    }

    @Value.Lazy
    public GraphScores graphScores() {
        return GraphScores.builder()
                .degeneracy(degeneracy())
                .globalClusteringCoefficient(globalClusteringCoefficient())
                .localClusteringCoefficient(localClusteringCoefficients())
                .harmonicCentrality(harmonicCentralities())
                .katzCentrality(katzCentralities())
                .eigenvectorCentrality(eigenvectorCentralities())
                .build();
    }

    private int degeneracy() {
        return new Coreness<>(graph()).getDegeneracy();
    }

    private float globalClusteringCoefficient() {
        return (float) new ClusteringCoefficient<>(graph()).getGlobalClusteringCoefficient();
    }

    private Stats localClusteringCoefficients() {
        return computeScoreStats(new ClusteringCoefficient<>(graph()));
    }

    private Stats harmonicCentralities() {
        return computeScoreStats(new HarmonicCentrality<>(graph(), shortestPath()));
    }

    private Stats katzCentralities() {
        return computeScoreStats(new KatzCentrality<>(graph()));
    }

    private Stats eigenvectorCentralities() {
        return computeScoreStats(new EigenvectorCentrality<>(graph()));
    }

    @Value.Lazy
    protected ShortestPathAlgorithm<V, E> shortestPath() {
        return new FloydWarshallShortestPaths<>(graph());
    }

    private static final class HarmonicCentrality<V, E> extends ClosenessCentrality<V, E> {

        private final ShortestPathAlgorithm<V, E> shortestPaths;

        public HarmonicCentrality(Graph<V, E> graph, ShortestPathAlgorithm<V, E> shortestPaths) {
            super(graph);
            this.shortestPaths = shortestPaths;
        }

        @Override
        protected ShortestPathAlgorithm<V, E> getShortestPathAlgorithm() {
            return shortestPaths;
        }
    }
}
