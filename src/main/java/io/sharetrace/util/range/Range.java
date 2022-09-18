package io.sharetrace.util.range;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Range<T extends Number> extends Iterable<T> {

  default Stream<T> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  default <R extends Number> Range<R> map(Function<T, R> mapper) {
    return () ->
        new Iterator<>() {
          private final Iterator<T> iterator = iterator();

          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public R next() {
            return mapper.apply(iterator.next());
          }
        };
  }
}
