package sharetrace.logging;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import sharetrace.logging.event.user.ContactEvent;
import sharetrace.logging.event.lifecycle.CreateUsersEnd;
import sharetrace.logging.event.lifecycle.CreateUsersStart;
import sharetrace.logging.event.user.LastEvent;
import sharetrace.logging.event.user.ReceiveEvent;
import sharetrace.logging.event.lifecycle.RiskPropagationEnd;
import sharetrace.logging.event.lifecycle.RiskPropagationStart;
import sharetrace.logging.event.lifecycle.SendContactsEnd;
import sharetrace.logging.event.lifecycle.SendContactsStart;
import sharetrace.logging.event.user.SendEvent;
import sharetrace.logging.event.lifecycle.SendRiskScoresEnd;
import sharetrace.logging.event.lifecycle.SendRiskScoresStart;
import sharetrace.logging.event.user.UpdateEvent;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  // User events
  @Type(value = ContactEvent.class, name = "C"),
  @Type(value = ReceiveEvent.class, name = "R"),
  @Type(value = SendEvent.class, name = "S"),
  @Type(value = UpdateEvent.class, name = "U"),
  @Type(value = LastEvent.class, name = "L"),
  // Risk propagation events
  @Type(value = CreateUsersStart.class, name = "CUS"),
  @Type(value = CreateUsersEnd.class, name = "CUE"),
  @Type(value = SendContactsStart.class, name = "SCS"),
  @Type(value = SendContactsEnd.class, name = "SCE"),
  @Type(value = SendRiskScoresStart.class, name = "SSS"),
  @Type(value = SendRiskScoresEnd.class, name = "SSE"),
  @Type(value = RiskPropagationStart.class, name = "RPS"),
  @Type(value = RiskPropagationEnd.class, name = "RPE"),
  // Settings
  @Type(value = Settings.class)
})
public interface LogRecord {}
