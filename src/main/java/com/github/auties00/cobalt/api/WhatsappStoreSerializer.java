
package com.github.auties00.cobalt.api;

import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.info.ContextInfo;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.message.model.ContextualMessage;
import com.github.auties00.cobalt.model.newsletter.Newsletter;
import com.github.auties00.cobalt.model.sync.HistorySyncMessage;
import com.github.auties00.cobalt.util.ImmutableLinkedList;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElseGet;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;

/**
 * A serialization interface for managing persistent storage of WhatsApp session data.
 * <p>
 * This interface provides a standardized mechanism for serializing and deserializing ({@link WhatsappStore}).
 * <p>
 * The interface supports multiple client types ({@link WhatsappClientType#WEB} and {@link WhatsappClientType#MOBILE})
 * and can be implemented to provide different storage backends. Two built-in implementations are provided:
 * <ul>
 *     <li>{@link WhatsappStoreSerializer#toProtobuf()} - Persists sessions as Protocol Buffer files on disk</li>
 *     <li>{@link WhatsappStoreSerializer#discarding()} - Discards all session data (useful for temporary/ephemeral sessions)</li>
 * </ul>
 * <p>
 * Sessions are identified by either a UUID or a phone number.
 * <p>
 * <b>Thread Safety:</b> Implementations should be thread-safe, as serialization operations may be
 * called concurrently from different parts of the WhatsApp API.
 *
 * @see WhatsappStore
 */
@SuppressWarnings("unused")
public abstract class WhatsappStoreSerializer {
    /**
     * Creates a serializer that discards all session data without persisting it.
     * <p>
     * This implementation is useful for temporary sessions where persistence is not required,
     * such as testing environments or scenarios where sessions should not survive application restarts.
     *
     * @return a serializer that performs no actual persistence
     */
    public static WhatsappStoreSerializer discarding() {
        return Discarding.INSTANCE;
    }

    /**
     * Creates a protobuf-based serializer that persists sessions to the default directory.
     * <p>
     * Sessions are stored as Protocol Buffer files in the default location, typically
     * {@code $HOME/.whatsapp/} on Unix-like systems or {@code %USERPROFILE%\.whatsapp\}
     * on Windows.
     *
     * @return a new protobuf-based serializer using the default storage directory
     */
    public static WhatsappStoreSerializer toProtobuf() {
        return new Protobuf();
    }

    /**
     * Creates a protobuf-based serializer that persists sessions to a specified directory.
     * <p>
     * This allows customization of where session files are stored, which can be useful for
     * applications that need to manage multiple isolated session stores or have specific
     * file system requirements.
     *
     * @param baseDirectory the directory where session files should be stored; must be writable
     * @return a new protobuf-based serializer using the specified storage directory
     */
    public static WhatsappStoreSerializer toProtobuf(Path baseDirectory) {
        return new Protobuf(baseDirectory);
    }

    /**
     * Retrieves an existing session from persistent storage using one or more identifiers.
     * <p>
     * This method attempts to locate and deserialize a session using the provided identifiers
     * in the following order:
     * <ol>
     *     <li>If {@code uuid} is provided, attempts to load the session by UUID</li>
     *     <li>If UUID lookup fails or is null, and {@code phoneNumber} is provided, attempts to load by phone number</li>
     * </ol>
     *
     * @param clientType  the type of WhatsApp client (web or mobile); must not be null
     * @param id          the unique identifier of the session to retrieve; may be null
     * @param phoneNumber the phone number associated with the session; may be null
     * @return an {@link Optional} containing the session if found, or empty if no matching session exists
     */
    public Optional<WhatsappStore> startDeserialize(WhatsappClientType clientType, UUID id, Long phoneNumber) {
        if (id != null) {
            var store = startDeserialize(clientType, id);
            if(store.isPresent()) {
                return store;
            }
        }

        if (phoneNumber != null) {
            var store = startDeserialize(clientType, phoneNumber);
            if(store.isPresent()) {
                return store;
            }
        }

        return Optional.empty();
    }

    /**
     * Lists all session UUIDs stored for a specific client type.
     * <p>
     * This method scans the persistent storage and returns the UUIDs of all sessions
     * associated with the specified client type. The returned collection maintains
     * the insertion order (typically the order in which sessions were created).
     *
     * @param type the WhatsApp client type to query; must not be null
     * @return a sequenced collection of UUIDs, never null but may be empty if no sessions exist
     */
    public abstract SequencedCollection<UUID> listIds(WhatsappClientType type);

    /**
     * Lists all session phone numbers stored for a specific client type.
     * <p>
     * This method scans the persistent storage and returns the phone numbers of all sessions
     * associated with the specified client type. The returned collection maintains
     * the insertion order (typically the order in which sessions were created).
     * <p>
     * Note: Not all sessions have associated phone numbers (particularly web sessions
     * that haven't completed pairing), so this list may be smaller than the list
     * returned by {@link #listIds(WhatsappClientType)}.
     *
     * @param type the WhatsApp client type to query; must not be null
     * @return a sequenced collection of phone numbers, never null but may be empty
     */
    public abstract SequencedCollection<Long> listPhoneNumbers(WhatsappClientType type);

    /**
     * Persists session state to storage.
     * <p>
     * This method serializes the provided {@link WhatsappStore} instance to persistent storage,
     * overwriting any existing store for the same session. Implementations should ensure
     * that the serialization is atomic to prevent data corruption in case of interruption.
     *
     * @param store the store to serialize; must not be null
     */
    public abstract void serialize(WhatsappStore store);

    /**
     * Retrieves session state from storage by UUID.
     * <p>
     * This method deserializes a {@link WhatsappStore} instance from persistent storage using
     * the session's UUID as the identifier.
     * <p>
     * <b>Important:</b> Implementations should only block while deserializing data that is
     * strictly necessary to bootstrap a WhatsApp session (e.g., JID, encryption keys references,
     * contact list). Large collections like chat history and newsletters should be deserialized
     * asynchronously to avoid blocking the caller for extended periods.
     *
     * @param type the WhatsApp client type; must not be null
     * @param id   the UUID of the session whose store should be retrieved; must not be null
     * @return an {@link Optional} containing the store if found, or empty if no store exists for this UUID
     * @see #finishDeserialize(WhatsappStore)
     */
    public abstract Optional<WhatsappStore> startDeserialize(WhatsappClientType type, UUID id);

    /**
     * Retrieves session state from storage by phone number.
     * <p>
     * This method deserializes a {@link WhatsappStore} instance from persistent storage using
     * the session's phone number as the identifier.
     * <p>
     * <b>Important:</b> Implementations should only block while deserializing data that is
     * strictly necessary to bootstrap a WhatsApp session (e.g., JID, encryption keys references,
     * contact list). Large collections like chat history and newsletters should be deserialized
     * asynchronously to avoid blocking the caller for extended periods.
     *
     * @param type        the WhatsApp client type; must not be null
     * @param phoneNumber the phone number of the session whose store should be retrieved; must not be null
     * @return an {@link Optional} containing the store if found, or empty if no store exists for this phone number
     * @see #finishDeserialize(WhatsappStore)
     */
    public abstract Optional<WhatsappStore> startDeserialize(WhatsappClientType type, Long phoneNumber);

    /**
     * Blocks until all asynchronous deserialization operations for a store are complete.
     * <p>
     * This method should be called after {@link #startDeserialize(WhatsappClientType, UUID)} or
     * {@link #startDeserialize(WhatsappClientType, Long)} when the caller needs to ensure that
     * all session data (including large collections like chats and newsletters) has been fully
     * loaded into memory.
     * <p>
     * Implementations that perform all deserialization synchronously may provide an empty
     * implementation of this method.
     *
     * @param store the store whose deserialization should be completed; must not be null
     */
    public abstract void finishDeserialize(WhatsappStore store);

    /**
     * Permanently removes a session from storage.
     * <p>
     * This method deletes all persistent data associated with the specified session.
     * After this operation completes, the session cannot be recovered and a new session must be created to use WhatsApp again.
     * <p>
     * This operation should be atomic and should not leave partial session data behind
     * in case of errors.
     *
     * @param type the WhatsApp client type; must not be null
     * @param uuid the UUID of the session to delete; must not be null
     */
    public abstract void deleteSession(WhatsappClientType type, UUID uuid);

    /**
     * Private implementation for a serializer that discards all data.
     */
    private static class Discarding extends WhatsappStoreSerializer {
        private static final Discarding INSTANCE = new Discarding();

        @Override
        public SequencedCollection<UUID> listIds(WhatsappClientType type) {
            return ImmutableLinkedList.empty();
        }

        @Override
        public SequencedCollection<Long> listPhoneNumbers(WhatsappClientType type) {
            return ImmutableLinkedList.empty();
        }

        @Override
        public void serialize(WhatsappStore store) {

        }

        @Override
        public Optional<WhatsappStore> startDeserialize(WhatsappClientType type, UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<WhatsappStore> startDeserialize(WhatsappClientType type, Long phoneNumber) {
            return Optional.empty();
        }

        @Override
        public void deleteSession(WhatsappClientType type, UUID uuid) {

        }

        @Override
        public void finishDeserialize(WhatsappStore store) {

        }
    }

    /**
     * Private implementation for a serializer that stores data on file using Protobuf
     */
    private static class Protobuf extends WhatsappStoreSerializer {
        private static final String CHAT_PREFIX = "chat_";
        private static final String NEWSLETTER_PREFIX = "newsletter_";
        private static final Path DEFAULT_SERIALIZER_PATH = Path.of(System.getProperty("user.home") + "/.cobalt/");


        private final Path baseDirectory;
        private final ConcurrentMap<UUID, Integer> storesHashCodes;
        private final ConcurrentMap<UUID, Thread> storesAttributions;
        private final ConcurrentMap<StoreJidPair, Integer> jidsHashCodes;
        private final ReentrantKeyedLock storeLock;

        private Protobuf() {
            this(DEFAULT_SERIALIZER_PATH);
        }

       private Protobuf(Path baseDirectory) {
            Objects.requireNonNull(baseDirectory, "baseDirectory cannot be null");
            this.baseDirectory = baseDirectory;
            this.storesHashCodes = new ConcurrentHashMap<>();
            this.storesAttributions = new ConcurrentHashMap<>();
            this.jidsHashCodes = new ConcurrentHashMap<>();
            this.storeLock = new ReentrantKeyedLock();
        }

        @Override
        public SequencedCollection<UUID> listIds(WhatsappClientType type) {
            return list(type, file -> {
                try {
                    var fileName = file.getFileName().toString();
                    var value = UUID.fromString(fileName);
                    return Optional.of(value);
                } catch (IllegalArgumentException ignored) {
                    return Optional.empty();
                }
            });
        }

        @Override
        public SequencedCollection<Long> listPhoneNumbers(WhatsappClientType type) {
            return list(type, file -> {
                try {
                    var fileName = file.getFileName().toString();
                    if(fileName.isEmpty()) {
                        return Optional.empty();
                    }
                    var result = Long.parseUnsignedLong(fileName, fileName.charAt(0) == '+' ? 1 : 0, fileName.length(), 10);
                    return Optional.of(result);
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            });
        }

        private <T> SequencedCollection<T> list(WhatsappClientType type, Function<Path, Optional<T>> adapter) {
            Objects.requireNonNull(type, "type cannot be null");

            var directory = getHome(type);
            if (Files.notExists(directory)) {
                return ImmutableLinkedList.empty();
            }

            try (var walker = Files.walk(directory, 1)
                    .sorted(Comparator.comparing(this::getLastModifiedTime))) {
                return walker.map(adapter)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toCollection(LinkedList::new));
            } catch (IOException exception) {
                return ImmutableLinkedList.empty();
            }
        }

        private FileTime getLastModifiedTime(Path path) {
            try {
                return Files.getLastModifiedTime(path);
            } catch (IOException exception) {
                return FileTime.fromMillis(0);
            }
        }

        @Override
        public void serialize(WhatsappStore store) {
            Objects.requireNonNull(store, "store cannot be null");
            try {
                storeLock.lock(store.uuid());
                var oldHashCode = storesHashCodes.getOrDefault(store.uuid(), -1);
                var newHashCode = store.hashCode();
                if(oldHashCode == newHashCode) {
                    return;
                }

                storesHashCodes.put(store.uuid(), newHashCode);
                try(var executor = newVirtualThreadPerTaskExecutor()) {
                    executor.submit(() -> encodeStore(store, getSessionFile(store, "store" + ".proto")));
                    store.chats()
                            .forEach(chat -> executor.submit(() -> serializeChat(store, chat)));
                    store.newsletters()
                            .forEach(newsletter -> executor.submit(() -> serializeNewsletter(store, newsletter)));
                    var phoneNumber = store.phoneNumber();
                    if(phoneNumber != null) {
                        executor.submit(() -> linkPhoneNumber(store.clientType(), store.uuid(), phoneNumber));
                    }
                }
            } finally {
                storeLock.unlock(store.uuid());
            }
        }

        private void encodeStore(WhatsappStore store, Path path) {
            try {
                var tempFile = Files.createTempFile(path.getFileName().toString(), ".tmp");
                try(var stream = Files.newOutputStream(tempFile)) {
                    StoreSpec.encode(store, ProtobufOutputStream.toStream(stream));
                }
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
            }catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        private void serializeChat(WhatsappStore store, Chat chat) {
            Objects.requireNonNull(store, "store cannot be null");
            Objects.requireNonNull(chat, "chat cannot be null");

            var outputFile = getMessagesContainerPathIfUpdated(store, chat.jid(), chat.hashCode(), CHAT_PREFIX);
            if (outputFile == null) {
                return;
            }

            try {
                var tempFile = Files.createTempFile(outputFile.getFileName().toString(), ".tmp");
                try(var stream = Files.newOutputStream(tempFile)) {
                    ChatSpec.encode(chat, ProtobufOutputStream.toStream(stream));
                }
                Files.move(tempFile, outputFile, StandardCopyOption.REPLACE_EXISTING);

            }catch (Throwable throwable) {
                handleSerializeError(outputFile, throwable);
            }
        }

        private void serializeNewsletter(WhatsappStore store, Newsletter newsletter) {
            Objects.requireNonNull(store, "store cannot be null");
            Objects.requireNonNull(newsletter, "newsletter cannot be null");

            var outputFile = getMessagesContainerPathIfUpdated(store, newsletter.jid(), newsletter.hashCode(), NEWSLETTER_PREFIX);
            if (outputFile == null) {
                return;
            }

            try {
                var tempFile = Files.createTempFile(outputFile.getFileName().toString(), ".tmp");
                try(var stream = Files.newOutputStream(tempFile)) {
                    NewsletterSpec.encode(newsletter, ProtobufOutputStream.toStream(stream));
                }
                Files.move(tempFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
            }catch (Throwable throwable) {
                handleSerializeError(outputFile, throwable);
            }
        }

        private Path getMessagesContainerPathIfUpdated(WhatsappStore store, Jid jid, int hashCode, String filePrefix) {
            var identifier = new StoreJidPair(store.uuid(), jid);
            var oldHashCode = jidsHashCodes.getOrDefault(identifier, -1);
            if (oldHashCode == hashCode) {
                return null;
            }

            jidsHashCodes.put(identifier, hashCode);
            var fileName = filePrefix + jid.user() + ".proto";
            return getSessionFile(store, fileName);
        }

        private void handleSerializeError(Path path, Throwable error) {
            var logger = System.getLogger("FileSerializer - " + path);
            logger.log(System.Logger.Level.ERROR, error);
        }

        @Override
        public Optional<WhatsappStore> startDeserialize(WhatsappClientType type, UUID id) {
            Objects.requireNonNull(type, "type cannot be null");
            Objects.requireNonNull(id, "id cannot be null");

            return deserializeStoreFromId(type, id.toString());
        }

        @Override
        public Optional<WhatsappStore> startDeserialize(WhatsappClientType type, Long phoneNumber) {
            Objects.requireNonNull(type, "type cannot be null");
            Objects.requireNonNull(phoneNumber, "phoneNumber cannot be null");

            var file = getSessionDirectory(type, phoneNumber.toString());
            if (Files.notExists(file)) {
                return Optional.empty();
            }

            try {
                return deserializeStoreFromId(type, Files.readString(file));
            } catch (IOException exception) {
                return Optional.empty();
            }
        }

        private Optional<WhatsappStore> deserializeStoreFromId(WhatsappClientType type, String id) {
            var path = getSessionFile(type, id, "store.proto");
            if (Files.notExists(path)) {
                return Optional.empty();
            }

            try(var stream = Files.newInputStream(path)) {
                var store = StoreSpec.decode(ProtobufInputStream.fromStream(stream));
                startAttribute(store);
                storesHashCodes.put(store.uuid(), store.hashCode());
                return Optional.of(store);
            } catch (IOException exception) {
                return Optional.empty();
            }
        }

        private void startAttribute(WhatsappStore store) {
            var task = Thread.startVirtualThread(() -> deserializeChatsAndNewsletters(store));
            storesAttributions.put(store.uuid(), task);
        }

        private void deserializeChatsAndNewsletters(WhatsappStore store) {
            var directory = getSessionDirectory(store.clientType(), store.uuid().toString());
            try (var walker = Files.walk(directory); var executor = newVirtualThreadPerTaskExecutor()) {
                walker.forEach(path -> executor.submit(() -> deserializeChatOrNewsletter(store, path)));
            } catch (IOException exception) {
                throw new RuntimeException("Cannot attribute store", exception);
            }
            attributeStoreContextualMessages(store);
        }

        private void deserializeChatOrNewsletter(WhatsappStore store, Path path) {
            try {
                var fileName = path.getFileName().toString();
                if(fileName.startsWith(CHAT_PREFIX)) {
                    deserializeChat(store, path);
                }else if(fileName.startsWith(NEWSLETTER_PREFIX)) {
                    deserializeNewsletter(store, path);
                }
            }catch (Throwable throwable) {
                handleSerializeError(path, throwable);
            }
        }

        private void deserializeChat(WhatsappStore store, Path chatFile) {
            try(var stream = Files.newInputStream(chatFile)) {
                var chat = ChatSpec.decode(ProtobufInputStream.fromStream(stream));
                var storeJidPair = new StoreJidPair(store.uuid(), chat.jid());
                jidsHashCodes.put(storeJidPair, chat.hashCode());
                for (var message : chat.messages()) {
                    message.messageInfo().setChat(chat);
                    store.findContactByJid(message.messageInfo().senderJid())
                            .ifPresent(message.messageInfo()::setSender);
                }
                store.addChat(chat);
            } catch (IOException exception) {
                try {
                    Files.deleteIfExists(chatFile);
                } catch (IOException ignored) {

                }
                var chatName = chatFile.getFileName().toString()
                        .replaceFirst(CHAT_PREFIX, "")
                        .replace(".proto", "");
                store.addNewChat(Jid.of(chatName));
            }
        }

        private void deserializeNewsletter(WhatsappStore store, Path newsletterFile) {
            try(var stream = Files.newInputStream(newsletter)) {
                var newsletter = NewsletterSpec.decode(ProtobufInputStream.fromStream(stream));
                var storeJidPair = new StoreJidPair(store.uuid(), newsletter.jid());
                jidsHashCodes.put(storeJidPair, newsletter.hashCode());
                for (var message : newsletter.messages()) {
                    message.setNewsletter(newsletter);
                }
                store.addNewsletter(newsletter);
            } catch (IOException exception) {
                try {
                    Files.deleteIfExists(newsletterFile);
                } catch (IOException ignored) {

                }
                var newsletterName = newsletterFile.getFileName().toString()
                        .replaceFirst(CHAT_PREFIX, "")
                        .replace(".proto", "");
                store.addNewNewsletter(Jid.of(newsletterName));
            }
        }

        @Override
        public void finishDeserialize(WhatsappStore store) {
            Objects.requireNonNull(store, "store cannot be null");

            var task = storesAttributions.get(store.uuid());
            if(task == null) {
                return;
            }

            try {
                task.join();
            }catch (InterruptedException exception) {
                throw new RuntimeException("Cannot finish deserializing store", exception);
            }
        }

        // Do this after we have all the chats, or it won't work for obvious reasons
        private void attributeStoreContextualMessages(WhatsappStore store) {
            store.chats()
                    .parallelStream()
                    .map(Chat::messages)
                    .flatMap(Collection::parallelStream)
                    .forEach(message -> attributeStoreContextualMessage(store, message));
        }

        private void attributeStoreContextualMessage(WhatsappStore store, HistorySyncMessage message) {
            message.messageInfo()
                    .message()
                    .contentWithContext()
                    .flatMap(ContextualMessage::contextInfo)
                    .ifPresent(contextInfo -> attributeStoreContextInfo(store, contextInfo));
        }

        private void attributeStoreContextInfo(WhatsappStore store, ContextInfo contextInfo) {
            contextInfo.quotedMessageParentJid()
                    .flatMap(store::findChatByJid)
                    .ifPresent(contextInfo::setQuotedMessageParent);
            contextInfo.quotedMessageSenderJid()
                    .flatMap(store::findContactByJid)
                    .ifPresent(contextInfo::setQuotedMessageSender);
        }

        @Override
        public void deleteSession(WhatsappClientType type, UUID uuid) {
            Objects.requireNonNull(type, "type cannot be null");
            Objects.requireNonNull(uuid, "uuid cannot be null");

            try {
                var folderPath = getSessionDirectory(type, uuid.toString());
                delete(folderPath);
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot delete session", exception);
            }
        }

        private void delete(Path path) throws IOException {
            if(Files.notExists(path)) {
                return;
            }

            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        private Object linkPhoneNumber(WhatsappClientType type, UUID uuid, long phoneNumber) {
            try {
                var link = getSessionDirectory(type, String.valueOf(phoneNumber));
                Files.writeString(link, uuid.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException ignored) {

            }
            return null;
        }

        private Path getHome(WhatsappClientType type) {
            return baseDirectory.resolve(type == WhatsappClientType.MOBILE ? "mobile" : "web");
        }

        private Path getSessionDirectory(WhatsappClientType clientType, String path) {
            try {
                var result = getHome(clientType).resolve(path);
                Files.createDirectories(result.getParent());
                return result;
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        private Path getSessionFile(WhatsappStore store, String fileName) {
            try {
                var result = getSessionFile(store.clientType(), store.uuid().toString(), fileName);
                Files.createDirectories(result.getParent());
                return result;
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot create directory", exception);
            }
        }

        private Path getSessionFile(WhatsappClientType clientType, String uuid, String fileName) {
            try {
                var result = getSessionDirectory(clientType, uuid).resolve(fileName);
                Files.createDirectories(result.getParent());
                return result;
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot create directory", exception);
            }
        }

        private record StoreJidPair(UUID storeId, Jid jid) {

        }

        private final static class ReentrantKeyedLock {
            private final ConcurrentMap<UUID, ReentrantLock> locks;
            private ReentrantKeyedLock() {
                this.locks = new ConcurrentHashMap<>();
            }

            private void lock(UUID key) {
                var lockWrapper = locks.compute(
                        key,
                        (ignored, value) -> requireNonNullElseGet(value, () -> new ReentrantLock(true))
                );
                lockWrapper.lock();
            }

            private void unlock(UUID key) {
                var lockWrapper = locks.get(key);
                if(lockWrapper == null || !lockWrapper.isHeldByCurrentThread()){
                    throw new IllegalStateException("The lock for the key %s doesn't exist or is not held by the current thread".formatted(key));
                }
                lockWrapper.unlock();
            }
        }
    }
}