package com.github.auties00.cobalt.store.cloud.protobuf;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStore;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import com.github.auties00.cobalt.util.BufferedProtobufInputStream;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@link CloudWhatsAppStoreFactory} that snapshots each Cloud session to a single {@code store.proto}
 * file on disk.
 *
 * <p>Data layout: each session lives under {@code <baseDirectory>/<phoneNumberId>/} with a single
 * {@code store.proto} snapshot; the credentials, Graph configuration, webhook configuration, and per-chat
 * read markers are all carried in that one file. Unlike the Linked persistent store there is no message
 * body store, because the Cloud transport keeps no message history.
 *
 * @apiNote
 * Cobalt embedders obtain instances through {@link CloudWhatsAppStoreFactory#persistent()} and its
 * overload; the class is public only so the interface's static methods can instantiate it across
 * packages, and embedders should depend on {@link CloudWhatsAppStoreFactory} rather than this type.
 *
 * @implNote
 * This implementation records the most recently created or loaded session in a {@code .latest} pointer
 * file beside the per-session directories so {@link #loadLatest()} resolves without scanning. The actual
 * snapshot write is driven by {@link CloudWhatsAppStore#save()} on the store the factory hands back; a
 * freshly created store carries its session directory as a builder-supplied property, and a loaded store
 * restores it from the snapshot, so no post-construction wiring step is needed.
 */
public final class PersistentCloudWhatsAppStoreFactory implements CloudWhatsAppStoreFactory {
    /**
     * The logger for {@link PersistentCloudWhatsAppStoreFactory}.
     */
    private static final System.Logger LOGGER = Log.get(PersistentCloudWhatsAppStoreFactory.class);

    /**
     * The default root directory for Cobalt persistent Cloud sessions.
     *
     * @implNote
     * This implementation resolves to {@code $HOME/.cobalt/proto/cloud}; embedders that need a custom
     * location pass an explicit directory to {@link #PersistentCloudWhatsAppStoreFactory(Path)}.
     */
    private static final Path DEFAULT_DIRECTORY = Path.of(System.getProperty("user.home"), ".cobalt", "proto", "cloud");

    /**
     * The name of the metadata file written under each session directory.
     */
    private static final String STORE_FILE = "store.proto";

    /**
     * The name of the pointer file, written under the base directory, that records the most recently
     * opened session for auto-resume.
     *
     * @implNote
     * The leading dot keeps the file visually out of the way; because it is a regular file rather than a
     * session directory it never collides with a phone-number-named session.
     */
    private static final String LATEST_SESSION_FILE = ".latest";

    /**
     * The root directory under which per-session folders are created.
     */
    private final Path baseDirectory;

    /**
     * Constructs a factory using {@link #DEFAULT_DIRECTORY}.
     *
     * @apiNote
     * Used by {@link CloudWhatsAppStoreFactory#persistent()}.
     */
    public PersistentCloudWhatsAppStoreFactory() {
        this(DEFAULT_DIRECTORY);
    }

    /**
     * Constructs a factory using the given root directory.
     *
     * @apiNote
     * Used by {@link CloudWhatsAppStoreFactory#persistent(Path)}.
     *
     * @param directory the root directory under which per-session folders are created; must not be
     *                  {@code null}
     * @throws NullPointerException if {@code directory} is {@code null}
     */
    public PersistentCloudWhatsAppStoreFactory(Path directory) {
        this.baseDirectory = Objects.requireNonNull(directory, "directory cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link Optional#empty()} when the session's {@code store.proto} does
     * not exist; otherwise it decodes the snapshot, whose persisted session directory is restored with
     * it, and records it as the latest session.
     */
    @Override
    public Optional<CloudWhatsAppStore> load(String phoneNumberId) throws IOException {
        Objects.requireNonNull(phoneNumberId, "phoneNumberId cannot be null");
        var sessionDirectory = sessionDirectory(phoneNumberId);
        var storeFile = sessionDirectory.resolve(STORE_FILE);
        if (Files.notExists(storeFile)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no cloud store snapshot found for {0}", new LogRedactable.Phone(phoneNumberId));
            return Optional.empty();
        }
        var store = deserialize(storeFile);
        writeLatestSession(phoneNumberId);
        if (Log.INFO) LOGGER.log(Level.INFO, "loaded cloud store for {0}", new LogRedactable.Phone(phoneNumberId));
        return Optional.of(store);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation resolves the {@link #readLatestSession() latest-session pointer} in a single
     * read and loads that session directly; there is no directory scan. A dangling pointer (one whose
     * {@code store.proto} no longer exists) yields {@link Optional#empty()}.
     */
    @Override
    public Optional<CloudWhatsAppStore> loadLatest() throws IOException {
        var latest = readLatestSession();
        if (latest.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no latest cloud session pointer recorded");
            return Optional.empty();
        }
        return load(latest.get());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation seeds the store with the two credentials plus the default
     * {@link CloudApiVersion}, supplies its session directory through the builder, writes the initial
     * snapshot, and records it as the latest session so a later {@link #loadLatest()} resumes it.
     */
    @Override
    public CloudWhatsAppStore create(String accessToken, String phoneNumberId) throws IOException {
        Objects.requireNonNull(accessToken, "accessToken cannot be null");
        Objects.requireNonNull(phoneNumberId, "phoneNumberId cannot be null");
        var store = new ProtobufCloudWhatsAppStoreBuilder()
                .accessToken(accessToken)
                .phoneNumberId(phoneNumberId)
                .apiVersion(CloudApiVersion.DEFAULT.version())
                .storeDirectory(sessionDirectory(phoneNumberId))
                .build();
        store.save();
        writeLatestSession(phoneNumberId);
        if (Log.INFO) LOGGER.log(Level.INFO, "created cloud store for {0}", new LogRedactable.Phone(phoneNumberId));
        return store;
    }

    /**
     * Returns the session directory for {@code phoneNumberId} under the base directory.
     *
     * @param phoneNumberId the phone number id keying the session
     * @return the session directory path
     */
    private Path sessionDirectory(String phoneNumberId) {
        return baseDirectory.resolve(phoneNumberId);
    }

    /**
     * Deserialises a {@code store.proto} snapshot into a {@code ProtobufCloudWhatsAppStore}.
     *
     * @param storeFile the snapshot file
     * @return the deserialised store
     * @throws IOException if the file cannot be read or decoded
     */
    private static ProtobufCloudWhatsAppStore deserialize(Path storeFile) throws IOException {
        try (var stream = new BufferedProtobufInputStream(storeFile)) {
            return ProtobufCloudWhatsAppStoreSpec.decode(stream);
        }
    }

    /**
     * Records {@code phoneNumberId} as the most recently opened session, so {@link #readLatestSession()}
     * can resolve it without scanning every session directory.
     *
     * @implNote
     * This implementation writes the identifier to a sibling {@code .tmp} file and then issues an
     * {@link StandardCopyOption#ATOMIC_MOVE atomic move}, falling back to a
     * {@link StandardCopyOption#REPLACE_EXISTING replacing move} on file systems that cannot move
     * atomically, so a crash mid-write never leaves a truncated pointer.
     *
     * @param phoneNumberId the session identifier to record
     * @throws IOException if the pointer file cannot be written or moved
     */
    private void writeLatestSession(String phoneNumberId) throws IOException {
        Files.createDirectories(baseDirectory);
        var pointer = baseDirectory.resolve(LATEST_SESSION_FILE);
        var temp = baseDirectory.resolve(LATEST_SESSION_FILE + ".tmp");
        Files.writeString(temp, phoneNumberId);
        try {
            Files.move(temp, pointer, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException _) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "atomic move unsupported, falling back to replacing move for latest session pointer");
            Files.move(temp, pointer, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Returns the most recently opened session identifier, as recorded by
     * {@link #writeLatestSession(String)}, or an empty {@link Optional} when no pointer has been written
     * yet.
     *
     * @implNote
     * This implementation reads the pointer optimistically and treats a {@link NoSuchFileException} as an
     * absent pointer rather than pre-checking existence, avoiding the redundant stat of a
     * check-then-read.
     *
     * @return the recorded session identifier, or empty when the pointer is absent or blank
     * @throws IOException if the pointer file exists but cannot be read
     */
    private Optional<String> readLatestSession() throws IOException {
        var pointer = baseDirectory.resolve(LATEST_SESSION_FILE);
        try {
            var phoneNumberId = Files.readString(pointer).strip();
            return phoneNumberId.isEmpty() ? Optional.empty() : Optional.of(phoneNumberId);
        } catch (NoSuchFileException _) {
            return Optional.empty();
        }
    }
}
