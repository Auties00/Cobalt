package com.github.auties00.cobalt.store.linked;

import com.github.auties00.cobalt.listener.WhatsAppListener;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;
import com.github.auties00.cobalt.store.WhatsAppStore;

import java.io.IOException;
import java.util.Collection;

/**
 * The persistent and transient state of a WhatsApp client session.
 *
 * <p>This is the facade over everything the official WhatsApp app keeps on disk plus everything it
 * keeps in memory while the user is logged in. The state is partitioned into domain sub-stores, each
 * reached through an accessor:
 *
 * <ul>
 *   <li>{@link #signalStore()} - Signal-protocol keys, sessions, sender keys and identities.
 *   <li>{@link #accountStore()} - identity (phone number, JID, LID), device, profile, business
 *       profile, the linked-device list and the companion-pairing artefacts.
 *   <li>{@link #contactStore()} - contacts, the phone/LID mapping table, device lists and the block list.
 *   <li>{@link #chatStore()} - chats, newsletters, status, messages, calls, call history and the
 *       sent-message receipt-record buffer.
 *   <li>{@link #syncStore()} - the app-state (syncd) machinery and the AB-props feature-flag bundle.
 *   <li>{@link #settingsStore()} - privacy settings, preferences, stickers, labels and quick replies.
 *   <li>{@link #businessStore()} - WhatsApp Business and payments state.
 *   <li>{@link #webSessionStore()} - the web-GraphQL credentials backing the relay and Facebook GraphQL transports.
 *   <li>{@link #connectionStore()} - the per-connection runtime state: proxy, offline-resume and routing hints.
 *   <li>{@link #wamStore()} - the WAM telemetry sequence numbers and staged event buffers.
 * </ul>
 *
 * <p>The facade itself owns only the registered listeners; every other piece of session state belongs
 * to one of the sub-stores and is reached through it.
 *
 * @apiNote
 * Embedders never construct this directly. Instances are obtained exclusively through
 * {@link LinkedWhatsAppStoreFactory}, which chooses the backing implementation.
 *
 * @implSpec
 * Implementations control how and where session data is stored. Read methods must return defensive
 * (unmodifiable) views of any internal collection. Setters return {@code this} so they can be chained.
 *
 * @see LinkedWhatsAppStoreFactory
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public non-sealed interface LinkedWhatsAppStore extends WhatsAppStore {
    /**
     * Blocks until all asynchronous initialization operations for this store complete.
     *
     * @throws IOException if the store cannot be deserialized completely or correctly
     */
    void await() throws IOException;

    /**
     * Flushes all pending changes to the underlying storage.
     *
     * @throws IOException if the session cannot be saved
     */
    void save() throws IOException;

    /**
     * Permanently deletes this session from storage.
     *
     * @throws IOException if the session cannot be deleted
     */
    void delete() throws IOException;

    /**
     * Returns the Signal-protocol cryptographic sub-store.
     *
     * @return the signal sub-store, never {@code null}
     */
    LinkedWhatsAppSignalStore signalStore();

    /**
     * Returns the account-identity and profile sub-store.
     *
     * @return the account sub-store, never {@code null}
     */
    LinkedWhatsAppAccountStore accountStore();

    /**
     * Returns the address-book and per-peer-device sub-store.
     *
     * @return the contact sub-store, never {@code null}
     */
    LinkedWhatsAppContactStore contactStore();

    /**
     * Returns the conversation (chats, newsletters, status, calls) sub-store.
     *
     * @return the chat sub-store, never {@code null}
     */
    LinkedWhatsAppChatStore chatStore();

    /**
     * Returns the app-state-sync and feature-flag sub-store.
     *
     * @return the sync sub-store, never {@code null}
     */
    LinkedWhatsAppSyncStore syncStore();

    /**
     * Returns the user-preference and settings sub-store.
     *
     * @return the settings sub-store, never {@code null}
     */
    LinkedWhatsAppSettingsStore settingsStore();

    /**
     * Returns the WhatsApp Business and payments sub-store.
     *
     * @return the business sub-store, never {@code null}
     */
    LinkedWhatsAppBusinessStore businessStore();

    /**
     * Returns the web-GraphQL credential sub-store backing the relay and Facebook GraphQL transports.
     *
     * @return the web-session sub-store, never {@code null}
     */
    LinkedWebSessionStore webSessionStore();

    /**
     * Returns the connection-runtime sub-store holding the proxy, offline-resume and routing state.
     *
     * @return the connection sub-store, never {@code null}
     */
    LinkedWhatsAppConnectionStore connectionStore();

    /**
     * Returns the WAM telemetry sub-store holding the sequence numbers and staged event buffers.
     *
     * @return the WAM sub-store, never {@code null}
     */
    LinkedWhatsAppWamStore wamStore();

    /**
     * Registers an event listener.
     *
     * @param listener the listener; may be any {@link WhatsAppListener} subtype
     *                 (a per-event functional interface or the aggregator
     *                 {@link LinkedWhatsAppClientListener})
     * @return the registered listener
     */
    WhatsAppListener addListener(WhatsAppListener listener);

    /**
     * Unregisters an event listener.
     *
     * @param listener the listener
     * @return {@code true} if the listener was registered
     */
    boolean removeListener(WhatsAppListener listener);

    /**
     * Returns the registered event listeners.
     *
     * <p>The returned collection is heterogeneous: each element is some
     * {@link WhatsAppListener} subtype, and dispatch sites recover the
     * concrete event interface through {@code instanceof} pattern matching.
     *
     * @return an unmodifiable copy of the listeners
     */
    Collection<WhatsAppListener> listeners();
}
