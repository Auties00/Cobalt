package com.github.auties00.cobalt.store;

import com.github.auties00.cobalt.client.WhatsAppClientType;
import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.chat.ChatSpec;
import com.github.auties00.cobalt.model.info.ContextInfo;
import com.github.auties00.cobalt.model.info.MessageInfo;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.message.model.ContextualMessage;
import com.github.auties00.cobalt.model.newsletter.Newsletter;
import com.github.auties00.cobalt.model.newsletter.NewsletterSpec;
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

final class ProtobufStoreSerializer implements WhatsappStoreSerializer {
    private static final String CHAT_PREFIX = "chat_";
    private static final String NEWSLETTER_PREFIX = "newsletter_";
    private static final Path DEFAULT_SERIALIZER_PATH = Path.of(System.getProperty("user.home") + "/.cobalt/");


    private final Path baseDirectory;
    private final ConcurrentMap<UUID, Integer> storesHashCodes;
    private final ConcurrentMap<UUID, Thread> storesAttributions;
    private final ConcurrentMap<StoreJidPair, Integer> jidsHashCodes;
    private final ReentrantKeyedLock storeLock;

    ProtobufStoreSerializer() {
        this(DEFAULT_SERIALIZER_PATH);
    }

    ProtobufStoreSerializer(Path baseDirectory) {
        Objects.requireNonNull(baseDirectory, "baseDirectory cannot be null");
        this.baseDirectory = baseDirectory;
        this.storesHashCodes = new ConcurrentHashMap<>();
        this.storesAttributions = new ConcurrentHashMap<>();
        this.jidsHashCodes = new ConcurrentHashMap<>();
        this.storeLock = new ReentrantKeyedLock();
    }

    @Override
    public SequencedCollection<UUID> listIds(WhatsAppClientType type) {
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
    public SequencedCollection<Long> listPhoneNumbers(WhatsAppClientType type) {
        return list(type, file -> {
            try {
                var fileName = file.getFileName().toString();
                if (fileName.isEmpty()) {
                    return Optional.empty();
                }
                var result = Long.parseUnsignedLong(fileName, fileName.charAt(0) == '+' ? 1 : 0, fileName.length(), 10);
                return Optional.of(result);
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        });
    }

    private <T> SequencedCollection<T> list(WhatsAppClientType type, Function<Path, Optional<T>> adapter) {
        Objects.requireNonNull(type, "type cannot be null");

        var directory = getHome(type);
        if (Files.notExists(directory)) {
            return List.of();
        }

        try (var walker = Files.walk(directory, 1)
                .sorted(Comparator.comparing(this::getLastModifiedTime))) {
            return walker.map(adapter)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toCollection(LinkedList::new));
        } catch (IOException exception) {
            return List.of();
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
    public void serialize(WhatsAppStore store) {
        Objects.requireNonNull(store, "store cannot be null");
        try {
            storeLock.lock(store.uuid());
            var oldHashCode = storesHashCodes.getOrDefault(store.uuid(), -1);
            var newHashCode = store.hashCode();
            if (oldHashCode == newHashCode) {
                return;
            }

            storesHashCodes.put(store.uuid(), newHashCode);
            try (var executor = newVirtualThreadPerTaskExecutor()) {
                executor.submit(() -> encodeStore(store, getSessionFile(store, "store" + ".proto")));
                store.chats()
                        .forEach(chat -> executor.submit(() -> serializeChat(store, chat)));
                store.newsletters()
                        .forEach(newsletter -> executor.submit(() -> serializeNewsletter(store, newsletter)));
                var phoneNumber = store.phoneNumber();
                if (phoneNumber.isPresent()) {
                    executor.submit(() -> linkPhoneNumber(store.clientType(), store.uuid(), phoneNumber.getAsLong()));
                }
            }
        } finally {
            storeLock.unlock(store.uuid());
        }
    }

    private void encodeStore(WhatsAppStore store, Path path) {
        try {
            var tempFile = Files.createTempFile(path.getFileName().toString(), ".tmp");
            try (var stream = Files.newOutputStream(tempFile)) {
                WhatsAppStoreSpec.encode(store, ProtobufOutputStream.toStream(stream));
            }
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void serializeChat(WhatsAppStore store, Chat chat) {
        Objects.requireNonNull(store, "store cannot be null");
        Objects.requireNonNull(chat, "chat cannot be null");

        var outputFile = getMessagesContainerPathIfUpdated(store, chat.jid(), chat.hashCode(), CHAT_PREFIX);
        if (outputFile == null) {
            return;
        }

        try {
            var tempFile = Files.createTempFile(outputFile.getFileName().toString(), ".tmp");
            try (var stream = Files.newOutputStream(tempFile)) {
                ChatSpec.encode(chat, ProtobufOutputStream.toStream(stream));
            }
            Files.move(tempFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable throwable) {
            handleSerializeError(outputFile, throwable);
        }
    }

    private void serializeNewsletter(WhatsAppStore store, Newsletter newsletter) {
        Objects.requireNonNull(store, "store cannot be null");
        Objects.requireNonNull(newsletter, "newsletter cannot be null");

        var outputFile = getMessagesContainerPathIfUpdated(store, newsletter.jid(), newsletter.hashCode(), NEWSLETTER_PREFIX);
        if (outputFile == null) {
            return;
        }

        try {
            var tempFile = Files.createTempFile(outputFile.getFileName().toString(), ".tmp");
            try (var stream = Files.newOutputStream(tempFile)) {
                NewsletterSpec.encode(newsletter, ProtobufOutputStream.toStream(stream));
            }
            Files.move(tempFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable throwable) {
            handleSerializeError(outputFile, throwable);
        }
    }

    private Path getMessagesContainerPathIfUpdated(WhatsAppStore store, Jid jid, int hashCode, String filePrefix) {
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
    public Optional<WhatsAppStore> startDeserialize(WhatsAppClientType type, UUID id) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(id, "id cannot be null");

        return deserializeStoreFromId(type, id.toString());
    }

    @Override
    public Optional<WhatsAppStore> startDeserialize(WhatsAppClientType type, Long phoneNumber) {
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

    private Optional<WhatsAppStore> deserializeStoreFromId(WhatsAppClientType type, String id) {
        var path = getSessionFile(type, id, "store.proto");
        if (Files.notExists(path)) {
            return Optional.empty();
        }

        try (var stream = Files.newInputStream(path)) {
            var store = WhatsAppStoreSpec.decode(ProtobufInputStream.fromStream(stream));
            startAttribute(store);
            storesHashCodes.put(store.uuid(), store.hashCode());
            return Optional.of(store);
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private void startAttribute(WhatsAppStore store) {
        var task = Thread.startVirtualThread(() -> deserializeChatsAndNewsletters(store));
        storesAttributions.put(store.uuid(), task);
    }

    private void deserializeChatsAndNewsletters(WhatsAppStore store) {
        var directory = getSessionDirectory(store.clientType(), store.uuid().toString());
        try (var walker = Files.walk(directory); var executor = newVirtualThreadPerTaskExecutor()) {
            walker.forEach(path -> executor.submit(() -> deserializeChatOrNewsletter(store, path)));
        } catch (IOException exception) {
            throw new RuntimeException("Cannot attribute store", exception);
        }
        attributeStoreContextualMessages(store);
    }

    private void deserializeChatOrNewsletter(WhatsAppStore store, Path path) {
        try {
            var fileName = path.getFileName().toString();
            if (fileName.startsWith(CHAT_PREFIX)) {
                deserializeChat(store, path);
            } else if (fileName.startsWith(NEWSLETTER_PREFIX)) {
                deserializeNewsletter(store, path);
            }
        } catch (Throwable throwable) {
            handleSerializeError(path, throwable);
        }
    }

    private void deserializeChat(WhatsAppStore store, Path chatFile) {
        try (var stream = Files.newInputStream(chatFile)) {
            var chat = ChatSpec.decode(ProtobufInputStream.fromStream(stream));
            var storeJidPair = new StoreJidPair(store.uuid(), chat.jid());
            jidsHashCodes.put(storeJidPair, chat.hashCode());
            for (var message : chat.messages()) {
                message.setChat(chat);
                store.findContactByJid(message.senderJid())
                        .ifPresent(message::setSender);
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

    private void deserializeNewsletter(WhatsAppStore store, Path newsletterFile) {
        try (var stream = Files.newInputStream(newsletterFile)) {
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
    public void finishDeserialize(WhatsAppStore store) {
        Objects.requireNonNull(store, "store cannot be null");

        var task = storesAttributions.get(store.uuid());
        if (task == null) {
            return;
        }

        try {
            task.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Cannot finish deserializing store", exception);
        }
    }

    // Do this after we have all the chats, or it won't work for obvious reasons
    private void attributeStoreContextualMessages(WhatsAppStore store) {
        store.chats()
                .parallelStream()
                .map(Chat::messages)
                .flatMap(Collection::parallelStream)
                .forEach(message -> attributeStoreContextualMessage(store, message));
    }

    private void attributeStoreContextualMessage(WhatsAppStore store, MessageInfo message) {
        message.message()
                .contentWithContext()
                .flatMap(ContextualMessage::contextInfo)
                .ifPresent(contextInfo -> attributeStoreContextInfo(store, contextInfo));
    }

    private void attributeStoreContextInfo(WhatsAppStore store, ContextInfo contextInfo) {
        contextInfo.quotedMessageParentJid()
                .flatMap(store::findChatByJid)
                .ifPresent(contextInfo::setQuotedMessageParent);
        contextInfo.quotedMessageSenderJid()
                .flatMap(store::findContactByJid)
                .ifPresent(contextInfo::setQuotedMessageSender);
    }

    @Override
    public void deleteSession(WhatsAppClientType type, UUID uuid) {
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
        if (Files.notExists(path)) {
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

    private Object linkPhoneNumber(WhatsAppClientType type, UUID uuid, long phoneNumber) {
        try {
            var link = getSessionDirectory(type, String.valueOf(phoneNumber));
            Files.writeString(link, uuid.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {

        }
        return null;
    }

    private Path getHome(WhatsAppClientType type) {
        return baseDirectory.resolve(type == WhatsAppClientType.MOBILE ? "mobile" : "web");
    }

    private Path getSessionDirectory(WhatsAppClientType clientType, String path) {
        try {
            var result = getHome(clientType).resolve(path);
            Files.createDirectories(result.getParent());
            return result;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private Path getSessionFile(WhatsAppStore store, String fileName) {
        try {
            var result = getSessionFile(store.clientType(), store.uuid().toString(), fileName);
            Files.createDirectories(result.getParent());
            return result;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot create directory", exception);
        }
    }

    private Path getSessionFile(WhatsAppClientType clientType, String uuid, String fileName) {
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
            if (lockWrapper == null || !lockWrapper.isHeldByCurrentThread()) {
                throw new IllegalStateException("The lock for the key %s doesn't exist or is not held by the current thread".formatted(key));
            }
            lockWrapper.unlock();
        }
    }
}
