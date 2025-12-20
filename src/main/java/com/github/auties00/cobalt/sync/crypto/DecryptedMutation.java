package com.github.auties00.cobalt.sync.crypto;

import com.github.auties00.cobalt.exception.WebAppStateFatalSyncException;
import com.github.auties00.cobalt.model.sync.ActionDataSyncSpec;
import com.github.auties00.cobalt.model.sync.ActionValueSync;
import com.github.auties00.cobalt.model.sync.RecordSync;
import it.auties.protobuf.stream.ProtobufInputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;

public sealed interface DecryptedMutation {
    String index();
    RecordSync.Operation operation();
    long timestamp();

    record Untrusted(
            String index,
            byte[] indexMac,
            byte[] valueMac,
            ActionValueSync value,
            RecordSync.Operation operation,
            long timestamp
    ) implements DecryptedMutation {
        private static final int IV_LENGTH = 16;
        private static final int MAC_LENGTH = 32;
        private static final byte[] VERSION = {0x00, 0x00, 0x00, 0x02};

        public static Untrusted of(
                byte[] encryptedValue,
                byte[] indexMac,
                MutationKeys keys,
                RecordSync.Operation operation
        ) throws GeneralSecurityException {
            if (encryptedValue.length < IV_LENGTH + MAC_LENGTH) {
                throw new IllegalArgumentException("Encrypted value too short");
            }

            // 1. Extract value MAC
            var valueMac = Arrays.copyOfRange(encryptedValue, encryptedValue.length - 32, encryptedValue.length);

            // 2. Verify value MAC
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(keys.valueMacKey());
            mac.update(operation.content());
            mac.update(VERSION);
            mac.update(encryptedValue, 0, IV_LENGTH);
            mac.update(encryptedValue, IV_LENGTH, encryptedValue.length - IV_LENGTH - MAC_LENGTH);
            var expectedMac = mac.doFinal();
            if (!MessageDigest.isEqual(valueMac, expectedMac)) {
                throw new WebAppStateFatalSyncException("Value MAC mismatch");
            }

            // 3. Decrypt payload with AES-256-CBC and decode protobuf
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var ivSpec = new IvParameterSpec(encryptedValue, 0, IV_LENGTH);
            cipher.init(Cipher.DECRYPT_MODE, keys.valueEncryptionKey(), ivSpec);
            var ciphertextStream = new ByteArrayInputStream(encryptedValue, IV_LENGTH, encryptedValue.length - IV_LENGTH - MAC_LENGTH);
            var plaintextStream = new CipherInputStream(ciphertextStream, cipher);
            var actionData = ActionDataSyncSpec.decode(ProtobufInputStream.fromStream(plaintextStream));

            // 4. Verify index MAC
            mac.init(keys.indexKey());
            var expectedIndexMac = mac.doFinal(actionData.index());
            if (!MessageDigest.isEqual(indexMac, expectedIndexMac)) {
                throw new WebAppStateFatalSyncException("Index MAC mismatch");
            }

            // 5. Build mutation
            return new Untrusted(
                    new String(actionData.index(), StandardCharsets.UTF_8),
                    indexMac,
                    valueMac,
                    actionData.value(),
                    operation,
                    actionData.value().timestamp()
            );
        }
    }

    record Trusted(
            String index,
            ActionValueSync value,
            RecordSync.Operation operation,
            long timestamp
    ) implements DecryptedMutation {

    }
}
