package com.github.auties00.cobalt.sync.crypto;

import com.github.auties00.cobalt.model.proto.sync.ActionDataSyncBuilder;
import com.github.auties00.cobalt.model.proto.sync.ActionDataSyncSpec;
import com.github.auties00.cobalt.model.sync.PendingMutation;
import com.github.auties00.cobalt.model.sync.RecordSync.Operation;
import com.github.auties00.cobalt.util.SecureBytes;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public record EncryptedMutation(
        byte[] indexMac,
        byte[] encryptedValue,
        byte[] keyId,
        Operation operation
) {
    private static final int IV_LENGTH = 16;
    private static final int MAC_LENGTH = 32;
    private static final int MAX_PADDING_LENGTH = 64;
    private static final byte[] VERSION = {0x00, 0x00, 0x00, 0x02};

    public static EncryptedMutation of(
            PendingMutation patch,
            MutationKeys keys,
            byte[] keyId
    ) throws GeneralSecurityException {
        // 1. Create ActionDataSync
        var padding = SecureBytes.random(1, MAX_PADDING_LENGTH + 1);
        var mutation = patch.mutation();
        var actionVersion = mutation.value()
                .version()
                .orElseThrow(() -> new IllegalArgumentException("Sync version must be present"));
        var actionData = new ActionDataSyncBuilder()
                .index(patch.mutation().index().getBytes(StandardCharsets.UTF_8))
                .value(mutation.value())
                .padding(padding)
                .version(actionVersion)
                .build();

        // 2. Encode to protobuf
        var plaintext = ActionDataSyncSpec.encode(actionData);

        // 3. Encrypt with AES-256-CBC
        var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        var ciphertextLength = cipher.getOutputSize(plaintext.length);
        var encryptedValue = new byte[IV_LENGTH + ciphertextLength +  MAC_LENGTH];
        SecureBytes.random(encryptedValue, 0, IV_LENGTH);
        var ivSpec = new IvParameterSpec(encryptedValue, 0, IV_LENGTH);
        cipher.init(Cipher.ENCRYPT_MODE, keys.valueEncryptionKey(), ivSpec);
        if(cipher.doFinal(plaintext, 0, plaintext.length, encryptedValue, IV_LENGTH) != ciphertextLength) {
            throw new InternalError("Ciphertext length mismatch");
        }

        // 4. Compute value MAC
        var mac = Mac.getInstance("HmacSHA256");
        var operation = mutation.operation().content();
        mac.init(keys.valueMacKey());
        mac.update(operation);
        mac.update(VERSION);
        mac.update(encryptedValue, 0, IV_LENGTH);
        mac.update(encryptedValue, IV_LENGTH, ciphertextLength);
        mac.doFinal(encryptedValue, IV_LENGTH + ciphertextLength);

        // 5. Compute index MAC
        var indexBytes = mutation.index().getBytes(StandardCharsets.UTF_8);
        mac.init(keys.indexKey());
        var indexMac = mac.doFinal(indexBytes);

        // 8. Create EncryptedMutation
        return new EncryptedMutation(indexMac, encryptedValue, keyId, mutation.operation());
    }
}
