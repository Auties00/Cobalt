package com.github.auties00.cobalt.store.linked.protobuf.persistent;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoSpec;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfoSpec;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.type.ByteArrayDataType;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The package-private H2 MVStore facade that backs every message accessor on {@link PersistentStore}.
 *
 * <p>The single-file store at {@code <sessionDirectory>/messages.mv} hosts three named maps:
 * <ul>
 *   <li>{@code chat_messages} keyed by {@code chatJid + 0x00 + msgId}, scanned per chat through the
 *       half-open range {@code [chatJid + 0x00, chatJid + 0x01)};</li>
 *   <li>{@code newsletter_messages} keyed by {@code newsletterJid + 0x00 + serverId(BE)}, where the
 *       big-endian {@code serverId} makes the natural key order numeric so oldest/newest are the range
 *       endpoints;</li>
 *   <li>{@code status_messages} flat, keyed by {@code msgId}, holding the global status feed.</li>
 * </ul>
 *
 * <p>Staged WAM event buffers are not stored here; they offload to the independent
 * {@link PersistentWamBuffersStore}, which owns its own file.
 *
 * <p>Every map is an {@link MVMap} of {@code byte[]} keys to {@code byte[]} protobuf payloads. Keys use
 * {@link LexByteArrayType} so they sort by the same unsigned-byte (memcmp) order the previous libmdbx
 * env relied on; values use H2's {@link ByteArrayDataType}. Reads decode straight from the
 * heap-resident value array MVStore returns, so no defensive copy is needed.
 *
 * @apiNote
 * Only {@link PersistentStore} and its {@link PersistentChat} / {@link PersistentNewsletter} subtypes
 * consume this facade. The instance is owned by the parent store and shut down through
 * {@link PersistentStore#close()} or {@link PersistentStore#delete()}.
 *
 * @implNote
 * This implementation is pure Java; it replaces the former libmdbx env (FFM bindings + dedicated
 * group-commit writer thread + MPSC queue) with a thread-safe MVStore. Writes are applied directly on
 * the caller's (virtual) thread to the concurrent {@link MVMap}; durability is asynchronous, driven by
 * MVStore's background auto-commit (so no caller blocks on {@code fsync}) and forced at well-defined
 * checkpoints by {@link #commit()} (wired into the parent store's flush cadence) and finally by
 * {@link #close()}. A hard kill outside those paths can lose at most the writes accumulated since the
 * last auto-commit, which the server-side history sync re-establishes on the next connection. Unlike
 * the libmdbx backend the streams hold no reader-table slot and the file is accessed through a
 * {@link java.nio.channels.FileChannel} rather than a memory map, so a closed store's file can be
 * unlinked even on Windows.
 *
 * @see PersistentStore
 */
final class PersistentMessageStore implements AutoCloseable {
    /**
     * The logger for {@link PersistentMessageStore}.
     */
    private static final System.Logger LOGGER = Log.get(PersistentMessageStore.class);

    /**
     * The byte separating the JID prefix from the message-id suffix in composite keys.
     *
     * @implNote
     * This implementation relies on {@code 0x00} being absent from both JID string forms and
     * base64-shaped message ids, so the separator is unambiguous.
     */
    private static final byte KEY_SEPARATOR = 0x00;

    /**
     * The sentinel byte that terminates a per-JID range.
     *
     * @implNote
     * This implementation uses {@code 0x01} (one greater than {@link #KEY_SEPARATOR}) so the half-open
     * range {@code [jid + 0x00, jid + 0x01)} captures every key whose prefix matches the JID exactly
     * without spilling into the next JID's keyspace.
     */
    private static final byte KEY_RANGE_END = 0x01;

    /**
     * The name of the chat-message map.
     */
    private static final String CHAT_MESSAGES = "chat_messages";

    /**
     * The name of the newsletter-message map.
     */
    private static final String NEWSLETTER_MESSAGES = "newsletter_messages";

    /**
     * The name of the status-feed map.
     */
    private static final String STATUS_MESSAGES = "status_messages";

    /**
     * The MVStore page-cache size, in megabytes.
     *
     * @implNote
     * This implementation pins the cache to 16 MB (MVStore's own default), a deliberate ceiling that
     * bounds the resident footprint of a large message history rather than letting it grow with the
     * working set.
     */
    private static final int CACHE_SIZE_MB = 16;

    /**
     * The amount of unwritten change, in kilobytes, MVStore buffers before its background writer forces
     * a commit.
     *
     * @implNote
     * This implementation sets 4 MB, four times MVStore's default, so an offline catch-up's burst of
     * message writes coalesces into fewer background commits; the trade is a larger in-memory write
     * buffer and a wider window of data at risk between auto-commits, both bounded and re-synced from
     * the server on reconnect.
     */
    private static final int AUTO_COMMIT_BUFFER_KB = 4096;

    /**
     * The underlying single-file store; owns the file handle and the background auto-commit writer.
     */
    private final MVStore store;

    /**
     * The {@value #CHAT_MESSAGES} map.
     */
    private final MVMap<byte[], byte[]> chatMessages;

    /**
     * The {@value #NEWSLETTER_MESSAGES} map.
     */
    private final MVMap<byte[], byte[]> newsletterMessages;

    /**
     * The {@value #STATUS_MESSAGES} map.
     */
    private final MVMap<byte[], byte[]> statusMessages;

    /**
     * Constructs a facade around an already-opened store and its three maps.
     *
     * @apiNote
     * This constructor is private; instances are produced through {@link #open(Path, long)}.
     *
     * @param store              the opened MVStore
     * @param chatMessages       the chat-message map
     * @param newsletterMessages the newsletter-message map
     * @param statusMessages     the status-feed map
     */
    private PersistentMessageStore(MVStore store, MVMap<byte[], byte[]> chatMessages, MVMap<byte[], byte[]> newsletterMessages, MVMap<byte[], byte[]> statusMessages) {
        this.store = store;
        this.chatMessages = chatMessages;
        this.newsletterMessages = newsletterMessages;
        this.statusMessages = statusMessages;
    }

    /**
     * Opens or creates the MVStore at {@code file} and returns a facade ready to serve chat, newsletter
     * and status reads and writes.
     *
     * @apiNote
     * Invoked by {@link PersistentLinkedWhatsAppStoreFactory} after the metadata snapshot is loaded or a
     * fresh store is built. The parent directory is created if it does not already exist.
     *
     * @implNote
     * This implementation opens the store with a fixed cache and auto-commit buffer, then opens the
     * three named maps with the unsigned-lexicographic {@link LexByteArrayType} key type and H2's
     * {@link ByteArrayDataType} value type. The {@code initialMapSize} argument is accepted for
     * source-compatibility with the factory but ignored: MVStore grows the file on demand and never
     * preallocates.
     *
     * @param file           the {@code messages.mv} file; its parent directory is created if absent
     * @param initialMapSize the former libmdbx upper bound, ignored by this backend
     * @return a fully initialised facade
     * @throws IOException if the parent directory cannot be created or the store cannot be opened
     */
    static PersistentMessageStore open(Path file, long initialMapSize) throws IOException {
        var parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        MVStore store;
        try {
            store = new MVStore.Builder()
                    .fileName(file.toString())
                    .cacheSize(CACHE_SIZE_MB)
                    .autoCommitBufferSize(AUTO_COMMIT_BUFFER_KB)
                    .open();
        } catch (RuntimeException error) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to open mvstore at " + file, error);
            throw new IOException("Could not open MVStore at " + file, error);
        }
        try {
            var chat = openMap(store, CHAT_MESSAGES);
            var newsletter = openMap(store, NEWSLETTER_MESSAGES);
            var status = openMap(store, STATUS_MESSAGES);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "opened message store at {0}", file);
            return new PersistentMessageStore(store, chat, newsletter, status);
        } catch (RuntimeException error) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to open message maps at " + file, error);
            store.closeImmediately();
            throw new IOException("Could not open message maps at " + file, error);
        }
    }

    /**
     * Opens one named map with the message key and value types.
     *
     * @apiNote
     * Internal helper for {@link #open(Path, long)}.
     *
     * @param store the opened store
     * @param name  the map name
     * @return the opened map
     */
    private static MVMap<byte[], byte[]> openMap(MVStore store, String name) {
        return store.openMap(name, new MVMap.Builder<byte[], byte[]>()
                .keyType(LexByteArrayType.INSTANCE)
                .valueType(ByteArrayDataType.INSTANCE));
    }

    /**
     * Inserts or replaces a chat message in the {@value #CHAT_MESSAGES} map.
     *
     * @apiNote
     * Called from {@link PersistentChat#addMessage(ChatMessageInfo)}. A message with no key id is
     * silently dropped because the key requires one.
     *
     * @param chatJid the JID identifying the owning chat
     * @param info    the message to persist
     */
    void putChatMessage(Jid chatJid, ChatMessageInfo info) {
        var msgId = info.key().id().orElse(null);
        if (msgId == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "dropping chat message with no key id for chat {0}", chatJid);
            return;
        }
        chatMessages.put(encodePrefixedKey(chatJid, msgId.getBytes(StandardCharsets.UTF_8)), encodeChat(info));
    }

    /**
     * Returns the chat message under the composite key {@code chatJid + 0x00 + msgId}, or empty.
     *
     * @apiNote
     * Called from {@link PersistentChat#getMessageById(String)}.
     *
     * @param chatJid the JID identifying the owning chat
     * @param msgId   the message key id
     * @return the decoded message, or empty if no entry exists
     */
    Optional<ChatMessageInfo> getChatMessage(Jid chatJid, String msgId) {
        var value = chatMessages.get(encodePrefixedKey(chatJid, msgId.getBytes(StandardCharsets.UTF_8)));
        return value == null ? Optional.empty() : Optional.of(decodeChat(value));
    }

    /**
     * Removes the chat message under the composite key {@code chatJid + 0x00 + msgId}.
     *
     * @apiNote
     * Called from {@link PersistentChat#removeMessage(String)}.
     *
     * @param chatJid the JID identifying the owning chat
     * @param msgId   the message key id
     * @return {@code true} if an entry was removed, {@code false} if no such entry existed
     */
    boolean removeChatMessage(Jid chatJid, String msgId) {
        return chatMessages.remove(encodePrefixedKey(chatJid, msgId.getBytes(StandardCharsets.UTF_8))) != null;
    }

    /**
     * Removes every chat message stored for the given chat.
     *
     * @apiNote
     * Called from {@link PersistentChat#removeMessages()} and from
     * {@link PersistentLinkedWhatsAppChatStore#removeChat} so a removed chat's body history does not
     * outlive its metadata.
     *
     * @param chatJid the JID identifying the owning chat
     * @return the number of entries removed
     */
    int removeChatMessages(Jid chatJid) {
        var removed = removeRange(chatMessages, chatJid);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "removed {0} chat messages for {1}", removed, chatJid);
        return removed;
    }

    /**
     * Returns the oldest chat message stored for the given chat in key order, or empty.
     *
     * @apiNote
     * Called from {@link PersistentChat#oldestMessage()}.
     *
     * @param chatJid the JID identifying the owning chat
     * @return the first message in the per-JID range, or empty
     */
    public Optional<ChatMessageInfo> oldestChatMessage(Jid chatJid) {
        return firstInRange(chatMessages, chatJid, PersistentMessageStore::decodeChat);
    }

    /**
     * Returns the newest chat message stored for the given chat in key order, or empty.
     *
     * @apiNote
     * Called from {@link PersistentChat#newestMessage()}.
     *
     * @param chatJid the JID identifying the owning chat
     * @return the last message in the per-JID range, or empty
     */
    Optional<ChatMessageInfo> newestChatMessage(Jid chatJid) {
        return lastInRange(chatMessages, chatJid, PersistentMessageStore::decodeChat);
    }

    /**
     * Returns the number of chat messages stored for the given chat.
     *
     * @apiNote
     * Called once by {@link PersistentChat#attach(PersistentMessageStore)} to seed the per-chat cached
     * counter; subsequent {@link PersistentChat#messageCount()} calls answer from the cache.
     *
     * @implNote
     * This implementation walks the per-JID key range and is {@code O(n)} in the chat size.
     *
     * @param chatJid the JID identifying the owning chat
     * @return the number of stored messages
     */
    int countChatMessages(Jid chatJid) {
        return countRange(chatMessages, chatJid);
    }

    /**
     * Returns a lazy stream of every chat message stored for the given chat, in key order.
     *
     * @apiNote
     * The stream walks a consistent MVStore snapshot taken when it is created. It holds no native
     * resources, but callers continue to consume it inside a try-with-resources block for parity with
     * the rest of the message API.
     *
     * @param chatJid the JID identifying the owning chat
     * @return a closeable stream of decoded chat messages
     */
    Stream<ChatMessageInfo> streamChatMessages(Jid chatJid) {
        return streamRange(chatMessages, jidRangeStart(chatJid), jidRangeStop(chatJid), PersistentMessageStore::decodeChat);
    }

    /**
     * Inserts or replaces a newsletter message in the {@value #NEWSLETTER_MESSAGES} map.
     *
     * @apiNote
     * Called from {@link PersistentNewsletter#addMessage(NewsletterMessageInfo)}. The key is
     * {@code newsletterJid + 0x00 + serverId(BE)} so the range endpoints are oldest/newest with no extra
     * index.
     *
     * @param newsletterJid the JID identifying the owning newsletter
     * @param info          the message to persist
     * @return {@code true} when a new {@code serverId} key was inserted, {@code false} when an existing entry was
     *         overwritten in place, so callers can maintain a cached count without double-counting re-puts
     */
    boolean putNewsletterMessage(Jid newsletterJid, NewsletterMessageInfo info) {
        var previous = newsletterMessages.put(encodePrefixedKey(newsletterJid, encodeServerId(info.serverId())), encodeNewsletter(info));
        return previous == null;
    }

    /**
     * Returns the newsletter message under the composite key {@code newsletterJid + 0x00 + serverId(BE)},
     * or empty.
     *
     * @apiNote
     * Called from {@link PersistentNewsletter#getMessageById(String)} and from
     * {@link PersistentLinkedWhatsAppChatStore#findMessageById(com.github.auties00.cobalt.wire.linked.newsletter.Newsletter, String)}
     * as the fast path before the per-newsletter scan fallback.
     *
     * @param newsletterJid the JID identifying the owning newsletter
     * @param serverId      the server-assigned monotonic identifier
     * @return the decoded message, or empty if no entry exists
     */
    Optional<NewsletterMessageInfo> getNewsletterMessageByServerId(Jid newsletterJid, int serverId) {
        var value = newsletterMessages.get(encodePrefixedKey(newsletterJid, encodeServerId(serverId)));
        return value == null ? Optional.empty() : Optional.of(decodeNewsletter(value));
    }

    /**
     * Removes the newsletter message identified by its server id.
     *
     * @apiNote
     * Called from {@link PersistentNewsletter#removeMessage(String)} after the caller parses the
     * message-id string into a numeric server id.
     *
     * @param newsletterJid the JID identifying the owning newsletter
     * @param serverId      the server-assigned monotonic identifier
     * @return {@code true} if an entry was removed
     */
    boolean removeNewsletterMessageByServerId(Jid newsletterJid, int serverId) {
        return newsletterMessages.remove(encodePrefixedKey(newsletterJid, encodeServerId(serverId))) != null;
    }

    /**
     * Removes every newsletter message stored for the given newsletter.
     *
     * @apiNote
     * Called from {@link PersistentNewsletter#removeMessages()} and from
     * {@link PersistentLinkedWhatsAppChatStore#removeNewsletter} so a removed newsletter's body history
     * does not outlive its metadata.
     *
     * @param newsletterJid the JID identifying the owning newsletter
     * @return the number of entries removed
     */
    int removeNewsletterMessages(Jid newsletterJid) {
        var removed = removeRange(newsletterMessages, newsletterJid);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "removed {0} newsletter messages for {1}", removed, newsletterJid);
        return removed;
    }

    /**
     * Returns the oldest newsletter message (lowest {@code serverId}) for the given newsletter, or empty.
     *
     * @apiNote
     * Called from {@link PersistentNewsletter#oldestMessage()}.
     *
     * @implNote
     * This implementation relies on the big-endian {@code serverId} encoding so unsigned-byte key order
     * aligns with the natural numeric order.
     *
     * @param newsletterJid the JID identifying the owning newsletter
     * @return the oldest message, or empty
     */
    Optional<NewsletterMessageInfo> oldestNewsletterMessage(Jid newsletterJid) {
        return firstInRange(newsletterMessages, newsletterJid, PersistentMessageStore::decodeNewsletter);
    }

    /**
     * Returns the newest newsletter message (highest {@code serverId}) for the given newsletter, or
     * empty.
     *
     * @apiNote
     * Called from {@link PersistentNewsletter#newestMessage()}.
     *
     * @param newsletterJid the JID identifying the owning newsletter
     * @return the newest message, or empty
     */
    Optional<NewsletterMessageInfo> newestNewsletterMessage(Jid newsletterJid) {
        return lastInRange(newsletterMessages, newsletterJid, PersistentMessageStore::decodeNewsletter);
    }

    /**
     * Returns the number of newsletter messages stored for the given newsletter.
     *
     * @apiNote
     * Called once by {@link PersistentNewsletter#attach(PersistentMessageStore)} to seed the cached
     * counter.
     *
     * @implNote
     * This implementation walks the per-JID key range; it is {@code O(n)} in the newsletter size.
     *
     * @param newsletterJid the JID identifying the owning newsletter
     * @return the number of stored messages
     */
    int countNewsletterMessages(Jid newsletterJid) {
        return countRange(newsletterMessages, newsletterJid);
    }

    /**
     * Returns a lazy stream of every newsletter message stored for the given newsletter, in key order.
     *
     * @apiNote
     * The stream walks a consistent MVStore snapshot; consume inside a try-with-resources block for
     * parity with the rest of the message API.
     *
     * @param newsletterJid the JID identifying the owning newsletter
     * @return a closeable stream of decoded newsletter messages
     */
    Stream<NewsletterMessageInfo> streamNewsletterMessages(Jid newsletterJid) {
        return streamRange(newsletterMessages, jidRangeStart(newsletterJid), jidRangeStop(newsletterJid), PersistentMessageStore::decodeNewsletter);
    }

    /**
     * Inserts or replaces a status-feed message keyed by message id.
     *
     * @apiNote
     * Called from {@link PersistentLinkedWhatsAppChatStore#addStatus(ChatMessageInfo)}. A message with
     * no key id is silently dropped because the key requires one.
     *
     * @param info the message to persist
     */
    void putStatusMessage(ChatMessageInfo info) {
        var msgId = info.key().id().orElse(null);
        if (msgId == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "dropping status message with no key id");
            return;
        }
        statusMessages.put(msgId.getBytes(StandardCharsets.UTF_8), encodeChat(info));
    }

    /**
     * Returns the status-feed message under the flat key {@code msgId}, or empty.
     *
     * @apiNote
     * Called from {@link PersistentLinkedWhatsAppChatStore#findStatusById(String)} and as a
     * status-broadcast branch of {@link PersistentLinkedWhatsAppChatStore#findMessageById}.
     *
     * @param msgId the message key id
     * @return the decoded message, or empty
     */
    Optional<ChatMessageInfo> getStatusMessage(String msgId) {
        var value = statusMessages.get(msgId.getBytes(StandardCharsets.UTF_8));
        return value == null ? Optional.empty() : Optional.of(decodeChat(value));
    }

    /**
     * Removes the status-feed message under {@code msgId} and returns its previous value.
     *
     * @apiNote
     * Called from {@link PersistentLinkedWhatsAppChatStore#removeStatus(String)}.
     *
     * @implNote
     * This implementation uses {@link MVMap#remove(Object)}, which atomically returns the prior value,
     * so the read and the delete are a single map operation.
     *
     * @param msgId the message key id
     * @return the previously stored message, or empty if absent
     */
    Optional<ChatMessageInfo> removeStatusMessage(String msgId) {
        var previous = statusMessages.remove(msgId.getBytes(StandardCharsets.UTF_8));
        return previous == null ? Optional.empty() : Optional.of(decodeChat(previous));
    }

    /**
     * Returns the number of status-feed messages stored.
     *
     * @apiNote
     * No caller currently consumes this directly; retained for parity with the chat and newsletter
     * accessors.
     *
     * @implNote
     * This implementation reads {@link MVMap#sizeAsLong()}, an {@code O(1)} counter, unlike the per-JID
     * counts that walk a range.
     *
     * @return the entry count
     */
    int countStatusMessages() {
        return (int) statusMessages.sizeAsLong();
    }

    /**
     * Returns a lazy stream of every status-feed message in key order.
     *
     * @apiNote
     * Called from {@link PersistentLinkedWhatsAppChatStore#status()}; consumed inside a
     * try-with-resources block and collected into a list.
     *
     * @return a closeable stream of decoded status messages
     */
    Stream<ChatMessageInfo> streamStatusMessages() {
        return streamRange(statusMessages, null, null, PersistentMessageStore::decodeChat);
    }

    /**
     * Returns the set of distinct chat JIDs that currently have at least one message stored.
     *
     * @apiNote
     * Called by {@link PersistentLinkedWhatsAppStoreFactory} on boot to recover orphan chats whose
     * messages landed in the store but whose metadata never reached the next {@code store.proto}
     * snapshot.
     *
     * @return a freshly allocated set of chat JIDs, never {@code null}
     */
    Set<Jid> distinctChatJids() {
        return distinctJids(chatMessages);
    }

    /**
     * Returns the set of distinct newsletter JIDs that currently have at least one message stored.
     *
     * @apiNote
     * Called by {@link PersistentLinkedWhatsAppStoreFactory} on boot to recover orphan newsletters whose
     * messages landed in the store but whose metadata never reached the next snapshot.
     *
     * @return a freshly allocated set of newsletter JIDs, never {@code null}
     */
    Set<Jid> distinctNewsletterJids() {
        return distinctJids(newsletterMessages);
    }

    /**
     * Forces any buffered changes to the underlying file.
     *
     * @apiNote
     * Called by {@link PersistentStore#await()} so the message store reaches a durable checkpoint on the
     * same cadence as the metadata snapshot, including from the client's JVM shutdown hook.
     *
     * @implNote
     * This implementation delegates to {@link MVStore#commit()}, which persists the current version
     * without blocking writers; the background auto-commit otherwise covers the steady state.
     */
    void commit() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "committing message store");
        store.commit();
    }

    /**
     * Closes the underlying store, flushing all buffered changes and releasing the file handle.
     *
     * @apiNote
     * Called from {@link PersistentStore#close()} and {@link PersistentStore#delete()}. After this call
     * every accessor will fail; the parent store nulls its reference so a second close is a no-op.
     *
     * @implNote
     * This implementation delegates to {@link MVStore#close()}, which commits the current version before
     * closing. Because the file is accessed through a {@link java.nio.channels.FileChannel} rather than a
     * memory map, the file can be deleted immediately afterwards on every platform.
     */
    @Override
    public void close() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "closing message store");
        store.close();
    }

    /**
     * Removes every entry of {@code map} whose key begins with {@code jid + 0x00}.
     *
     * @apiNote
     * Internal helper for the range-delete accessors.
     *
     * @implNote
     * This implementation re-seeks the smallest remaining in-range key after each delete, an
     * {@code O(n log n)} walk that allocates no intermediate key list.
     *
     * @param map the target map
     * @param jid the JID prefix
     * @return the number of entries removed
     */
    private static int removeRange(MVMap<byte[], byte[]> map, Jid jid) {
        var start = jidRangeStart(jid);
        var stop = jidRangeStop(jid);
        var removed = 0;
        var key = map.ceilingKey(start);
        while (key != null && Arrays.compareUnsigned(key, stop) < 0) {
            map.remove(key);
            removed++;
            key = map.ceilingKey(start);
        }
        return removed;
    }

    /**
     * Returns the first decoded entry of {@code map} in the per-JID range, or empty.
     *
     * @apiNote
     * Internal helper for the oldest-message accessors.
     *
     * @param map     the target map
     * @param jid     the JID prefix
     * @param decoder how to decode the value bytes
     * @param <T>     the decoded type
     * @return the first entry, or empty
     */
    private static <T> Optional<T> firstInRange(MVMap<byte[], byte[]> map, Jid jid, Function<byte[], T> decoder) {
        var key = map.ceilingKey(jidRangeStart(jid));
        if (key == null || Arrays.compareUnsigned(key, jidRangeStop(jid)) >= 0) {
            return Optional.empty();
        }
        return Optional.of(decoder.apply(map.get(key)));
    }

    /**
     * Returns the last decoded entry of {@code map} in the per-JID range, or empty.
     *
     * @apiNote
     * Internal helper for the newest-message accessors.
     *
     * @implNote
     * This implementation seeks the greatest key strictly below the range-end sentinel
     * ({@link MVMap#lowerKey(Object)}), giving an {@code O(log n)} newest lookup, then rejects it if it
     * falls before the range start.
     *
     * @param map     the target map
     * @param jid     the JID prefix
     * @param decoder how to decode the value bytes
     * @param <T>     the decoded type
     * @return the last entry, or empty
     */
    private static <T> Optional<T> lastInRange(MVMap<byte[], byte[]> map, Jid jid, Function<byte[], T> decoder) {
        var key = map.lowerKey(jidRangeStop(jid));
        if (key == null || Arrays.compareUnsigned(key, jidRangeStart(jid)) < 0) {
            return Optional.empty();
        }
        return Optional.of(decoder.apply(map.get(key)));
    }

    /**
     * Returns the number of entries in {@code map} within the per-JID range.
     *
     * @apiNote
     * Internal helper for the per-JID count accessors.
     *
     * @implNote
     * This implementation subtracts the two range-bound insertion indices reported by
     * {@code MVMap.getKeyIndex}, an {@code O(log n)} count that avoids walking the range. The bound keys
     * ({@code jid + 0x00} and {@code jid + 0x01}) are never stored, so each lookup returns the negative
     * not-found encoding whose decoded insertion point is the first index at or after the bound; their
     * difference is the number of keys in the half-open range.
     *
     * @param map the target map
     * @param jid the JID prefix
     * @return the entry count
     */
    private static int countRange(MVMap<byte[], byte[]> map, Jid jid) {
        var start = insertionIndex(map.getKeyIndex(jidRangeStart(jid)));
        var stop = insertionIndex(map.getKeyIndex(jidRangeStop(jid)));
        return (int) (stop - start);
    }

    /**
     * Decodes the index {@code MVMap.getKeyIndex} returns into the position the key occupies, or would
     * occupy if absent.
     *
     * @apiNote
     * Internal helper for {@link #countRange(MVMap, Jid)}.
     *
     * @implNote
     * This implementation follows the binary-search convention: a non-negative result is the key's own
     * index; a negative result encodes {@code -(insertionPoint) - 1}.
     *
     * @param keyIndex the value returned by {@code MVMap.getKeyIndex}
     * @return the key's index, or the insertion point if the key is absent
     */
    private static long insertionIndex(long keyIndex) {
        return keyIndex >= 0 ? keyIndex : -(keyIndex + 1);
    }

    /**
     * Walks {@code map} once and collects the distinct JID prefixes that precede the first
     * {@link #KEY_SEPARATOR} in each key.
     *
     * @apiNote
     * Internal helper for the orphan-recovery accessors {@link #distinctChatJids()} and
     * {@link #distinctNewsletterJids()}.
     *
     * @param map the map to scan
     * @return a set of decoded JIDs
     */
    private static Set<Jid> distinctJids(MVMap<byte[], byte[]> map) {
        var jids = new HashSet<Jid>();
        var iterator = map.keyIterator(null);
        while (iterator.hasNext()) {
            var jid = jidFromKey(iterator.next());
            if (jid != null) {
                jids.add(jid);
            }
        }
        return jids;
    }

    /**
     * Wraps a key-range cursor walk into a lazy {@link Stream} of decoded values.
     *
     * @apiNote
     * Internal helper for the stream accessors. A {@code null} {@code start} and {@code stop} stream the
     * whole map (the status feed); otherwise the half-open per-JID range is streamed.
     *
     * @implNote
     * This implementation reads each value through {@link Cursor#getValue()} after advancing the cursor;
     * the cursor captures a consistent snapshot at creation, so the walk is unaffected by concurrent
     * writers and needs no close hook.
     *
     * @param map     the target map
     * @param start   the inclusive range start key, or {@code null} for the whole map
     * @param stop    the exclusive range end key, or {@code null} for the whole map
     * @param decoder how to decode each value's bytes
     * @param <T>     the decoded type
     * @return a closeable stream of decoded entries
     */
    private static <T> Stream<T> streamRange(MVMap<byte[], byte[]> map, byte[] start, byte[] stop, Function<byte[], T> decoder) {
        var cursor = start == null ? map.cursor(null) : map.cursor(start, stop, false);
        var iterator = new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return cursor.hasNext();
            }

            @Override
            public T next() {
                cursor.next();
                return decoder.apply(cursor.getValue());
            }
        };
        var spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.NONNULL);
        return StreamSupport.stream(spliterator, false);
    }

    /**
     * Encodes a chat message to a fresh protobuf byte array.
     *
     * @apiNote
     * Internal helper for the chat and status writers.
     *
     * @param info the message to encode
     * @return the serialized bytes
     */
    private static byte[] encodeChat(ChatMessageInfo info) {
        var bytes = new byte[ChatMessageInfoSpec.sizeOf(info)];
        ChatMessageInfoSpec.encode(info, ProtobufOutputStream.toBuffer(ByteBuffer.wrap(bytes)));
        return bytes;
    }

    /**
     * Encodes a newsletter message to a fresh protobuf byte array.
     *
     * @apiNote
     * Internal helper for the newsletter writer.
     *
     * @param info the message to encode
     * @return the serialized bytes
     */
    private static byte[] encodeNewsletter(NewsletterMessageInfo info) {
        var bytes = new byte[NewsletterMessageInfoSpec.sizeOf(info)];
        NewsletterMessageInfoSpec.encode(info, ProtobufOutputStream.toBuffer(ByteBuffer.wrap(bytes)));
        return bytes;
    }

    /**
     * Decodes a value's bytes into a heap-resident {@link ChatMessageInfo}.
     *
     * @apiNote
     * Internal helper for every chat and status read.
     *
     * @param bytes the stored value bytes
     * @return the decoded chat message
     */
    private static ChatMessageInfo decodeChat(byte[] bytes) {
        return ChatMessageInfoSpec.decode(ProtobufInputStream.fromBytes(bytes));
    }

    /**
     * Decodes a value's bytes into a heap-resident {@link NewsletterMessageInfo}.
     *
     * @apiNote
     * Internal helper for every newsletter read.
     *
     * @param bytes the stored value bytes
     * @return the decoded newsletter message
     */
    private static NewsletterMessageInfo decodeNewsletter(byte[] bytes) {
        return NewsletterMessageInfoSpec.decode(ProtobufInputStream.fromBytes(bytes));
    }

    /**
     * Returns the JID encoded in the prefix of a composite key, or {@code null} if the key has no
     * separator.
     *
     * @apiNote
     * Internal helper for {@link #distinctJids(MVMap)}.
     *
     * @param key the key bytes
     * @return the decoded JID, or {@code null}
     */
    private static Jid jidFromKey(byte[] key) {
        var separator = -1;
        for (var i = 0; i < key.length; i++) {
            if (key[i] == KEY_SEPARATOR) {
                separator = i;
                break;
            }
        }
        if (separator < 0) {
            return null;
        }
        return Jid.of(new String(key, 0, separator, StandardCharsets.UTF_8));
    }

    /**
     * Returns the composite key {@code jidBytes + 0x00 + suffix}.
     *
     * @apiNote
     * Internal helper for the chat and newsletter maps.
     *
     * @param jid    the JID prefix
     * @param suffix the suffix bytes (a message id or an encoded server id)
     * @return the composite key bytes
     */
    private static byte[] encodePrefixedKey(Jid jid, byte[] suffix) {
        var jidBytes = jid.toString().getBytes(StandardCharsets.UTF_8);
        var key = new byte[jidBytes.length + 1 + suffix.length];
        System.arraycopy(jidBytes, 0, key, 0, jidBytes.length);
        key[jidBytes.length] = KEY_SEPARATOR;
        System.arraycopy(suffix, 0, key, jidBytes.length + 1, suffix.length);
        return key;
    }

    /**
     * Returns the inclusive start key {@code jidBytes + 0x00} of a per-JID range.
     *
     * @apiNote
     * Internal helper for the per-JID range operations.
     *
     * @param jid the JID prefix
     * @return the range start key bytes
     */
    private static byte[] jidRangeStart(Jid jid) {
        var jidBytes = jid.toString().getBytes(StandardCharsets.UTF_8);
        var key = new byte[jidBytes.length + 1];
        System.arraycopy(jidBytes, 0, key, 0, jidBytes.length);
        key[jidBytes.length] = KEY_SEPARATOR;
        return key;
    }

    /**
     * Returns the exclusive end key {@code jidBytes + 0x01} of a per-JID range.
     *
     * @apiNote
     * Internal helper for the per-JID range operations.
     *
     * @param jid the JID prefix
     * @return the range end key bytes
     */
    private static byte[] jidRangeStop(Jid jid) {
        var jidBytes = jid.toString().getBytes(StandardCharsets.UTF_8);
        var key = new byte[jidBytes.length + 1];
        System.arraycopy(jidBytes, 0, key, 0, jidBytes.length);
        key[jidBytes.length] = KEY_RANGE_END;
        return key;
    }

    /**
     * Encodes {@code serverId} as four big-endian bytes.
     *
     * @apiNote
     * Used as the suffix of newsletter-message keys.
     *
     * @implNote
     * This implementation emits big-endian so unsigned-byte key compare yields the natural numeric
     * order; little-endian would invert the order.
     *
     * @param serverId the server-assigned monotonic identifier
     * @return a four-byte big-endian encoding
     */
    private static byte[] encodeServerId(int serverId) {
        return new byte[]{
                (byte) (serverId >>> 24),
                (byte) (serverId >>> 16),
                (byte) (serverId >>> 8),
                (byte) serverId
        };
    }
}
