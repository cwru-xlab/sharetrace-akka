package sharetrace.logging;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import sharetrace.logging.event.ContactEvent;
import sharetrace.logging.event.ReceiveEvent;
import sharetrace.logging.event.SendEvent;
import sharetrace.logging.event.UpdateEvent;
import sharetrace.logging.metric.CreateUsersRuntime;
import sharetrace.logging.metric.GraphCycles;
import sharetrace.logging.metric.GraphEccentricity;
import sharetrace.logging.metric.GraphScores;
import sharetrace.logging.metric.MessagePassingRuntime;
import sharetrace.logging.metric.SendContactsRuntime;
import sharetrace.logging.metric.SendRiskScoresRuntime;
import sharetrace.logging.metric.TotalRuntime;
import sharetrace.logging.setting.ExperimentSettings;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  // Events
  @Type(value = ContactEvent.class, name = "C"),
  @Type(value = ReceiveEvent.class, name = "R"),
  @Type(value = SendEvent.class, name = "S"),
  @Type(value = UpdateEvent.class, name = "U"),
  // Metrics
  @Type(value = CreateUsersRuntime.class),
  @Type(value = GraphCycles.class),
  @Type(value = GraphEccentricity.class),
  @Type(value = GraphScores.class),
  @Type(value = MessagePassingRuntime.class),
  @Type(value = SendContactsRuntime.class),
  @Type(value = SendRiskScoresRuntime.class),
  @Type(value = TotalRuntime.class),
  // Settings
  @Type(value = ExperimentSettings.class)
})
public interface LogRecord {

  static String key() {
    return "key";
  }
}
