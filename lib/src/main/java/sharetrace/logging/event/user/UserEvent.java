package sharetrace.logging.event.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import sharetrace.logging.event.Event;

public interface UserEvent extends Event {

  @JsonProperty("s")
  int self();
}
