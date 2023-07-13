package sharetrace.util.logging.event;

import org.immutables.value.Value;
import sharetrace.model.message.RiskScoreMessage;

@Value.Immutable
interface BaseUpdateEvent extends EventRecord {

  RiskScoreMessage previous();

  RiskScoreMessage current();
}
