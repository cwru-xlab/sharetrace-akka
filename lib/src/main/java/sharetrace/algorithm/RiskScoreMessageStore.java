package sharetrace.algorithm;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.time.InstantSource;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BinaryOperator;
import sharetrace.model.message.RiskScoreMessage;

final class RiskScoreMessageStore extends ExpirableStore<RiskScoreMessage> {

  private static final Comparator<RiskScoreMessage> COMPARATOR = Comparator.naturalOrder();
  private static final BinaryOperator<RiskScoreMessage> MERGER = BinaryOperator.maxBy(COMPARATOR);

  private final RangeMap<Long, RiskScoreMessage> store;

  public RiskScoreMessageStore(InstantSource timeSource) {
    super(timeSource);
    store = TreeRangeMap.create();
  }

  public Optional<RiskScoreMessage> max(Range<Long> range) {
    refresh();
    return values(store.subRangeMap(range)).stream().max(COMPARATOR);
  }

  @Override
  public void add(RiskScoreMessage message) {
    var key = Range.closedOpen(message.timestamp(), message.expiryTime());
    store.merge(key, message, MERGER);
    super.add(message);
  }

  @Override
  protected Collection<RiskScoreMessage> values() {
    return values(store);
  }

  private Collection<RiskScoreMessage> values(RangeMap<Long, RiskScoreMessage> store) {
    return store.asMapOfRanges().values();
  }
}
