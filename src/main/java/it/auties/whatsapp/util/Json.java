package it.auties.whatsapp.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.PropertyAccessor.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static java.lang.System.Logger.Level.ERROR;

public final class Json {
    private static final ObjectMapper json;
    private static final ObjectWriter prettyWriter;

    static {
        try {
            var optionalModule = new SimpleModule();
            optionalModule.addDeserializer(Optional.class, new OptionalDeserializer());
            json = new ObjectMapper()
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule())
                    .registerModule(new ParameterNamesModule())
                    .registerModule(optionalModule)
                    .enable(FAIL_ON_EMPTY_BEANS)
                    .enable(ACCEPT_SINGLE_VALUE_AS_ARRAY)
                    .disable(FAIL_ON_UNKNOWN_PROPERTIES)
                    .setVisibility(ALL, ANY)
                    .setVisibility(GETTER, NONE)
                    .setSerializationInclusion(NON_NULL)
                    .setVisibility(IS_GETTER, NONE);
            prettyWriter = json.copy()
                    .setSerializationInclusion(NON_DEFAULT)
                    .writerWithDefaultPrettyPrinter();
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

    public static void writeValueAsBytes(Object object, OutputStream outputStream) {
        try {
            json.writer().writeValue(outputStream, object);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot write json", exception);
        }
    }

    public static String writeValueAsString(Object object) {
        return writeValueAsString(object, false);
    }

    public static String writeValueAsString(Object object, boolean pretty) {
        try {
            var writer = pretty ? prettyWriter : json.writer();
            return writer.writeValueAsString(object);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot write json", exception);
        }
    }

    public static <T> T readValue(byte[] value, Class<T> clazz) {
        try {
            return json.readValue(value, clazz);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read json", exception);
        }
    }

    public static <T> T readValue(byte[] value, TypeReference<T> clazz) {
        try {
            return json.readValue(value, clazz);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read json", exception);
        }
    }

    public static <T> T readValue(String value, Class<T> clazz) {
        try {
            return json.readValue(value, clazz);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read json", exception);
        }
    }

    public static <T> T readValue(InputStream value, Class<T> clazz) {
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

    private static class OptionalDeserializer extends StdDeserializer<Optional<?>> implements ContextualDeserializer {
        private final JavaType optionalType;

        public OptionalDeserializer() {
            super(Optional.class);
            this.optionalType = null;
        }

        private OptionalDeserializer(JavaType optionalType) {
            super(Optional.class);
            this.optionalType = optionalType;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
            if (property == null) {
                var optionalType = context.getContextualType();
                var mapValueType = optionalType.containedTypeOrUnknown(0);
                return new OptionalDeserializer(mapValueType);
            }

            var optionalType = property.getType();
            var mapValueType = optionalType.containedTypeOrUnknown(0);
            return new OptionalDeserializer(mapValueType);
        }

        @Override
        public Optional<?> deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
            Objects.requireNonNull(optionalType, "Missing context");
            var node = jsonParser.getCodec().readTree(jsonParser);
            var value = jsonParser.getCodec().treeToValue(node, optionalType.getRawClass());
            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(value);
        }

        @Override
        public Optional<?> getNullValue(DeserializationContext context) {
            return Optional.empty();
        }
    }
}
