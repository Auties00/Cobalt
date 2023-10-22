package it.auties.whatsapp.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

public class MediaUtils {
    public static byte[] readBytes(String url) {
        try(var stream = URI.create(url).toURL().openStream()) {
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot download media", exception);
        }
    }
}