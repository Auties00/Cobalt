package it.auties.whatsapp.util;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.PrettifyOutputStream;
import com.dslplatform.json.runtime.Settings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class Json {
    private static final DslJson<Object> dslJson = new DslJson<>(Settings.basicSetup());

    public static byte[] writeValueAsBytes(Object object) {
        try(var outputStream = new ByteArrayOutputStream()) {
            dslJson.serialize(object, outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot write json", exception);
        }
    }

    public static String writeValueAsString(Object object) {
        return writeValueAsString(object, false);
    }

    public static String writeValueAsString(Object object, boolean pretty) {
        try(var outputStream = new ByteArrayOutputStream()) {
            dslJson.serialize(object, pretty ? new PrettifyOutputStream(outputStream) : outputStream);
            return outputStream.toString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot write json", exception);
        }
    }

    public static <T> T readValue(String value, Class<T> clazz) {
        try(var inputStream = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8))) {
            return dslJson.deserialize(clazz, inputStream);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read json", exception);
        }
    }

    public static <T> List<T> readList(String value, Class<T> clazz) {
        try(var inputStream = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8))) {
            return dslJson.deserializeList(clazz, inputStream);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read json", exception);
        }
    }
}
