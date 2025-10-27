package com.github.auties00.cobalt.sync.model;

import com.github.auties00.cobalt.model.proto.sync.ActionValueSync;
import com.github.auties00.cobalt.model.proto.sync.RecordSync;

public sealed interface DecryptedMutation {
    String index();
    RecordSync.Operation operation();
    long timestamp();

    /**
     * Represents a decrypted mutation.
     *
     * @param index     the mutation index string (e.g., "archive:chatId")
     * @param indexMac  the index MAC (32 bytes)
     * @param valueMac  the value MAC (32 bytes)
     * @param value     the decrypted action value
     * @param operation the operation type (SET or REMOVE)
     * @param timestamp the mutation timestamp (Unix micros)
     */
    record Untrusted(
            String index,
            byte[] indexMac,
            byte[] valueMac,
            ActionValueSync value,
            RecordSync.Operation operation,
            long timestamp
    ) implements DecryptedMutation {

    }

    /**
     * Represents a decrypted and validated mutation.
     *
     * @param index     the mutation index string (e.g., "archive:chatId")
     * @param value     the decrypted action value
     * @param operation the operation type (SET or REMOVE)
     * @param timestamp the mutation timestamp (Unix micros)
     */
    record Trusted(
            String index,
            ActionValueSync value,
            RecordSync.Operation operation,
            long timestamp
    ) implements DecryptedMutation {

    }
}
