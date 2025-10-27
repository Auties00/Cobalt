package com.github.auties00.cobalt.model.core.sync;

import com.alibaba.fastjson2.JSONArray;
import com.github.auties00.cobalt.model.proto.sync.ActionValueSync;
import com.github.auties00.cobalt.model.proto.sync.RecordSync;

import java.util.Collections;

/**
 * Represents a pending mutation that hasn't been synced to the server yet.
 *
 * <p>Pending mutations are queued locally and sent to the server during the next sync cycle.
 *
 * @param mutation     the mutation to be synced
 * @param attemptCount the number of sync attempts made for this mutation
 */
public record PendingMutation(
        DecryptedMutation.Trusted mutation,
        int attemptCount
) {
    /**
     * Creates a new pending mutation with attempt count 0.
     *
     * @param sync      the patch to be synced
     * @param operation the operation to be performed on the patch
     * @param args      the arguments to be passed to the operation
     */
    public PendingMutation(ActionValueSync sync, RecordSync.Operation operation, String... args) {
        var array = new JSONArray(1 + args.length);
        if (sync.action().isPresent()) {
            array.add(sync.action().get().indexName());
        } else if (sync.setting().isPresent()) {
            array.add(sync.setting().get().indexName());
        } else {
            throw new IllegalArgumentException("Invalid sync: expected an action or setting");
        }
        Collections.addAll(array, args);
        var mutation = new DecryptedMutation.Trusted(array.toJSONString(), sync, operation, System.currentTimeMillis());
        this(mutation, 0);
    }

    /**
     * Creates a copy of this mutation with incremented attempt count.
     *
     * @return a new pending mutation with attempt count + 1
     */
    public PendingMutation incrementAttempt() {
        return new PendingMutation(mutation, attemptCount + 1);
    }
}
