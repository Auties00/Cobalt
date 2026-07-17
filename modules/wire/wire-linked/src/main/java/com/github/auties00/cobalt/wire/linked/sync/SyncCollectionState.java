package com.github.auties00.cobalt.wire.linked.sync;

import it.auties.protobuf.annotation.ProtobufEnum;

/**
 * State in the lifecycle of an app state sync collection on the local
 * device.
 *
 * <p>Every collection transitions through these states as sync requests
 * are issued, acknowledged, or retried. Under normal operation the cycle
 * is:
 * <pre>
 * UP_TO_DATE &gt; DIRTY &gt; IN_FLIGHT &gt; UP_TO_DATE
 *                                    |
 *                                    v
 *                                 PENDING (more data available)
 * </pre>
 *
 * <p>In addition to the normal path, the collection may enter one of the
 * error states:
 * <ul>
 *   <li>{@link #BLOCKED} when encryption keys are missing</li>
 *   <li>{@link #ERROR_RETRY} for transient errors that trigger a retry</li>
 *   <li>{@link #ERROR_FATAL} for permanent errors that require intervention
 *       or a snapshot recovery</li>
 * </ul>
 */
@ProtobufEnum
public enum SyncCollectionState {
    /**
     * Collection is fully synchronised with the server: the local version
     * matches the server version and no pending mutations remain.
     */
    UP_TO_DATE,

    /**
     * Collection has local changes to push or the server is known to have
     * a newer version that needs to be pulled.
     */
    DIRTY,

    /**
     * A sync request has been sent to the server and the local side is
     * waiting for the response.
     */
    IN_FLIGHT,

    /**
     * The server indicated that more data is available and another sync
     * round is required to fetch the remaining patches.
     */
    PENDING,

    /**
     * Sync cannot proceed because the required encryption keys are missing
     * locally; sync resumes automatically when the keys become available.
     */
    BLOCKED,

    /**
     * A transient error has occurred (such as a network timeout) and sync
     * will be retried with exponential backoff.
     */
    ERROR_RETRY,

    /**
     * A fatal error has occurred (such as a decryption failure or a
     * detected tampering condition) and sync cannot proceed without manual
     * intervention or a fresh snapshot download.
     */
    ERROR_FATAL
}
