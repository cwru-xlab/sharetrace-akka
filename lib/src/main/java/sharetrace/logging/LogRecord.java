package sharetrace.logging;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import sharetrace.logging.event.ContactEvent;
import sharetrace.logging.event.CreateUsersEnd;
import sharetrace.logging.event.CreateUsersStart;
import sharetrace.logging.event.ReceiveEvent;
import sharetrace.logging.event.RiskPropagationEnd;
import sharetrace.logging.event.RiskPropagationStart;
import sharetrace.logging.event.SendContactsEnd;
import sharetrace.logging.event.SendContactsStart;
import sharetrace.logging.event.SendEvent;
import sharetrace.logging.event.SendRiskScoresEnd;
import sharetrace.logging.event.SendRiskScoresStart;
import sharetrace.logging.event.UpdateEvent;
import sharetrace.logging.setting.ExperimentSettings;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  // User events
  @Type(value = ContactEvent.class, name = "C"),
  @Type(value = ReceiveEvent.class, name = "R"),
  @Type(value = SendEvent.class, name = "S"),
  @Type(value = UpdateEvent.class, name = "U"),
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
  @Type(value = ExperimentSettings.class)
})
public interface LogRecord {

  static String key() {
    return "key";
  }
}
