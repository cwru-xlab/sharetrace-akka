package org.sharetrace.experiment.state;

import java.util.function.Function;
import org.sharetrace.experiment.GraphType;

public interface GraphTypeBuilder extends IdBuilder {

  IdBuilder graphType(GraphType graphType);

  IdBuilder graphType(Function<GraphTypeContext, GraphType> factory);
}
