package it.auties.whatsapp.crypto;

import static it.auties.bytes.Bytes.ofRandom;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.util.Validate;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AesCbc {

  private final String AES_CBC = "AES/CBC/PKCS5Padding";
  private final String AES = "AES";
  private final int AES_BLOCK_SIZE = 16;

  public byte[] encryptAndPrefix(byte[] plaintext, byte[] key) {
    var iv = ofRandom(AES_BLOCK_SIZE);
    var encrypted = encrypt(iv.toByteArray(), plaintext, key);
    return iv.append(encrypted)
        .toByteArray();
  }

  @SneakyThrows
  public byte[] encrypt(byte[] iv, byte[] plaintext, byte[] key) {
    var cipher = Cipher.getInstance(AES_CBC);
    var keySpec = new SecretKeySpec(key, AES);
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
    return cipher.doFinal(plaintext);
  }

  public byte[] decrypt(byte[] encrypted, byte[] key) {
    var binary = Bytes.of(encrypted);
    var iv = binary.cut(16)
        .toByteArray();
    var encryptedNoIv = binary.slice(16)
        .toByteArray();
    return decrypt(iv, encryptedNoIv, key);
  }

  @SneakyThrows
  public byte[] decrypt(byte[] iv, byte[] encrypted, byte[] key) {
    Validate.isTrue(iv.length == AES_BLOCK_SIZE, "Invalid iv size: expected %s, got %s",
        AES_BLOCK_SIZE, iv.length);
    Validate.isTrue(encrypted.length % AES_BLOCK_SIZE == 0, "Invalid encrypted size");
    var cipher = Cipher.getInstance(AES_CBC);
    var keySpec = new SecretKeySpec(key, AES);
    cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
    return cipher.doFinal(encrypted);
  }
}
