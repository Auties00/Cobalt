package it.auties.whatsapp.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import it.auties.map.EasyMapModule;
import it.auties.protobuf.annotation.ProtobufConfigurator;
import it.auties.protobuf.decoder.ProtobufDecoderConfigurator;

public interface JacksonProvider {
    /**
     * An instance of Jackson
     */
    ObjectMapper JACKSON = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new EasyMapModule())
            .enable(SerializationFeature.WRITE_ENUMS_USING_INDEX)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @ProtobufConfigurator
    class Configurator implements ProtobufDecoderConfigurator, JacksonProvider {
        @Override
        public ObjectMapper createMapper() {
            return JACKSON;
        }
    }
}
