package sharetrace.model.message;

import org.immutables.value.Value;

@Value.Immutable
interface BaseTimedOutMessage extends UserMessage, AlgorithmMessage {

  @Value.Parameter
  int timeoutId();
}
