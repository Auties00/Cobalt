package it.auties.whatsapp.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MediaUtils {

  public byte[] readBytes(String url) {
    try {
      return new URL(url).openStream()
          .readAllBytes();
    } catch (IOException exception) {
      throw new UncheckedIOException("Cannot download media", exception);
    }
  }
}