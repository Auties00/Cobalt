package it.auties.whatsapp.controller;

import it.auties.whatsapp.api.WhatsappClientType;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatBuilder;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.newsletter.NewsletterBuilder;
import it.auties.whatsapp.model.sync.HistorySyncMessage;
import it.auties.whatsapp.util.ImmutableLinkedList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElseGet;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;

// FIXME: There is a memory leak in here
abstract class FileControllerSerializer implements ControllerSerializer {
    private static final String CHAT_PREFIX = "chat_";
    private static final String NEWSLETTER_PREFIX = "newsletter_";
    private static final Path DEFAULT_SERIALIZER_PATH = Path.of(System.getProperty("user.home") + "/.cobalt/");


    private final Path baseDirectory;
    private final ConcurrentMap<UUID, Integer> keysHashCodes;
    private final ConcurrentMap<UUID, Integer> storesHashCodes;
    private final ConcurrentMap<UUID, Thread> storesAttributions;
    private final ConcurrentMap<StoreJidPair, Integer> jidsHashCodes;
    private final ReentrantKeyedLock keysLock;
    private final ReentrantKeyedLock storeLock;

    FileControllerSerializer() {
        this(DEFAULT_SERIALIZER_PATH);
    }

    FileControllerSerializer(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.keysHashCodes = new ConcurrentHashMap<>();
        this.storesHashCodes = new ConcurrentHashMap<>();
        this.storesAttributions = new ConcurrentHashMap<>();
        this.jidsHashCodes = new ConcurrentHashMap<>();
        this.keysLock = new ReentrantKeyedLock();
        this.storeLock = new ReentrantKeyedLock();
    }

    abstract String fileExtension();
    
    abstract void encodeKeys(Keys keys, Path path);
    abstract void encodeStore(Store store, Path path);
    abstract void encodeChat(Chat chat, Path path);
    abstract void encodeNewsletter(Newsletter newsletter, Path path);

    abstract Keys decodeKeys(Path keys) throws IOException;
    abstract Store decodeStore(Path store) throws IOException;
    abstract Chat decodeChat(Path chat) throws IOException;
    abstract Newsletter decodeNewsletter(Path newsletter) throws IOException;

    @Override
    public LinkedList<UUID> listIds(WhatsappClientType type) {
        var directory = getHome(type);
        if (Files.notExists(directory)) {
            return ImmutableLinkedList.empty();
        }

        try (var walker = Files.walk(directory, 1).sorted(Comparator.comparing(this::getLastModifiedTime))) {
            return walker.map(this::parsePathAsId)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toCollection(LinkedList::new));
        } catch (IOException exception) {
            return ImmutableLinkedList.empty();
        }
    }

    @Override
    public LinkedList<PhoneNumber> listPhoneNumbers(WhatsappClientType type) {
        var directory = getHome(type);
        if (Files.notExists(directory)) {
            return ImmutableLinkedList.empty();
        }

        try (var walker = Files.walk(directory, 1).sorted(Comparator.comparing(this::getLastModifiedTime))) {
            return walker.map(this::parsePathAsPhoneNumber)
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

    private Optional<UUID> parsePathAsId(Path file) {
        try {
            return Optional.of(UUID.fromString(file.getFileName().toString()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private Optional<PhoneNumber> parsePathAsPhoneNumber(Path file) {
        try {
            return PhoneNumber.of(file.getFileName().toString());
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void serializeKeys(Keys keys) {
        try {
            keysLock.lock(keys.uuid());
            var oldHashCode = keysHashCodes.getOrDefault(keys.uuid(), -1);
            var newHashCode = keys.hashCode();
            if(oldHashCode == newHashCode) {
                return;
            }

            keysHashCodes.put(keys.uuid(), newHashCode);
            var keysName = "keys" + fileExtension();
            var outputFile = getSessionFile(keys.clientType(), keys.uuid().toString(), keysName);
            encodeKeys(keys, outputFile);
        }finally {
            keysLock.unlock(keys.uuid());
        }
    }

    @Override
    public void serializeStore(Store store) {
        try {
            storeLock.lock(store.uuid());
            var oldHashCode = storesHashCodes.getOrDefault(store.uuid(), -1);
            var newHashCode = store.hashCode();
            if(oldHashCode == newHashCode) {
                return;
            }

            storesHashCodes.put(store.uuid(), newHashCode);
            try(var executor = newVirtualThreadPerTaskExecutor()) {
                executor.submit(() -> encodeStore(store, getSessionFile(store, "store" + fileExtension())));
                store.chats()
                        .forEach(chat -> executor.submit(() -> serializeChat(store, chat)));
                store.newsletters()
                        .forEach(newsletter -> executor.submit(() -> serializeNewsletter(store, newsletter)));
                store.phoneNumber()
                        .ifPresent(phoneNumber -> executor.submit(() -> linkToUuid(store.clientType(), store.uuid(), phoneNumber.toString())));
                store.alias()
                        .forEach(alias -> executor.submit(() -> linkToUuid(store.clientType(), store.uuid(), alias)));
            }
        } finally {
            storeLock.unlock(store.uuid());
        }
    }

    @SuppressWarnings("SameReturnValue")
    private Object serializeChat(Store store, Chat chat) {
        var outputFile = getMessagesContainerPathIfUpdated(store, chat.jid(), chat.hashCode(), CHAT_PREFIX);
        if (outputFile == null) {
            return null;
        }

        try {
            encodeChat(chat, outputFile);
        }catch (Throwable throwable) {
            handleSerializeError(outputFile, throwable);
        }
        return null;
    }

    @SuppressWarnings("SameReturnValue")
    private Object serializeNewsletter(Store store, Newsletter newsletter) {
        var outputFile = getMessagesContainerPathIfUpdated(store, newsletter.jid(), newsletter.hashCode(), NEWSLETTER_PREFIX);
        if (outputFile == null) {
            return null;
        }

        try {
            encodeNewsletter(newsletter, outputFile);
        }catch (Throwable throwable) {
            handleSerializeError(outputFile, throwable);
        }
        return null;
    }

    private Path getMessagesContainerPathIfUpdated(Store store, Jid jid, int hashCode, String filePrefix) {
        var identifier = new StoreJidPair(store.uuid(), jid);
        var oldHashCode = jidsHashCodes.getOrDefault(identifier, -1);
        if (oldHashCode == hashCode) {
            return null;
        }

        jidsHashCodes.put(identifier, hashCode);
        var fileName = filePrefix + jid.user() + fileExtension();
        return getSessionFile(store, fileName);
    }

    private void handleSerializeError(Path path, Throwable error) {
        var logger = System.getLogger("FileSerializer - " + path);
        logger.log(System.Logger.Level.ERROR, error);
    }

    @Override
    public Optional<Keys> deserializeKeys(WhatsappClientType type, UUID id) {
        return deserializeKeysFromId(type, id.toString());
    }

    @Override
    public Optional<Keys> deserializeKeys(WhatsappClientType type, String alias) {
        var file = getSessionDirectory(type, alias);
        if (Files.notExists(file)) {
            return Optional.empty();
        }

        try {
            return deserializeKeysFromId(type, Files.readString(file));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Keys> deserializeKeys(WhatsappClientType type, PhoneNumber phoneNumber) {
        var file = getSessionDirectory(type, phoneNumber.toString());
        if (Files.notExists(file)) {
            return Optional.empty();
        }

        try {
            return deserializeKeysFromId(type, Files.readString(file));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private Optional<Keys> deserializeKeysFromId(WhatsappClientType type, String id) {
        var path = getSessionFile(type, id, "keys.proto");
        if(Files.notExists(path)) {
            return Optional.empty();
        }

        try {
            var keys = decodeKeys(path);
            keysHashCodes.put(keys.uuid(), keys.hashCode());
            return Optional.of(keys);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Store> deserializeStore(WhatsappClientType type, UUID id) {
        return deserializeStoreFromId(type, id.toString());
    }

    @Override
    public Optional<Store> deserializeStore(WhatsappClientType type, String alias) {
        var file = getSessionDirectory(type, alias);
        if (Files.notExists(file)) {
            return Optional.empty();
        }

        try {
            return deserializeStoreFromId(type, Files.readString(file));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Store> deserializeStore(WhatsappClientType type, PhoneNumber phoneNumber) {
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

    private Optional<Store> deserializeStoreFromId(WhatsappClientType type, String id) {
        var path = getSessionFile(type, id, "store.proto");
        if (Files.notExists(path)) {
            return Optional.empty();
        }

        try {
            var store = decodeStore(path);
            startAttribute(store);
            storesHashCodes.put(store.uuid(), store.hashCode());
            return Optional.of(store);
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private void startAttribute(Store store) {
        var task = Thread.startVirtualThread(() -> deserializeChatsAndNewsletters(store));
        storesAttributions.put(store.uuid(), task);
    }

    private void deserializeChatsAndNewsletters(Store store) {
        var directory = getSessionDirectory(store.clientType(), store.uuid().toString());
        try (var walker = Files.walk(directory); var executor = newVirtualThreadPerTaskExecutor()) {
            walker.forEach(path -> executor.submit(() -> deserializeChatOrNewsletter(store, path)));
        } catch (IOException exception) {
            throw new RuntimeException("Cannot attribute store", exception);
        }
        attributeStoreContextualMessages(store);
    }

    private Object deserializeChatOrNewsletter(Store store, Path path) {
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
        return null;
    }

    private void deserializeChat(Store store, Path chatFile) {
        try {
            var chat = decodeChat(chatFile);
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
                    .replace(fileExtension(), "");
            store.addChat(new ChatBuilder()
                    .jid(Jid.of(chatName))
                    .build());
        }
    }

    private void deserializeNewsletter(Store store, Path newsletterFile) {
        try {
            var newsletter = decodeNewsletter(newsletterFile);
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
                    .replace(fileExtension(), "");
            store.addNewsletter(new NewsletterBuilder()
                    .jid(Jid.of(newsletterName))
                    .build());
        }
    }

    @Override
    public void finishDeserializeStore(Store store) {
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
    private void attributeStoreContextualMessages(Store store) {
        store.chats()
                .parallelStream()
                .map(Chat::messages)
                .flatMap(Collection::parallelStream)
                .forEach(message -> attributeStoreContextualMessage(store, message));
    }

    private void attributeStoreContextualMessage(Store store, HistorySyncMessage message) {
        message.messageInfo()
                .message()
                .contentWithContext()
                .flatMap(ContextualMessage::contextInfo)
                .ifPresent(contextInfo -> attributeStoreContextInfo(store, contextInfo));
    }

    private void attributeStoreContextInfo(Store store, ContextInfo contextInfo) {
        contextInfo.quotedMessageChatJid()
                .flatMap(store::findChatByJid)
                .ifPresent(contextInfo::setQuotedMessageChat);
        contextInfo.quotedMessageSenderJid()
                .flatMap(store::findContactByJid)
                .ifPresent(contextInfo::setQuotedMessageSender);
    }

    @Override
    public void deleteSession(Controller controller) {
        try {
            var folderPath = getSessionDirectory(controller.clientType(), controller.uuid().toString());
            delete(folderPath);
            var phoneNumber = controller.phoneNumber().orElse(null);
            if (phoneNumber == null) {
                return;
            }
            var linkedFolderPath = getSessionDirectory(controller.clientType(), phoneNumber.toString());
            Files.deleteIfExists(linkedFolderPath);
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

    private Object linkToUuid(WhatsappClientType type, UUID uuid, String string) {
        try {
            var link = getSessionDirectory(type, string);
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

    private Path getSessionFile(Store store, String fileName) {
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
