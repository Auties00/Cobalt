package it.auties.whatsapp.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;
import java.io.UncheckedIOException;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.fasterxml.jackson.annotation.PropertyAccessor.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static java.lang.System.Logger.Level.ERROR;

public final class Json {
    private static final ObjectMapper json;

    static {
        try {
            json = new ObjectMapper().registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule())
                    .registerModule(new ParameterNamesModule())
                    .setSerializationInclusion(NON_DEFAULT)
                    .enable(FAIL_ON_EMPTY_BEANS)
                    .enable(ACCEPT_SINGLE_VALUE_AS_ARRAY)
                    .disable(FAIL_ON_UNKNOWN_PROPERTIES)
                    .setVisibility(ALL, ANY)
                    .setVisibility(GETTER, NONE)
                    .setVisibility(IS_GETTER, NONE);
        } catch (Throwable throwable) {
            var logger = System.getLogger("Json");
            logger.log(ERROR, "An exception occurred while initializing json", throwable);
            throw new RuntimeException("Cannot initialize json", throwable);
        }
    }

    public static byte[] writeValueAsBytes(Object object) {
        try {
            return json.writer().writeValueAsBytes(object);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot write json", exception);
        }
    }

    public static String writeValueAsString(Object object) {
        return writeValueAsString(object, false);
    }

    public static String writeValueAsString(Object object, boolean pretty) {
        try {
            var writer = pretty ? json.writerWithDefaultPrettyPrinter() : json.writer();
            return writer.writeValueAsString(object);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot write json", exception);
        }
    }

    public static <T> T readValue(String value, Class<T> clazz) {
        try {
            return json.readValue(value, clazz);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read json", exception);
        }
    }

    public static <T> T readValue(String value, TypeReference<T> clazz) {
        try {
            return json.readValue(value, clazz);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read json", exception);
        }
    }
}
