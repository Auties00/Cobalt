package com.github.auties00.cobalt.store.linked.protobuf.persistent;

import com.github.auties00.cobalt.telemetry.log.Log;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.type.ByteArrayDataType;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * The package-private H2 MVStore facade that stages a persistence-variant WAM store's encoded event
 * buffers.
 *
 * <p>The single-file store at {@code <sessionDirectory>/wam.mv} hosts one named map, {@code wam_buffers},
 * flat and keyed by the UTF-8 bytes of a WAM buffer save key, holding the raw encoded telemetry buffers
 * staged for upload. The disk-backed WAM store offloads each encoded-but-unsent buffer here rather than
 * inflating the metadata snapshot, exactly as the persistent chat store offloads message bodies to
 * {@link PersistentMessageStore}. This store is deliberately independent of the message store: it owns its
 * own file, {@link MVStore} handle and durability checkpoints, so telemetry staging neither shares nor
 * contends with the chat, newsletter and status maps.
 *
 * <p>The map is an {@link MVMap} of {@code byte[]} keys to {@code byte[]} buffer payloads. Keys use
 * {@link LexByteArrayType} for unsigned-byte (memcmp) order and values use H2's {@link ByteArrayDataType}.
 *
 * @apiNote
 * Only the persistent WAM sub-store consumes this facade. The instance is opened by
 * {@link PersistentLinkedWhatsAppStoreFactory}, wired into {@link PersistentStore} through
 * {@link PersistentStore#setWamBuffersStore(PersistentWamBuffersStore)}, and shut down through
 * {@link PersistentStore#close()} or {@link PersistentStore#delete()}.
 *
 * @implNote
 * This implementation applies writes directly on the caller's (virtual) thread to the concurrent
 * {@link MVMap}; durability is asynchronous, driven by MVStore's background auto-commit (so no caller
 * blocks on {@code fsync}) and forced at well-defined checkpoints by {@link #commit()} (wired into the
 * parent store's flush cadence) and finally by {@link #close()}. The buffer count is small (one per
 * unshipped flush slice), so a hard kill loses at most the buffers staged since the last auto-commit,
 * which the telemetry pipeline re-stages on the next flush. Because the file is accessed through a
 * {@link java.nio.channels.FileChannel} rather than a memory map, a closed store's file can be unlinked
 * even on Windows.
 *
 * @see PersistentMessageStore
 * @see PersistentLinkedWhatsAppWamStore
 */
final class PersistentWamBuffersStore implements AutoCloseable {
    /**
     * The logger for {@link PersistentWamBuffersStore}.
     */
    private static final System.Logger LOGGER = Log.get(PersistentWamBuffersStore.class);

    /**
     * The name of the WAM buffer map.
     */
    private static final String WAM_BUFFERS = "wam_buffers";

    /**
     * The underlying single-file store; owns the file handle and the background auto-commit writer.
     */
    private final MVStore store;

    /**
     * The {@value #WAM_BUFFERS} map, keyed by the UTF-8 bytes of a save key.
     */
    private final MVMap<byte[], byte[]> wamBuffers;

    /**
     * Constructs a facade around an already-opened store and its map.
     *
     * @apiNote
     * This constructor is private; instances are produced through {@link #open(Path)}.
     *
     * @param store      the opened MVStore
     * @param wamBuffers the WAM buffer map
     */
    private PersistentWamBuffersStore(MVStore store, MVMap<byte[], byte[]> wamBuffers) {
        this.store = store;
        this.wamBuffers = wamBuffers;
    }

    /**
     * Opens or creates the MVStore at {@code file} and returns a facade ready to stage and serve WAM event
     * buffers.
     *
     * @apiNote
     * Invoked by {@link PersistentLinkedWhatsAppStoreFactory} after the metadata snapshot is loaded or a
     * fresh store is built. The parent directory is created if it does not already exist.
     *
     * @implNote
     * This implementation opens the store with MVStore's default cache and auto-commit settings, then
     * opens the {@value #WAM_BUFFERS} map with the unsigned-lexicographic {@link LexByteArrayType} key type
     * and H2's {@link ByteArrayDataType} value type.
     *
     * @param file the {@code wam.mv} file; its parent directory is created if absent
     * @return a fully initialised facade
     * @throws IOException if the parent directory cannot be created or the store cannot be opened
     */
    static PersistentWamBuffersStore open(Path file) throws IOException {
        var parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        MVStore store;
        try {
            store = new MVStore.Builder()
                    .fileName(file.toString())
                    .open();
        } catch (RuntimeException error) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to open wam buffers store at " + file, error);
            throw new IOException("Could not open MVStore at " + file, error);
        }
        try {
            var wam = store.openMap(WAM_BUFFERS, new MVMap.Builder<byte[], byte[]>()
                    .keyType(LexByteArrayType.INSTANCE)
                    .valueType(ByteArrayDataType.INSTANCE));
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "opened wam buffers store at {0}", file);
            return new PersistentWamBuffersStore(store, wam);
        } catch (RuntimeException error) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to open wam buffer map at " + file, error);
            store.closeImmediately();
            throw new IOException("Could not open wam buffer map at " + file, error);
        }
    }

    /**
     * Returns the save keys of every staged WAM event buffer.
     *
     * @implNote
     * This implementation walks the map's keys and decodes each from its UTF-8 bytes; the buffer count is
     * small (one per unshipped flush slice), so a full key walk is cheap.
     *
     * @return a freshly allocated copy of the staged buffer save keys, never {@code null}
     */
    Collection<String> wamBufferKeys() {
        var result = new ArrayList<String>();
        var iterator = wamBuffers.keyIterator(null);
        while (iterator.hasNext()) {
            result.add(new String(iterator.next(), StandardCharsets.UTF_8));
        }
        return result;
    }

    /**
     * Stages a WAM event buffer under the given save key, replacing any buffer already staged under it.
     *
     * @implNote
     * This implementation stores the buffer under the UTF-8 bytes of {@code saveKey}; the put is atomic on
     * the concurrent {@link MVMap}, so no sibling-temp-file dance is needed as it was for the former
     * filesystem staging.
     *
     * @param saveKey the save key identifying the buffer, never {@code null}
     * @param buffer  the encoded buffer bytes, never {@code null}
     */
    void putWamBuffer(String saveKey, byte[] buffer) {
        wamBuffers.put(saveKey.getBytes(StandardCharsets.UTF_8), buffer);
    }

    /**
     * Returns the staged WAM event buffer for the given save key.
     *
     * @param saveKey the save key identifying the buffer, never {@code null}
     * @return the encoded buffer bytes, or empty if no buffer is staged under that key
     */
    Optional<byte[]> findWamBuffer(String saveKey) {
        return Optional.ofNullable(wamBuffers.get(saveKey.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Removes the staged WAM event buffer for the given save key.
     *
     * @param saveKey the save key identifying the buffer, never {@code null}
     * @return {@code true} if a buffer was removed, {@code false} if none was staged under that key
     */
    boolean removeWamBuffer(String saveKey) {
        return wamBuffers.remove(saveKey.getBytes(StandardCharsets.UTF_8)) != null;
    }

    /**
     * Removes every staged WAM event buffer.
     */
    void clearWamBuffers() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "clearing staged wam buffers");
        wamBuffers.clear();
    }

    /**
     * Forces any buffered changes to the underlying file.
     *
     * @apiNote
     * Called by {@link PersistentStore#await()} so the WAM buffer store reaches a durable checkpoint on the
     * same cadence as the metadata snapshot, including from the client's JVM shutdown hook.
     *
     * @implNote
     * This implementation delegates to {@link MVStore#commit()}, which persists the current version without
     * blocking writers; the background auto-commit otherwise covers the steady state.
     */
    void commit() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "committing wam buffers store");
        store.commit();
    }

    /**
     * Closes the underlying store, flushing all buffered changes and releasing the file handle.
     *
     * @apiNote
     * Called from {@link PersistentStore#close()} and {@link PersistentStore#delete()}. After this call
     * every accessor will fail.
     *
     * @implNote
     * This implementation delegates to {@link MVStore#close()}, which commits the current version before
     * closing. Because the file is accessed through a {@link java.nio.channels.FileChannel} rather than a
     * memory map, the file can be deleted immediately afterwards on every platform.
     */
    @Override
    public void close() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "closing wam buffers store");
        store.close();
    }
}
