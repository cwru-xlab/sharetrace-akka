package org.sharetrace.model.graph;

import java.util.Collection;
import java.util.List;

public interface TemporalGraph<T> {

  Collection<T> nodes();

  Collection<List<T>> edges();
}
