package it.auties.whatsapp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.auties.map.SimpleMapModule;
import it.auties.protobuf.serializer.jackson.ProtobufMapper;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.fasterxml.jackson.annotation.PropertyAccessor.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_INDEX;
import static java.lang.System.Logger.Level.ERROR;

public interface JacksonProvider {
    ProtobufMapper PROTOBUF = createProtobuf();
    ObjectMapper JSON = createJson();
    SmileMapper SMILE = createSmile();

    private static ProtobufMapper createProtobuf() {
        try {
            return (ProtobufMapper) new ProtobufMapper().enable(FAIL_ON_EMPTY_BEANS)
                    .disable(FAIL_ON_UNKNOWN_PROPERTIES)
                    .registerModule(new Jdk8Module())
                    .registerModule(new SimpleMapModule());
        } catch (Throwable throwable) {
            var logger = System.getLogger("JacksonProvider");
            logger.log(ERROR, "An exception occurred while initializing protobuf", throwable);
            throw new RuntimeException("Cannot initialize protobuf", throwable);
        }
    }

    private static ObjectMapper createJson() {
        try {
            return new ObjectMapper().registerModule(new Jdk8Module())
                    .registerModule(new SimpleMapModule())
                    .registerModule(new JavaTimeModule())
                    .setSerializationInclusion(NON_DEFAULT)
                    .enable(FAIL_ON_EMPTY_BEANS)
                    .enable(ACCEPT_SINGLE_VALUE_AS_ARRAY)
                    .disable(FAIL_ON_UNKNOWN_PROPERTIES)
                    .setVisibility(ALL, ANY)
                    .setVisibility(GETTER, NONE)
                    .setVisibility(IS_GETTER, NONE);
        } catch (Throwable throwable) {
            var logger = System.getLogger("JacksonProvider");
            logger.log(ERROR, "An exception occurred while initializing json", throwable);
            throw new RuntimeException("Cannot initialize json", throwable);
        }
    }

    private static SmileMapper createSmile() {
        try {
            return (SmileMapper) new SmileMapper().registerModule(new Jdk8Module())
                    .registerModule(new SimpleMapModule())
                    .registerModule(new JavaTimeModule())
                    .setSerializationInclusion(NON_DEFAULT)
                    .enable(WRITE_ENUMS_USING_INDEX)
                    .enable(FAIL_ON_EMPTY_BEANS)
                    .enable(ACCEPT_SINGLE_VALUE_AS_ARRAY)
                    .disable(FAIL_ON_UNKNOWN_PROPERTIES)
                    .setVisibility(ALL, ANY)
                    .setVisibility(GETTER, NONE)
                    .setVisibility(IS_GETTER, NONE);
        } catch (Throwable throwable) {
            var logger = System.getLogger("JacksonProvider");
            logger.log(ERROR, "An exception occurred while initializing smile", throwable);
            throw new RuntimeException("Cannot initialize smile", throwable);
        }
    }
}
