package sharetrace.graph;

import org.jgrapht.Graph;
import sharetrace.model.Identifiable;

public interface ContactNetwork extends Graph<Integer, TemporalEdge>, Identifiable {}
