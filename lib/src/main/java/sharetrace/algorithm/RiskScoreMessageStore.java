package sharetrace.algorithm;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BinaryOperator;
import sharetrace.model.factory.TimeFactory;
import sharetrace.model.message.RiskScoreMessage;

final class RiskScoreMessageStore extends ExpirableStore<RiskScoreMessage> {

  private static final BinaryOperator<RiskScoreMessage> MAX_OPERATOR = maxOperator();

  private final RangeMap<Long, RiskScoreMessage> store;

  public RiskScoreMessageStore(TimeFactory timeFactory) {
    super(timeFactory);
    store = TreeRangeMap.create();
  }

  public Optional<RiskScoreMessage> max(Range<Long> range) {
    refresh();
    return values(store.subRangeMap(range)).stream().reduce(MAX_OPERATOR);
  }

  @Override
  public void add(RiskScoreMessage message) {
    var key = Range.closedOpen(message.timestamp(), message.expiryTime());
    store.merge(key, message, MAX_OPERATOR);
    super.add(message);
  }

  @Override
  protected Collection<RiskScoreMessage> values() {
    return values(store);
  }

  private static Collection<RiskScoreMessage> values(RangeMap<Long, RiskScoreMessage> store) {
    return store.asMapOfRanges().values();
  }

  private static BinaryOperator<RiskScoreMessage> maxOperator() {
    return BinaryOperator.maxBy(comparator());
  }

  private static Comparator<RiskScoreMessage> comparator() {
    return Comparator.comparingDouble(RiskScoreMessage::value)
        .thenComparingLong(RiskScoreMessage::expiryTime);
  }
}
