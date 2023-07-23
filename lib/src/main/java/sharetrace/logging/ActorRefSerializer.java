package sharetrace.logging;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

@SuppressWarnings("rawtypes")
public final class ActorRefSerializer extends StdSerializer<ActorRef> {

  public ActorRefSerializer() {
    super(ActorRef.class);
  }

  @Override
  public void serialize(ActorRef value, JsonGenerator generator, SerializerProvider serializers)
      throws IOException {
    generator.writeString(value.path().name());
  }
}
