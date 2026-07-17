package com.github.auties00.cobalt.message.receive;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.stanza.model.Stanza;

/**
 * Single entry point for every inbound {@code <message>} stanza, turning each one into the
 * typed {@link LinkedMessageInfo} that matches the stanza's address class.
 *
 * <p>Channels posts (a {@code from} JID on the {@code @newsletter} server) become a
 * {@link NewsletterMessageInfo} read straight from the {@code <plaintext>} child; every other
 * address class (1:1, group, broadcast, status, peer) goes through the Signal-protocol
 * decryption pipeline and becomes a {@link ChatMessageInfo}. The
 * {@link LinkedWhatsAppClient} drives this service from the socket layer; direct use is reserved
 * for unit tests and custom dispatchers.
 *
 * @implSpec
 * Implementations must be thread-safe and must deduplicate the same in-flight delivery so a
 * server fanout that duplicates a message during an offline-to-online transition is processed
 * once.
 */
public interface MessageReceivingService {
    /**
     * Routes and processes an incoming {@code <message>} stanza into the appropriate
     * {@link LinkedMessageInfo} subtype.
     *
     * <p>Returns a {@link NewsletterMessageInfo} for Channels posts and a
     * {@link ChatMessageInfo} for every other message class. Returns {@code null} for
     * unavailable fanout placeholders that should be silently acknowledged and for
     * duplicate deliveries already in flight; callers must treat both as no-ops.
     *
     * @implSpec
     * Implementations must dispatch newsletter-server stanzas to the plaintext path and every
     * other address class to the decryption path, and must return {@code null} for both
     * unavailable fanout placeholders and duplicate in-flight deliveries.
     *
     * @param stanza the raw incoming {@code <message>} stanza; must be non-{@code null}
     * @return the processed message info, or {@code null} when the stanza is dropped
     * @throws NullPointerException             if {@code stanza} is {@code null}
     * @throws WhatsAppMessageException.Receive if decryption or validation fails for
     *                                          an E2E message
     */
    LinkedMessageInfo process(Stanza stanza);

    /**
     * Clears the pending-message dedup cache.
     *
     * <p>Called when the offline-delivery phase ends so messages re-delivered in a new
     * session are not mistakenly flagged as duplicates.
     *
     * @implSpec
     * Implementations must drop every tracked in-flight key so a subsequent delivery of any
     * previously-seen message id is processed rather than skipped.
     */
    void clearPendingMessages();
}
