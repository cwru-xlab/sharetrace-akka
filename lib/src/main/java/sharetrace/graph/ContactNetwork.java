package sharetrace.graph;

import org.jgrapht.Graph;
import sharetrace.model.Identifiable;

public interface ContactNetwork<V> extends Graph<V, TemporalEdge>, Identifiable {}
