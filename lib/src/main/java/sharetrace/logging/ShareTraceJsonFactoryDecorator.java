package sharetrace.logging;

import com.fasterxml.jackson.core.JsonFactory;
import net.logstash.logback.decorate.JsonFactoryDecorator;

public record ShareTraceJsonFactoryDecorator() implements JsonFactoryDecorator {

  private static final JsonFactory INSTANCE = Jackson.newIonJsonFactory();

  @Override
  public JsonFactory decorate(JsonFactory factory) {
    return INSTANCE;
  }
}
