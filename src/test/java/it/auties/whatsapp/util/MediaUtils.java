package it.auties.whatsapp.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;

public class MediaUtils {
    public static ByteBuffer readBytes(String url) {
        try(var stream = URI.create(url).toURL().openStream()) {
            return ByteBuffer.wrap(stream.readAllBytes());
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot download media", exception);
        }
    }
}