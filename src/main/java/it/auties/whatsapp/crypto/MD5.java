package it.auties.whatsapp.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MD5 {
  private final String MD5 = "MD5";

  public byte[] calculate(@NonNull String data) {
    return calculate(data.getBytes(StandardCharsets.UTF_8));
  }

  @SneakyThrows
  public byte[] calculate(byte @NonNull [] data) {
    var digest = MessageDigest.getInstance(MD5);
    digest.update(data);
    return digest.digest();
  }
}
