package io.sharetrace.util.logging;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import net.logstash.logback.decorate.JsonFactoryDecorator;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public final class LoggingDecorator implements JsonFactoryDecorator {

    @Override
    public JsonFactory decorate(JsonFactory factory) {
        ObjectMapper codec = (ObjectMapper) factory.getCodec();
        registerJdk8(codec);
        registerJavaTime(codec);
        registerBlackbird(codec);
        return factory;
    }

    private static void registerJdk8(ObjectMapper mapper) {
        mapper.registerModule(new Jdk8Module());
    }

    private static void registerJavaTime(ObjectMapper mapper) {
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(Instant.class, new SecondsInstantSerializer());
        module.addSerializer(Duration.class, new SecondsDurationSerializer());
        mapper.registerModule(module);
    }

    private static void registerBlackbird(ObjectMapper mapper) {
        mapper.registerModule(new BlackbirdModule());
    }

    private static final class SecondsInstantSerializer extends InstantSerializer {

        @Override
        public void serialize(Instant value, JsonGenerator generator, SerializerProvider provider)
                throws IOException {
            generator.writeNumber(value.getEpochSecond());
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
            return this;
        }
    }

    private static final class SecondsDurationSerializer extends DurationSerializer {

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
            return this;
        }

        @Override
        public void serialize(Duration duration, JsonGenerator generator, SerializerProvider provider)
                throws IOException {
            generator.writeNumber(duration.getSeconds());
        }
    }
}
