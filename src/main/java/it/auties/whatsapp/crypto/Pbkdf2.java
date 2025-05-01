package it.auties.whatsapp.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Pbkdf2 {
    private static final String ALGORITHM = "HMACSHA1";

    public static byte[] hmacSha1With8Bit(byte[] password, byte[] salt, int iterationCount, int keySizeBits) {
        try {
            var mac = Mac.getInstance(ALGORITHM);
            var key = new SecretKeySpec(password, ALGORITHM);
            mac.init(key);
            var state = new byte[mac.getMacLength()];
            var keySize = keySizeBits / 8;
            var blocks = (keySize + mac.getMacLength() - 1) / mac.getMacLength();
            var iBuf = new byte[4];
            var result = new byte[blocks * mac.getMacLength()];
            var offset = 0;
            for(var i = 1; i <= blocks; ++i) {
                var pos = 3;
                while (++iBuf[pos] == 0) {
                    --pos;
                }

                if (salt != null) {
                    mac.update(salt, 0, salt.length);
                }

                mac.update(iBuf, 0, iBuf.length);
                mac.doFinal(state, 0);
                System.arraycopy(state, 0, result, offset, state.length);

                for(var count = 1; count < iterationCount; ++count) {
                    mac.update(state, 0, state.length);
                    mac.doFinal(state, 0);

                    for(var j = 0; j != state.length; ++j) {
                        result[offset + j] ^= state[j];
                    }
                }

                offset += mac.getMacLength();
            }

            return Arrays.copyOf(result, keySize);
        }catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("Missing HMACSHA1 implementation", exception);
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException(exception);
        }
    }
}