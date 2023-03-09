package it.auties.whatsapp.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.auties.map.SimpleMapModule;
import lombok.experimental.UtilityClass;

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

@UtilityClass
public class Json {
    private final ObjectMapper JSON;
    static {
        try {
            JSON = new ObjectMapper().registerModule(new Jdk8Module())
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
            var logger = System.getLogger("Json");
            logger.log(ERROR, "An exception occurred while initializing json", throwable);
            throw new RuntimeException("Cannot initialize json", throwable);
        }
    }

    public String writeValueAsString(Object object){
        return writeValueAsString(object, false);
    }

    public String writeValueAsString(Object object, boolean pretty){
        try {
            var writer = pretty ? JSON.writerWithDefaultPrettyPrinter() : JSON.writer();
            return writer.writeValueAsString(object);
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot write json", exception);
        }
    }

    public <T> T readValue(String value, Class<T> clazz){
        try {
            return JSON.readValue(value, clazz);
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot read json", exception);
        }
    }

    public <T> T readValue(String value, TypeReference<T> clazz){
        try {
            return JSON.readValue(value, clazz);
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot read json", exception);
        }
    }
}
