package sharetrace;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.io.IOException;

public class Sandbox {

  public static void main(String[] args) throws IOException {

    RangeMap<Integer, Integer> map = TreeRangeMap.create();
    map.merge(Range.closed(0, 5), 1, Math::max);
    map.merge(Range.closed(0, 7), 2, Math::max);
    System.out.println(map);
  }
}
