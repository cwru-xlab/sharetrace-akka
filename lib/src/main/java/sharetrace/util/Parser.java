package sharetrace.util;

import java.util.List;
import java.util.stream.StreamSupport;

@FunctionalInterface
public interface Parser<I, O> {

  O parse(I input);

  default List<O> parse(Iterable<? extends I> inputs) {
    return StreamSupport.stream(inputs.spliterator(), false).map(this::parse).toList();
  }
}
