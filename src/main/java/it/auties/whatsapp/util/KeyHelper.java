package it.auties.whatsapp.util;

import it.auties.bytes.Bytes;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class KeyHelper {

  private final String SHA_PRNG = "SHA1PRNG";

  public byte[] withHeader(byte[] key) {
    if (key == null) {
      return null;
    }
    return switch (key.length) {
      case 33 -> key;
      case 32 -> writeKeyHeader(key);
      default -> throw new IllegalArgumentException("Invalid key size: %s".formatted(key.length));
    };
  }

  private byte[] writeKeyHeader(byte[] key) {
    Validate.isTrue(key.length == 32, "Invalid key size: %s", key.length);
    var result = new byte[33];
    System.arraycopy(key, 0, result, 1, key.length);
    result[0] = 5;
    return result;
  }

  public byte[] withoutHeader(byte[] key) {
    if (key == null) {
      return null;
    }
    return switch (key.length) {
      case 32 -> key;
      case 33 -> Bytes.of(key)
          .slice(1)
          .toByteArray();
      default -> throw new IllegalArgumentException("Invalid key size: %s".formatted(key.length));
    };
  }

  @SneakyThrows
  public int header() {
    var key = new byte[1];
    var secureRandom = SecureRandom.getInstance(SHA_PRNG);
    secureRandom.nextBytes(key);
    return 1 + (15 & key[0]);
  }

  @SneakyThrows
  public int registrationId() {
    var secureRandom = SecureRandom.getInstance(SHA_PRNG);
    return secureRandom.nextInt(16380) + 1;
  }

  public String identityId(){
    return Bytes.ofRandom(20).toHex();
  }

  public String deviceId(){
    return Base64.getUrlEncoder().encodeToString(Bytes.ofRandom(16).toByteArray());
  }

  public String phoneId(){
    return UUID.randomUUID().toString();
  }

  @SneakyThrows
  public byte[] senderKey() {
    var key = new byte[32];
    var secureRandom = SecureRandom.getInstance(SHA_PRNG);
    secureRandom.nextBytes(key);
    return key;
  }

  @SneakyThrows
  public int senderKeyId() {
    var secureRandom = SecureRandom.getInstance(SHA_PRNG);
    return secureRandom.nextInt(0, 2147483647);
  }
}