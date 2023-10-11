package it.auties.whatsapp.controller;

import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatBuilder;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.util.Smile;
import it.auties.whatsapp.util.Validate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The default serializer
 * It uses smile to serialize all the data locally
 * The store and the keys are decoded synchronously, but the store's chat are decoded asynchronously to save time
 */
public class DefaultControllerSerializer implements ControllerSerializer {
    private static final Path DEFAULT_DIRECTORY = Path.of(System.getProperty("user.home") + "/.cobalt/");
    private static final String CHAT_PREFIX = "chat_";
    private static final String NEWSLETTER_PREFIX = "newsletter_";
    private static final String STORE_NAME = "store.smile";
    private static final String KEYS_NAME = "keys.smile";
    private static final ControllerSerializer DEFAULT_SERIALIZER = new DefaultControllerSerializer();

    private final Path baseDirectory;
    private final Map<UUID, CompletableFuture<Void>> attributeStoreSerializers;
    private LinkedList<UUID> cachedUuids;
    private LinkedList<PhoneNumber> cachedPhoneNumbers;

    public static ControllerSerializer instance() {
        return DEFAULT_SERIALIZER;
    }

    /**
     * Creates a provider using the default path
     */
    private DefaultControllerSerializer() {
        this(DEFAULT_DIRECTORY);
    }

    /**
     * Creates a provider using the specified path
     *
     * @param baseDirectory the non-null directory where data will be serialized
     */
    public DefaultControllerSerializer(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.attributeStoreSerializers = new ConcurrentHashMap<>();
        try {
            Files.createDirectories(baseDirectory);
            Validate.isTrue(Files.isDirectory(baseDirectory), "Expected a directory as base path: %s", baseDirectory);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public LinkedList<UUID> listIds(ClientType type) {
        if (cachedUuids != null) {
            return cachedUuids;
        }

        var directory = getHome(type);
        if(Files.notExists(directory)) {
            return new LinkedList<>();
        }

        try (var walker = Files.walk(directory, 1).sorted(Comparator.comparing(this::getLastModifiedTime))) {
            return cachedUuids = walker.map(this::parsePathAsId)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toCollection(LinkedList::new));
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot list known ids", exception);
        }
    }

    @Override
    public LinkedList<PhoneNumber> listPhoneNumbers(ClientType type) {
        if (cachedPhoneNumbers != null) {
            return cachedPhoneNumbers;
        }

        var directory = getHome(type);
        if(Files.notExists(directory)) {
            return new LinkedList<>();
        }

        try (var walker = Files.walk(directory, 1).sorted(Comparator.comparing(this::getLastModifiedTime))) {
            return cachedPhoneNumbers = walker.map(this::parsePathAsPhoneNumber)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toCollection(LinkedList::new));
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot list known ids", exception);
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
        if (cachedUuids != null && !cachedUuids.contains(keys.uuid())) {
            cachedUuids.add(keys.uuid());
        }

        var outputFile = getSessionFile(keys.clientType(), keys.uuid().toString(), KEYS_NAME);
        if (async) {
            return CompletableFuture.runAsync(() -> writeFile(keys, KEYS_NAME, outputFile));
        }

        writeFile(keys, KEYS_NAME, outputFile);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> serializeStore(Store store, boolean async) {
        if (cachedUuids != null && !cachedUuids.contains(store.uuid())) {
            cachedUuids.add(store.uuid());
        }

        var phoneNumber = store.phoneNumber().orElse(null);
        if (cachedPhoneNumbers != null && !cachedPhoneNumbers.contains(phoneNumber)) {
            cachedPhoneNumbers.add(phoneNumber);
        }

        var task = attributeStoreSerializers.get(store.uuid());
        if (task != null && !task.isDone()) {
            return task;
        }

        var chatsFutures = serializeChatsAsync(store);
        var newslettersFutures = serializeNewslettersAsync(store);
        var dependableFutures = Stream.of(chatsFutures, newslettersFutures)
                .flatMap(Arrays::stream)
                .toArray(CompletableFuture[]::new);
        var result = CompletableFuture.allOf(dependableFutures).thenRunAsync(() -> {
            var storePath = getSessionFile(store, STORE_NAME);
            writeFile(store, STORE_NAME, storePath);
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
        if(!chat.hasUpdate()) {
            return CompletableFuture.completedFuture(null);
        }

        var fileName = CHAT_PREFIX + chat.jid() + ".smile";
        var outputFile = getSessionFile(store, fileName);
        return CompletableFuture.runAsync(() -> writeFile(chat, fileName, outputFile));
    }

    private CompletableFuture<?>[] serializeNewslettersAsync(Store store) {
        return store.newsletters()
                .stream()
                .map(newsletter -> serializeNewsletterAsync(store, newsletter))
                .toArray(CompletableFuture[]::new);
    }

    private CompletableFuture<Void> serializeNewsletterAsync(Store store, Newsletter newsletter) {
        var fileName = NEWSLETTER_PREFIX + newsletter.jid() + ".smile";
        var outputFile = getSessionFile(store, fileName);
        return CompletableFuture.runAsync(() -> writeFile(newsletter, fileName, outputFile));
    }

    private void writeFile(Object object, String fileName, Path outputFile) {
        try {
            var tempFile = Files.createTempFile(fileName, ".tmp");
            try (var tempFileOutputStream = new GZIPOutputStream(Files.newOutputStream(tempFile))) {
                Smile.writeValueAsBytes(tempFileOutputStream, object);
                Files.move(tempFile, outputFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
        }catch (IOException exception) {
            throw new UncheckedIOException("Cannot write file", exception);
        }
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
            throw new UncheckedIOException("Cannot read %s".formatted(alias), exception);
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
            throw new UncheckedIOException("Cannot read %s".formatted(phoneNumber), exception);
        }
    }

    private Optional<Keys> deserializeKeysFromId(ClientType type, String id) {
        var path = getSessionFile(type, id, "keys.smile");
        try (var input = new GZIPInputStream(Files.newInputStream(path))) {
            return Optional.of(Smile.readValue(input, Keys.class));
        } catch (IOException exception) {
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
            throw new UncheckedIOException("Cannot read %s".formatted(alias), exception);
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
            throw new UncheckedIOException("Cannot read %s".formatted(phoneNumber), exception);
        }
    }

    private Optional<Store> deserializeStoreFromId(ClientType type, String id) {
        var path = getSessionFile(type, id, "store.smile");
        try (var input = new GZIPInputStream(Files.newInputStream(path))) {
            return Optional.of(Smile.readValue(input, Store.class));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Void> attributeStore(Store store) {
        var oldTask = attributeStoreSerializers.get(store.uuid());
        if (oldTask != null) {
            return oldTask;
        }
        var directory = getSessionDirectory(store.clientType(), store.uuid().toString());
        if (Files.notExists(directory)) {
            return CompletableFuture.completedFuture(null);
        }
        try (var walker = Files.walk(directory)) {
            var futures = walker.map(entry -> handleStoreFile(store, entry))
                    .filter(Objects::nonNull)
                    .toArray(CompletableFuture[]::new);
            var result = CompletableFuture.allOf(futures);
            attributeStoreSerializers.put(store.uuid(), result);
            return result;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot deserialize store", exception);
        }
    }

    private CompletableFuture<Void> handleStoreFile(Store store, Path entry) {
        return switch (FileType.of(entry)) {
            case UNKNOWN -> null;
            case NEWSLETTER -> CompletableFuture.runAsync(() -> deserializeNewsletter(store, entry));
            case CHAT -> CompletableFuture.runAsync(() -> deserializeChat(store, entry));
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
        }catch (IOException exception) {
            throw new UncheckedIOException("Cannot delete session", exception);
        }
    }

    private Path delete(Path path) throws IOException {
        return Files.walkFileTree(path, new SimpleFileVisitor<>() {
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
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot link %s to %s".formatted(string, uuid), exception);
        }
    }

    private void deserializeChat(Store store, Path chatFile) {
        try (var input = new GZIPInputStream(Files.newInputStream(chatFile))) {
            store.addChatDirect(Smile.readValue(input, Chat.class));
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
                .replace(".smile", "")
                .replaceAll("~~", ":");
        return new ChatBuilder()
                .jid(Jid.of(chatName))
                .build();
    }

    private void deserializeNewsletter(Store store, Path newsletterFile) {
        try (var input = new GZIPInputStream(Files.newInputStream(newsletterFile))) {
            store.addNewsletter(Smile.readValue(input, Newsletter.class));
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
                .replace(".smile", "")
                .replaceAll("~~", ":");
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
       }catch (IOException exception) {
           throw new UncheckedIOException(exception);
       }
    }

    private Path getSessionFile(Store store, String fileName) {
        try {
            var fixedName = fileName.replaceAll(":", "~~");
            var result = getSessionFile(store.clientType(), store.uuid().toString(), fixedName);
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
