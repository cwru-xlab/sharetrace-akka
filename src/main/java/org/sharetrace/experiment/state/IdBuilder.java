package org.sharetrace.experiment.state;

import java.util.function.Function;

public interface IdBuilder extends MdcBuilder {

  MdcBuilder id(String id);

  MdcBuilder id(Function<IdContext, String> factory);
}
