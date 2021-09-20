package it.auties.whatsapp4j.test;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.net.URL;

@UtilityClass
public class MediaUtils {
    public byte[] readBytes(String url) throws IOException {
        return new URL(url).openStream().readAllBytes();
    }
}
