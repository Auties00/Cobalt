package com.github.auties00.cobalt.store.linked.protobuf.persistent;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevice;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.store.*;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppChatStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStoreFactory;
import com.github.auties00.cobalt.store.linked.protobuf.*;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientSixPartsKeys;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The {@link LinkedWhatsAppStoreFactory} that snapshots session metadata to {@code store.proto} and
 * offloads message bodies to an embedded {@link PersistentMessageStore MVStore} per session.
 *
 * @apiNote
 * Cobalt embedders obtain instances through {@link LinkedWhatsAppStoreFactory#persistent()} and its
 * overloads; the class is package-private so the persistence strategy is not part of the public
 * API surface. Each session lives under {@code <baseDirectory>/<clientType>/<sessionId>/} with a
 * {@code store.proto} metadata snapshot beside a {@code messages.mv} message file and a {@code wam.mv}
 * WAM buffer file.
 *
 * @implNote
 * This implementation runs an orphan-recovery pass on {@link #load load}: after the metadata
 * snapshot deserialises, the factory walks the MVStore once and inserts metadata stubs for any
 * chat or newsletter that holds bodies in MVStore but is missing from the snapshot. This bridges the
 * post-commit window where an MVStore write landed but the next metadata save never ran (process
 * killed in between). Recovered entries surface through the normal
 * {@link LinkedWhatsAppChatStore#chats()} and {@link LinkedWhatsAppChatStore#newsletters()} collections so callers
 * see a consistent shape regardless of whether the previous shutdown was clean or crashy.
 */
public final class PersistentLinkedWhatsAppStoreFactory implements LinkedWhatsAppStoreFactory {
    /**
     * The logger for {@link PersistentLinkedWhatsAppStoreFactory}.
     */
    private static final System.Logger LOGGER = Log.get(PersistentLinkedWhatsAppStoreFactory.class);

    /**
     * The default root directory for Cobalt persistent sessions.
     *
     * @implNote
     * This implementation resolves to {@code $HOME/.cobalt/proto}; embedders that need a custom
     * location pass an explicit directory to {@link #PersistentLinkedWhatsAppStoreFactory(Path)} or
     * {@link #PersistentLinkedWhatsAppStoreFactory(Path, long)}.
     */
    private static final Path DEFAULT_DIRECTORY = Path.of(System.getProperty("user.home"), ".cobalt", "proto");

    /**
     * The default map-size hint in bytes.
     *
     * @implNote
     * This 8 GiB value is the legacy libmdbx geometry upper bound. The MVStore backend grows its file
     * on demand and ignores it; it is retained only so the {@code mapSize}-bearing factory and
     * {@code open} signatures stay source-compatible.
     */
    private static final long DEFAULT_MAP_SIZE = 8L * 1024 * 1024 * 1024;

    /**
     * The name of the pointer file, written inside each client type's home directory, that records
     * the most recently opened session identifier for auto-resume.
     *
     * @implNote
     * The leading dot keeps the file visually out of the way; because it is a regular file rather
     * than a session directory it never collides with a UUID- or phone-number-named session.
     */
    private static final String LATEST_SESSION_FILE = ".latest";

    /**
     * The root directory under which per-session folders are created.
     */
    private final Path directory;

    /**
     * The map-size hint in bytes, validated positive but ignored by the MVStore backend.
     */
    private final long mapSize;

    /**
     * Constructs a factory using {@link #DEFAULT_DIRECTORY} and {@link #DEFAULT_MAP_SIZE}.
     *
     * @apiNote
     * Used by {@link LinkedWhatsAppStoreFactory#persistent()}.
     */
    public PersistentLinkedWhatsAppStoreFactory() {
        this(DEFAULT_DIRECTORY, DEFAULT_MAP_SIZE);
    }

    /**
     * Constructs a factory using the given root directory and {@link #DEFAULT_MAP_SIZE}.
     *
     * @apiNote
     * Used by {@link LinkedWhatsAppStoreFactory#persistent(Path)}.
     *
     * @param directory the root directory under which per-session folders are created; must not
     *                  be {@code null}
     */
    public PersistentLinkedWhatsAppStoreFactory(Path directory) {
        this(directory, DEFAULT_MAP_SIZE);
    }

    /**
     * Constructs a factory using the given root directory and map-size hint.
     *
     * @apiNote
     * Used by {@link LinkedWhatsAppStoreFactory#persistent(Path, long)}.
     *
     * @implNote
     * This implementation rejects a non-positive {@code mapSize} eagerly so the failure surfaces at
     * factory construction time, even though the MVStore backend itself ignores the value.
     *
     * @param directory the root directory under which per-session folders are created; must not
     *                  be {@code null}
     * @param mapSize   the legacy map-size hint in bytes, ignored by the MVStore backend; must be positive
     * @throws IllegalArgumentException if {@code mapSize <= 0}
     * @throws NullPointerException     if {@code directory} is {@code null}
     */
    public PersistentLinkedWhatsAppStoreFactory(Path directory, long mapSize) {
        if (mapSize <= 0) {
            throw new IllegalArgumentException("mapSize must be positive");
        }
        this.directory = Objects.requireNonNull(directory, "directory cannot be null");
        this.mapSize = mapSize;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation forwards to {@link #loadSession(LinkedWhatsAppClientType, String)} with the
     * UUID stringified.
     */
    @Override
    public Optional<LinkedWhatsAppStore> load(LinkedWhatsAppClientType clientType, UUID uuid) throws IOException {
        Objects.requireNonNull(clientType, "clientType cannot be null");
        Objects.requireNonNull(uuid, "uuid cannot be null");
        return loadSession(clientType, uuid.toString());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation forwards to {@link #loadSession(LinkedWhatsAppClientType, String)} with the
     * phone number stringified.
     */
    @Override
    public Optional<LinkedWhatsAppStore> load(LinkedWhatsAppClientType clientType, long phoneNumber) throws IOException {
        Objects.requireNonNull(clientType, "clientType cannot be null");
        return loadSession(clientType, String.valueOf(phoneNumber));
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation resolves the {@link #readLatestSession(LinkedWhatsAppClientType) latest-session
     * pointer} in a single read and loads that session directly; there is no directory scan. When
     * the pointer is absent, or names a session whose {@code store.proto} no longer exists, the
     * result is {@link Optional#empty()}.
     */
    @Override
    public Optional<LinkedWhatsAppStore> loadLatest(LinkedWhatsAppClientType clientType) throws IOException {
        Objects.requireNonNull(clientType, "clientType cannot be null");
        var pointer = readLatestSession(clientType);
        if (pointer.isEmpty()) {
            return Optional.empty();
        }
        return loadSession(clientType, pointer.get());
    }

    /**
     * Loads the session identified by {@code sessionId} for the given client type, opens its
     * MVStore, attaches it to the deserialised store, and runs the orphan-recovery pass.
     *
     * @apiNote
     * Internal helper for the three public {@code load} overloads.
     *
     * @param clientType the client type
     * @param sessionId  the session UUID string or phone-number string
     * @return the loaded store, or {@link Optional#empty()} if no {@code store.proto} exists for
     *         that session
     * @throws IOException if the metadata file cannot be read or decoded
     */
    private Optional<LinkedWhatsAppStore> loadSession(LinkedWhatsAppClientType clientType, String sessionId) throws IOException {
        var storeFile = PersistentStore.storeFilePath(clientType, directory, sessionId);
        if (Files.notExists(storeFile)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no persisted session found for client type {0}", clientType);
            return Optional.empty();
        }
        var store = PersistentStore.deserialize(storeFile);
        var envPath = PersistentStore.messagesEnvPath(clientType, directory, sessionId);
        var messageStore = PersistentMessageStore.open(envPath, mapSize);
        store.setMessageStore(messageStore);
        var wamPath = PersistentStore.wamBuffersEnvPath(clientType, directory, sessionId);
        store.setWamBuffersStore(PersistentWamBuffersStore.open(wamPath));
        recoverOrphans(store, messageStore);
        writeLatestSession(clientType, sessionId);
        if (Log.INFO) LOGGER.log(Level.INFO, "session loaded for client type {0}", clientType);
        return Optional.of(store);
    }

    /**
     * Inserts metadata stubs for every chat or newsletter that holds messages in
     * {@code messageStore} but has no corresponding entry in the deserialised snapshot.
     *
     * @apiNote
     * Bridges the post-commit window where an MVStore write succeeded but the next metadata save
     * never happened (for example, the process was killed between the two).
     *
     * @implNote
     * This implementation walks {@link PersistentMessageStore#distinctChatJids()} and
     * {@link PersistentMessageStore#distinctNewsletterJids()} once and reuses
     * {@link PersistentLinkedWhatsAppChatStore#addNewChat} and {@link PersistentLinkedWhatsAppChatStore#addNewNewsletter} to build the
     * stubs so the inserted entries receive the same attachment handling as fresh ones.
     *
     * @param store        the freshly attached store
     * @param messageStore the just-opened MVStore facade
     */
    private static void recoverOrphans(PersistentStore store, PersistentMessageStore messageStore) {
        var chatStore = store.chatStore();
        var recoveredChats = 0;
        for (var chatJid : messageStore.distinctChatJids()) {
            if (!chatStore.chats.containsKey(chatJid)) {
                chatStore.addNewChat(chatJid);
                recoveredChats++;
            }
        }
        var recoveredNewsletters = 0;
        for (var newsletterJid : messageStore.distinctNewsletterJids()) {
            if (!chatStore.newsletters.containsKey(newsletterJid)) {
                chatStore.addNewNewsletter(newsletterJid);
                recoveredNewsletters++;
            }
        }
        if (Log.WARNING && (recoveredChats > 0 || recoveredNewsletters > 0)) {
            LOGGER.log(Level.WARNING, "recovered {0} orphan chats and {1} orphan newsletters from message store",
                    recoveredChats, recoveredNewsletters);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation generates a random UUID when {@code uuid} is {@code null}, opens a
     * fresh MVStore, and returns an otherwise empty store with the platform-appropriate device
     * descriptor.
     */
    @Override
    public LinkedWhatsAppStore create(LinkedWhatsAppClientType clientType, UUID uuid) throws IOException {
        Objects.requireNonNull(clientType, "clientType cannot be null");
        var resolvedUuid = Objects.requireNonNullElseGet(uuid, UUID::randomUUID);
        var sessionId = resolvedUuid.toString();
        var store = new PersistentStore(
                new ProtobufLinkedWhatsAppSignalStoreBuilder().build(),
                new ProtobufLinkedWhatsAppAccountStoreBuilder()
                        .uuid(resolvedUuid)
                        .clientType(clientType)
                        .device(defaultDevice(clientType))
                        .build(),
                new ProtobufLinkedWhatsAppContactStoreBuilder().build(),
                new ProtobufLinkedWhatsAppSyncStoreBuilder().build(),
                new ProtobufLinkedWhatsAppSettingsStoreBuilder().build(),
                directory,
                new ProtobufLinkedWebSessionStoreBuilder().build(),
                new PersistentLinkedWhatsAppWamStoreBuilder().build(),
                new PersistentLinkedWhatsAppChatStoreBuilder().build());
        attachFreshStores(store, clientType, sessionId);
        return store;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation keys the session directory by the stringified phone number and assigns
     * a fresh random UUID.
     */
    @Override
    public LinkedWhatsAppStore create(LinkedWhatsAppClientType clientType, long phoneNumber) throws IOException {
        Objects.requireNonNull(clientType, "clientType cannot be null");
        var sessionId = String.valueOf(phoneNumber);
        var store = new PersistentStore(
                new ProtobufLinkedWhatsAppSignalStoreBuilder().build(),
                new ProtobufLinkedWhatsAppAccountStoreBuilder()
                        .uuid(UUID.randomUUID())
                        .phoneNumber(phoneNumber)
                        .clientType(clientType)
                        .device(defaultDevice(clientType))
                        .build(),
                new ProtobufLinkedWhatsAppContactStoreBuilder().build(),
                new ProtobufLinkedWhatsAppSyncStoreBuilder().build(),
                new ProtobufLinkedWhatsAppSettingsStoreBuilder().build(),
                directory,
                new ProtobufLinkedWebSessionStoreBuilder().build(),
                new PersistentLinkedWhatsAppWamStoreBuilder().build(),
                new PersistentLinkedWhatsAppChatStoreBuilder().build());
        attachFreshStores(store, clientType, sessionId);
        return store;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation builds a web-device store seeded with the supplied Noise and identity
     * key pairs, sets {@code registered=true} so the pairing pipeline is skipped, and assigns
     * the user JID derived from the bundled phone number. Used to bootstrap a web session from a
     * previously exported six-parts key blob without a fresh QR pairing flow.
     */
    @Override
    public LinkedWhatsAppStore create(LinkedWhatsAppClientType clientType, LinkedWhatsAppClientSixPartsKeys sixPartsKeys) throws IOException {
        Objects.requireNonNull(clientType, "clientType cannot be null");
        Objects.requireNonNull(sixPartsKeys, "sixPartsKeys cannot be null");
        var phoneNumber = sixPartsKeys.phoneNumber();
        var sessionId = String.valueOf(phoneNumber);
        var store = new PersistentStore(
                new ProtobufLinkedWhatsAppSignalStoreBuilder()
                        .noiseKeyPair(sixPartsKeys.noiseKeyPair())
                        .identityKeyPair(sixPartsKeys.identityKeyPair())
                        .identityId(sixPartsKeys.identityId())
                        .build(),
                new ProtobufLinkedWhatsAppAccountStoreBuilder()
                        .uuid(UUID.randomUUID())
                        .phoneNumber(phoneNumber)
                        .clientType(clientType)
                        .device(LinkedWhatsAppClientDevice.web())
                        .registered(true)
                        .jid(Jid.of(phoneNumber))
                        .build(),
                new ProtobufLinkedWhatsAppContactStoreBuilder().build(),
                new ProtobufLinkedWhatsAppSyncStoreBuilder().build(),
                new ProtobufLinkedWhatsAppSettingsStoreBuilder().build(),
                directory,
                new ProtobufLinkedWebSessionStoreBuilder().build(),
                new PersistentLinkedWhatsAppWamStoreBuilder().build(),
                new PersistentLinkedWhatsAppChatStoreBuilder().build());
        attachFreshStores(store, clientType, sessionId);
        return store;
    }

    /**
     * Opens fresh MVStores for {@code sessionId}, wires them into {@code store}, and records the
     * session as the most recently opened one.
     *
     * @apiNote
     * Internal helper shared by every {@code create(...)} overload after the metadata builder
     * produces an otherwise empty store.
     *
     * @implNote
     * This implementation opens both the message store and the independent WAM buffer store, then writes
     * the {@link #writeLatestSession(LinkedWhatsAppClientType, String) latest-session pointer} so a
     * subsequent {@link #loadLatest(LinkedWhatsAppClientType)} resumes the session just created without
     * scanning the home directory.
     *
     * @param store      the freshly built store
     * @param clientType the client type
     * @param sessionId  the session UUID string or phone-number string
     * @throws IOException if the session directory cannot be created or the pointer cannot be written
     */
    private void attachFreshStores(PersistentStore store, LinkedWhatsAppClientType clientType, String sessionId) throws IOException {
        var envPath = PersistentStore.messagesEnvPath(clientType, directory, sessionId);
        store.setMessageStore(PersistentMessageStore.open(envPath, mapSize));
        var wamPath = PersistentStore.wamBuffersEnvPath(clientType, directory, sessionId);
        store.setWamBuffersStore(PersistentWamBuffersStore.open(wamPath));
        writeLatestSession(clientType, sessionId);
        if (Log.INFO) LOGGER.log(Level.INFO, "new session created for client type {0}", clientType);
    }

    /**
     * Records {@code sessionId} as the most recently opened session for {@code clientType}, so
     * {@link #readLatestSession(LinkedWhatsAppClientType)} can resolve it without scanning every session
     * directory.
     *
     * @apiNote
     * Invoked whenever a session becomes the active one: on creation and on a successful load. The
     * pointer captures the session the embedder actually opened, a more faithful notion of "latest"
     * than a filesystem modification time that a backup, antivirus scan, or unrelated write could
     * perturb.
     *
     * @implNote
     * This implementation writes the identifier to a sibling {@code .tmp} file and then issues an
     * {@link StandardCopyOption#ATOMIC_MOVE atomic move}, falling back to a
     * {@link StandardCopyOption#REPLACE_EXISTING replacing move} on file systems that cannot move
     * atomically, so a crash mid-write never leaves a truncated pointer.
     *
     * @param clientType the client type that owns the home directory
     * @param sessionId  the session identifier to record
     * @throws IOException if the pointer file cannot be written or moved
     */
    private void writeLatestSession(LinkedWhatsAppClientType clientType, String sessionId) throws IOException {
        var home = ProtobufWhatsAppStore.getHomeDirectory(clientType, directory);
        var pointer = home.resolve(LATEST_SESSION_FILE);
        var temp = home.resolve(LATEST_SESSION_FILE + ".tmp");
        Files.writeString(temp, sessionId);
        try {
            Files.move(temp, pointer, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException _) {
            Files.move(temp, pointer, StandardCopyOption.REPLACE_EXISTING);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "recorded latest session pointer for client type {0}", clientType);
    }

    /**
     * Returns the most recently opened session identifier for {@code clientType}, as recorded by
     * {@link #writeLatestSession(LinkedWhatsAppClientType, String)}, or an empty {@link Optional} when no
     * pointer has been written yet.
     *
     * <p>The identifier is not validated against the filesystem: the pointed session may since have
     * been deleted. {@link #loadLatest(LinkedWhatsAppClientType)} treats a dangling pointer the same as a
     * missing one.
     *
     * @implNote
     * This implementation reads the pointer optimistically and treats a {@link NoSuchFileException}
     * as an absent pointer rather than pre-checking existence, avoiding the redundant stat of a
     * check-then-read.
     *
     * @param clientType the client type that owns the home directory
     * @return the recorded session identifier, or empty when the pointer is absent or blank
     * @throws IOException if the pointer file exists but cannot be read
     */
    private Optional<String> readLatestSession(LinkedWhatsAppClientType clientType) throws IOException {
        var pointer = ProtobufWhatsAppStore.getHomeDirectory(clientType, directory)
                .resolve(LATEST_SESSION_FILE);
        try {
            var sessionId = Files.readString(pointer).strip();
            return sessionId.isEmpty() ? Optional.empty() : Optional.of(sessionId);
        } catch (NoSuchFileException _) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no latest-session pointer found for client type {0}", clientType);
            return Optional.empty();
        }
    }

    /**
     * Returns the synthetic device descriptor used for newly created sessions of the given
     * client type.
     *
     * @apiNote
     * Internal helper that picks a desktop-shaped {@link LinkedWhatsAppClientDevice} for web sessions and an
     * iOS-shaped descriptor for mobile sessions.
     *
     * @param clientType the client type
     * @return a fresh {@link LinkedWhatsAppClientDevice} suitable for the type
     */
    private static LinkedWhatsAppClientDevice defaultDevice(LinkedWhatsAppClientType clientType) {
        return switch (clientType) {
            case WEB -> LinkedWhatsAppClientDevice.desktop();
            case MOBILE -> LinkedWhatsAppClientDevice.ios(false);
        };
    }
}
