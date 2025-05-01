package it.auties.whatsapp.controller;

import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatBuilder;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.sync.HistorySyncMessage;
import it.auties.whatsapp.util.ImmutableLinkedList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class FileControllerSerializer implements ControllerSerializer {
    private static final String CHAT_PREFIX = "chat_";
    private static final String NEWSLETTER_PREFIX = "newsletter_";
    
    private final Path baseDirectory;
    private int keysHashCode;
    private int storeHashCode;
    private final ConcurrentMap<Jid, Integer> jidsHashCodes;
    FileControllerSerializer(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.keysHashCode = -1;
        this.storeHashCode = -1;
        this.jidsHashCodes = new ConcurrentHashMap<>();
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
    public LinkedList<UUID> listIds(ClientType type) {
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
    public LinkedList<PhoneNumber> listPhoneNumbers(ClientType type) {
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
            var longValue = Long.parseLong(file.getFileName().toString());
            return PhoneNumber.ofNullable(longValue);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Void> serializeKeys(Keys keys, boolean async) {
        var newHashCode = keys.hashCode();
        if(newHashCode == keysHashCode) {
            return CompletableFuture.completedFuture(null);
        }

        this.keysHashCode = newHashCode;
        var keysName = "keys" + fileExtension();
        var outputFile = getSessionFile(keys.clientType(), keys.uuid().toString(), keysName);
        if (async) {
            return CompletableFuture.runAsync(() -> encodeKeys(keys, outputFile))
                    .exceptionallyAsync(error -> onError(outputFile, error));
        }

        encodeKeys(keys, outputFile);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> serializeStore(Store store, boolean async) {
        var newHashCode = store.hashCode();
        if(newHashCode == storeHashCode) {
            return CompletableFuture.completedFuture(null);
        }

        this.storeHashCode = newHashCode;
        var chatsFutures = serializeChatsAsync(store);
        var newslettersFutures = serializeNewslettersAsync(store);
        var dependableFutures = Stream.of(chatsFutures, newslettersFutures)
                .flatMap(Arrays::stream)
                .toArray(CompletableFuture[]::new);
        var result = CompletableFuture.allOf(dependableFutures).thenRunAsync(() -> {
            var storeName = "store" + fileExtension();
            var storePath = getSessionFile(store, storeName);
            encodeStore(store, storePath);
        });
        if (async) {
            return result;
        }

        result.join();
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<?>[] serializeChatsAsync(Store store) {
        return store.chats()
                .stream()
                .map(chat -> serializeChatAsync(store, chat))
                .toArray(CompletableFuture[]::new);
    }

    private CompletableFuture<Void> serializeChatAsync(Store store, Chat chat) {
        var newHashCode = chat.hashCode();
        if(newHashCode == jidsHashCodes.getOrDefault(chat.jid(), -1)) {
            return CompletableFuture.completedFuture(null);
        }

        jidsHashCodes.put(chat.jid(), newHashCode);
        var fileName = CHAT_PREFIX + chat.jid().user() + fileExtension();
        var outputFile = getSessionFile(store, fileName);
        return CompletableFuture.runAsync(() -> encodeChat(chat, outputFile))
                .exceptionallyAsync(error -> onError(outputFile, error));
    }

    private Void onError(Path path, Throwable error) {
        var logger = System.getLogger("FileSerializer - " + path);
        logger.log(System.Logger.Level.ERROR, error);
        return null;
    }

    private CompletableFuture<?>[] serializeNewslettersAsync(Store store) {
        return store.newsletters()
                .stream()
                .map(newsletter -> serializeNewsletterAsync(store, newsletter))
                .toArray(CompletableFuture[]::new);
    }

    private CompletableFuture<Void> serializeNewsletterAsync(Store store, Newsletter newsletter) {
        var newHashCode = newsletter.hashCode();
        if(newHashCode == jidsHashCodes.getOrDefault(newsletter.jid(), -1)) {
            return CompletableFuture.completedFuture(null);
        }

        jidsHashCodes.put(newsletter.jid(), newHashCode);
        var fileName = NEWSLETTER_PREFIX + newsletter.jid().user() + fileExtension();
        var outputFile = getSessionFile(store, fileName);
        return CompletableFuture.runAsync(() -> encodeNewsletter(newsletter, outputFile));
    }

    @Override
    public Optional<Keys> deserializeKeys(ClientType type, UUID id) {
        return deserializeKeysFromId(type, id.toString());
    }

    @Override
    public Optional<Keys> deserializeKeys(ClientType type, String alias) {
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
    public Optional<Keys> deserializeKeys(ClientType type, long phoneNumber) {
        var file = getSessionDirectory(type, String.valueOf(phoneNumber));
        if (Files.notExists(file)) {
            return Optional.empty();
        }

        try {
            return deserializeKeysFromId(type, Files.readString(file));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private Optional<Keys> deserializeKeysFromId(ClientType type, String id) {
        var path = getSessionFile(type, id, "keys.proto");
        if(Files.notExists(path)) {
            return Optional.empty();
        }

        try {
            var keys = decodeKeys(path);
            keysHashCode = keys.hashCode();
            return Optional.of(keys);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Store> deserializeStore(ClientType type, UUID id) {
        return deserializeStoreFromId(type, id.toString());
    }

    @Override
    public Optional<Store> deserializeStore(ClientType type, String alias) {
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
    public Optional<Store> deserializeStore(ClientType type, long phoneNumber) {
        var file = getSessionDirectory(type, String.valueOf(phoneNumber));
        if (Files.notExists(file)) {
            return Optional.empty();
        }

        try {
            return deserializeStoreFromId(type, Files.readString(file));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private Optional<Store> deserializeStoreFromId(ClientType type, String id) {
        var path = getSessionFile(type, id, "store.proto");
        if (Files.notExists(path)) {
            return Optional.empty();
        }

        try {
            var store = decodeStore(path);
            storeHashCode = store.hashCode();
            return Optional.of(store);
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Void> attributeStore(Store store) {
        var directory = getSessionDirectory(store.clientType(), store.uuid().toString());
        if (Files.notExists(directory)) {
            return CompletableFuture.completedFuture(null);
        }
        try (var walker = Files.walk(directory)) {
            var futures = walker.map(entry -> handleStoreFile(store, entry))
                    .filter(Objects::nonNull)
                    .toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures)
                    .thenRun(() -> attributeStoreContextualMessages(store));
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    // Do this after we have all the chats, or it won't work for obvious reasons
    private void attributeStoreContextualMessages(Store store) {
        store.chats()
                .stream()
                .flatMap(chat -> chat.messages().stream())
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

    private CompletableFuture<Void> handleStoreFile(Store store, Path entry) {
        return switch (FileType.of(entry)) {
            case NEWSLETTER -> CompletableFuture.runAsync(() -> deserializeNewsletter(store, entry))
                    .exceptionallyAsync(error -> onError(entry, error));
            case CHAT -> CompletableFuture.runAsync(() -> deserializeChat(store, entry))
                    .exceptionallyAsync(error -> onError(entry, error));
            case UNKNOWN -> null;
        };
    }

    private enum FileType {
        UNKNOWN(null),
        CHAT(CHAT_PREFIX),
        NEWSLETTER(NEWSLETTER_PREFIX);

        private final String prefix;

        FileType(String prefix) {
            this.prefix = prefix;
        }

        private static FileType of(Path path) {
            return Arrays.stream(values())
                    .filter(entry -> entry.prefix() != null && path.getFileName().toString().startsWith(entry.prefix()))
                    .findFirst()
                    .orElse(UNKNOWN);
        }

        private String prefix() {
            return prefix;
        }
    }

    @Override
    public void deleteSession(Controller<?> controller) {
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

    @Override
    public void linkMetadata(Controller<?> controller) {
        controller.phoneNumber()
                .ifPresent(phoneNumber -> linkToUuid(controller.clientType(), controller.uuid(), phoneNumber.toString()));
        controller.alias()
                .forEach(alias -> linkToUuid(controller.clientType(), controller.uuid(), alias));
    }

    private void linkToUuid(ClientType type, UUID uuid, String string) {
        try {
            var link = getSessionDirectory(type, string);
            Files.writeString(link, uuid.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {

        }
    }

    private void deserializeChat(Store store, Path chatFile) {
        try {
            var chat = decodeChat(chatFile);
            jidsHashCodes.put(chat.jid(), chat.hashCode());
            for (var message : chat.messages()) {
                message.messageInfo().setChat(chat);
                store.findContactByJid(message.messageInfo().senderJid())
                        .ifPresent(message.messageInfo()::setSender);
            }
            store.addChatDirect(chat);
        } catch (IOException exception) {
            store.addChatDirect(rescueChat(chatFile));
        }
    }

    private Chat rescueChat(Path entry) {
        try {
            Files.deleteIfExists(entry);
        } catch (IOException ignored) {

        }
        var chatName = entry.getFileName().toString()
                .replaceFirst(CHAT_PREFIX, "")
                .replace(fileExtension(), "");
        return new ChatBuilder()
                .jid(Jid.of(chatName))
                .build();
    }

    private void deserializeNewsletter(Store store, Path newsletterFile) {
        try {
            var newsletter = decodeNewsletter(newsletterFile);
            jidsHashCodes.put(newsletter.jid(), newsletter.hashCode());
            for (var message : newsletter.messages()) {
                message.setNewsletter(newsletter);
            }
            store.addNewsletter(newsletter);
        } catch (IOException exception) {
            store.addNewsletter(rescueNewsletter(newsletterFile));
        }
    }

    private Newsletter rescueNewsletter(Path entry) {
        try {
            Files.deleteIfExists(entry);
        } catch (IOException ignored) {

        }
        var newsletterName = entry.getFileName().toString()
                .replaceFirst(CHAT_PREFIX, "")
                .replace(fileExtension(), "");
        return new Newsletter(Jid.of(newsletterName), null, null, null);
    }

    private Path getHome(ClientType type) {
        return baseDirectory.resolve(type == ClientType.MOBILE ? "mobile" : "web");
    }

    private Path getSessionDirectory(ClientType clientType, String path) {
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

    private Path getSessionFile(ClientType clientType, String uuid, String fileName) {
        try {
            var result = getSessionDirectory(clientType, uuid).resolve(fileName);
            Files.createDirectories(result.getParent());
            return result;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot create directory", exception);
        }
    }
}
