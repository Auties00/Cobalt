package it.auties.whatsapp.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.auties.map.SimpleMapModule;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.fasterxml.jackson.annotation.PropertyAccessor.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_INDEX;
import static java.lang.System.Logger.Level.ERROR;

@UtilityClass
public class Smile {
    private final ObjectMapper smile;

    static {
        try {
            smile = new SmileMapper()
                    .registerModule(new Jdk8Module())
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
            var logger = System.getLogger("Smile");
            logger.log(ERROR, "An exception occurred while initializing smile", throwable);
            throw new RuntimeException("Cannot initialize smile", throwable);
        }
    }

    public byte[] writeValueAsBytes(Object object){
        try {
            return smile.writeValueAsBytes(object);
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot write smile", exception);
        }
    }

    public void writeValueAsBytes(OutputStream outputStream, Object object){
        try {
            smile.writeValue(outputStream, object);
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot write smile", exception);
        }
    }

    public <T> T readValue(byte[] value, Class<T> clazz){
        try {
            return smile.readValue(value, clazz);
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot read smile", exception);
        }
    }

    public <T> T readValue(InputStream inputStream, Class<T> clazz){
        try {
            return smile.readValue(inputStream, clazz);
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot read smile", exception);
        }
    }

    public <T> T readValue(byte[] value, TypeReference<T> clazz){
        try {
            return smile.readValue(value, clazz);
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot read smile", exception);
        }
    }

    public <T> T readValue(InputStream inputStream, TypeReference<T> clazz){
        try {
            return smile.readValue(inputStream, clazz);
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot read smile", exception);
        }
    }
}
