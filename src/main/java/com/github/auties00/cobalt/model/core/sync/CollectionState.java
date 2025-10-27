package com.github.auties00.cobalt.model.core.sync;

/**
 * Represents the synchronization state of a collection.
 *
 * <p>Collections transition through these states during the sync lifecycle:
 * <pre>
 * UpToDate → Dirty → InFlight → Success → UpToDate
 *                                    ↓
 *                                 Pending
 * </pre>
 *
 * <p>Error states:
 * <ul>
 *   <li>{@link #BLOCKED} - Missing encryption keys</li>
 *   <li>{@link #ERROR_RETRY} - Transient errors (will retry)</li>
 *   <li>{@link #ERROR_FATAL} - Permanent errors (requires intervention)</li>
 * </ul>
 */
public enum CollectionState {
    /**
     * Collection is synchronized with the server.
     * Local version matches server version.
     */
    UP_TO_DATE,

    /**
     * Collection has local changes or server has newer version.
     * Needs to be synchronized.
     */
    DIRTY,

    /**
     * Sync request sent, waiting for server response.
     */
    IN_FLIGHT,

    /**
     * More data available from server.
     * Need to perform another sync to fetch remaining data.
     */
    PENDING,

    /**
     * Cannot sync due to missing encryption keys.
     * Will automatically resume when keys become available.
     */
    BLOCKED,

    /**
     * Transient error occurred (e.g., network timeout).
     * Will retry with exponential backoff.
     */
    ERROR_RETRY,

    /**
     * Fatal error occurred (e.g., decryption failure, tampering detected).
     * Requires manual intervention or snapshot recovery.
     */
    ERROR_FATAL
}
