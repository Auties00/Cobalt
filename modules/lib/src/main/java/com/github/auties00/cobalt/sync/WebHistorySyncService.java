package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncNotification;
import com.github.auties00.cobalt.wire.linked.sync.history.HistorySync;

/**
 * Pulls every {@link HistorySyncNotification} the primary device sends into a decoded
 * {@link HistorySync} payload, fans the result out to the registered
 * {@code LinkedWhatsAppClientListener}s, and ingests the phone-number to LID mappings carried with each
 * chunk.
 *
 * <p>The service is driven by the message receiver every time a {@link HistorySyncNotification}
 * arrives; it is not called directly. To consume the decoded chunks, callers register a
 * {@code LinkedWhatsAppClientListener} and override {@code onWebHistorySyncMessages},
 * {@code onWebHistorySyncProgress}, {@code onWebHistorySyncPastParticipants}, {@code onChats},
 * {@code onContacts}, or {@code onStatus}.
 *
 * @implSpec
 * Implementations must process each notification off the stanza dispatch loop so a multi-megabyte
 * CDN download cannot block inbound stanza handling.
 */
public interface WebHistorySyncService {
    /**
     * Hands a {@link HistorySyncNotification} off for asynchronous processing.
     *
     * <p>Called by the message receiver immediately after a
     * {@link HistorySyncNotification} is decoded. The download, decryption, and decoding happen
     * asynchronously so the stanza dispatcher thread is not blocked on a CDN round-trip. Passing
     * {@code null} is a no-op.
     *
     * @implSpec
     * Implementations must accept and ignore a {@code null} notification and must not block the
     * caller on the download.
     *
     * @param notification the notification to process; {@code null} is
     *                     accepted and ignored
     */
    void process(HistorySyncNotification notification);
}
