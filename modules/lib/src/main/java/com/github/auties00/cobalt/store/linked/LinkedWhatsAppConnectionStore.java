package com.github.auties00.cobalt.store.linked;

import com.github.auties00.cobalt.client.WhatsAppClientProxy;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientOfflineResumeState;

import java.util.Optional;

/**
 * The connection-runtime state of a WhatsApp client session.
 *
 * <p>This is the sub-store that owns the transient state of the live socket connection rather than any
 * persisted domain: the optional outbound proxy, the offline-resume state machine that gates initial
 * history delivery after a reconnect, and the server-routing hints (the opaque routing token and the
 * routing domain) that pin the session to a particular edge.
 *
 * <p>None of this state is persisted; it is re-established every time the client connects.
 *
 * @apiNote
 * Embedders reach this through {@link LinkedWhatsAppStore#connectionStore()}. The proxy is the only
 * member embedders commonly set; the offline-resume and routing members are driven by the client.
 *
 * @see LinkedWhatsAppStore
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface LinkedWhatsAppConnectionStore {
    /**
     * Returns the configured proxy.
     *
     * @return the proxy, or empty if none is configured
     */
    Optional<WhatsAppClientProxy> proxy();

    /**
     * Sets the proxy.
     *
     * @param proxy the proxy, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppConnectionStore setProxy(WhatsAppClientProxy proxy);

    /**
     * Returns the offline-resume state.
     *
     * @return the offline-resume state, never {@code null}
     */
    LinkedWhatsAppClientOfflineResumeState offlineResumeState();

    /**
     * Sets the offline-resume state, driving the offline-delivery latch accordingly.
     *
     * @param state the state, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppConnectionStore setOfflineResumeState(LinkedWhatsAppClientOfflineResumeState state);

    /**
     * Returns whether resume-from-restart has progressed past the initial phase.
     *
     * @return {@code true} if resume from restart is complete
     */
    boolean isResumeFromRestartComplete();

    /**
     * Blocks until offline delivery completes or a five-minute timeout elapses.
     */
    void waitForOfflineDeliveryEnd();

    /**
     * Returns the opaque server-routing token.
     *
     * @return the routing token, or empty if none is set
     */
    Optional<byte[]> routingInfo();

    /**
     * Sets the server-routing token.
     *
     * @param routingInfo the routing token, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppConnectionStore setRoutingInfo(byte[] routingInfo);

    /**
     * Returns the routing domain hint.
     *
     * @return the routing domain, or empty if none is set
     */
    Optional<String> routingDomain();

    /**
     * Sets the routing domain hint.
     *
     * @param routingDomain the routing domain, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppConnectionStore setRoutingDomain(String routingDomain);
}
