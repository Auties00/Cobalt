package it.auties.whatsapp.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.util.Smile;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;

/**
 * The default serializer
 * It uses smile to serialize all the data locally
 * The store and the keys are decoded synchronously, but the store's chat are decoded asynchronously to save time
 */
public class DefaultControllerSerializer implements ControllerSerializer {
    private static final Path DEFAULT_DIRECTORY = Path.of(System.getProperty("user.home") + "/.whatsapp4j/");
    private static final String CHAT_PREFIX = "chat_";
    private static final ControllerSerializer DEFAULT_SERIALIZER = new DefaultControllerSerializer();

    private final Path baseDirectory;
    private final Logger logger;
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
    public DefaultControllerSerializer(@NonNull Path baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.logger = System.getLogger("DefaultSerializer");
        this.attributeStoreSerializers = new ConcurrentHashMap<>();
        try {
            Files.createDirectories(baseDirectory);
            Validate.isTrue(Files.isDirectory(baseDirectory), "Expected a directory as base path: %s", baseDirectory);
        } catch (IOException exception) {
            logger.log(WARNING, "Cannot create base directory at %s: %s".formatted(baseDirectory, exception.getMessage()));
        }
    }

    @Override
    public LinkedList<UUID> listIds(@NonNull ClientType type) {
        if (cachedUuids != null) {
            return cachedUuids;
        }

        try (var walker = Files.walk(getHome(type), 1).sorted(Comparator.comparing(this::getLastModifiedTime))) {
            return cachedUuids = walker.map(this::parsePathAsId)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toCollection(LinkedList::new));
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot list known ids", exception);
        }
    }

    @Override
    public LinkedList<PhoneNumber> listPhoneNumbers(@NonNull ClientType type) {
        if (cachedPhoneNumbers != null) {
            return cachedPhoneNumbers;
        }

        try (var walker = Files.walk(getHome(type), 1).sorted(Comparator.comparing(this::getLastModifiedTime))) {
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
    public void serializeKeys(Keys keys, boolean async) {
        if (cachedUuids != null && !cachedUuids.contains(keys.uuid())) {
            cachedUuids.add(keys.uuid());
        }

        var path = getSessionFile(keys.clientType(), keys.uuid().toString(), "keys.smile");
        var preferences = SmileFile.of(path);
        preferences.write(keys, async);
    }

    @Override
    public void serializeStore(Store store, boolean async) {
        if (cachedUuids != null && !cachedUuids.contains(store.uuid())) {
            cachedUuids.add(store.uuid());
        }

        var phoneNumber = store.phoneNumber().orElse(null);
        if (cachedPhoneNumbers != null && !cachedPhoneNumbers.contains(phoneNumber)) {
            cachedPhoneNumbers.add(phoneNumber);
        }

        var task = attributeStoreSerializers.get(store.uuid());
        if (task != null && !task.isDone()) {
            return;
        }
        var path = getSessionFile(store, "store.smile");
        var preferences = SmileFile.of(path);
        preferences.write(store, async);
        if(async){
            store.chats().forEach(chat -> serializeChat(store, chat));
            return;
        }

        var futures = store.chats()
                .stream()
                .map(chat -> serializeChat(store, chat))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
    }

    private CompletableFuture<Void> serializeChat(Store store, Chat chat) {
        var path = getSessionFile(store, "%s%s.smile".formatted(CHAT_PREFIX, chat.uuid()));
        var preferences = SmileFile.of(path);
        return preferences.write(chat, true);
    }

    @Override
    public Optional<Keys> deserializeKeys(@NonNull ClientType type, UUID id) {
        return deserializeKeysFromId(type, id.toString());
    }

    @Override
    public Optional<Keys> deserializeKeys(@NonNull ClientType type, long phoneNumber) {
        var file = getSessionDirectory(type, String.valueOf(phoneNumber));
        if(Files.notExists(file)){
            return Optional.empty();
        }

        try {
            return deserializeKeysFromId(type, Files.readString(file));
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot read %s".formatted(phoneNumber), exception);
        }
    }

    private Optional<Keys> deserializeKeysFromId(ClientType type, String id) {
        try {
            var path = getSessionFile(type, id, "keys.smile");
            var preferences = SmileFile.of(path);
            return preferences.read(Keys.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Corrupted keys", exception);
        }
    }

    @Override
    public Optional<Store> deserializeStore(@NonNull ClientType type, UUID id) {
        return deserializeStoreFromId(type, id.toString());
    }

    @Override
    public Optional<Store> deserializeStore(@NonNull ClientType type, long phoneNumber) {
        var file = getSessionDirectory(type, String.valueOf(phoneNumber));
        if(Files.notExists(file)){
            return Optional.empty();
        }

        try {
            return deserializeStoreFromId(type, Files.readString(file));
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot read %s".formatted(phoneNumber), exception);
        }
    }

    private Optional<Store> deserializeStoreFromId(ClientType type, String id) {
        try {
            var path = getSessionFile(type, id, "store.smile");
            var preferences = SmileFile.of(path);
            return preferences.read(Store.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Corrupted store", exception);
        }
    }

    @Override
    public synchronized CompletableFuture<Void> attributeStore(Store store) {
        var oldTask = attributeStoreSerializers.get(store.uuid());
        if (oldTask != null) {
            return oldTask;
        }
        var directory = getSessionDirectory(store.clientType(), store.uuid().toString());
        if (Files.notExists(directory)) {
            return CompletableFuture.completedFuture(null);
        }
        try (var walker = Files.walk(directory)) {
            var futures = walker.filter(entry -> entry.getFileName().toString().startsWith(CHAT_PREFIX))
                    .map(entry -> CompletableFuture.runAsync(() -> deserializeChat(store, entry)))
                    .toArray(CompletableFuture[]::new);
            var result = CompletableFuture.allOf(futures);
            attributeStoreSerializers.put(store.uuid(), result);
            return result;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot deserialize store", exception);
        }
    }

    @Override
    public void deleteSession(@NonNull Controller<?> controller) {
        var folderPath = getSessionDirectory(controller.clientType(), controller.uuid().toString());
        deleteDirectory(folderPath.toFile());
        var phoneNumber = controller.phoneNumber().orElse(null);
        if(phoneNumber == null){
            return;
        }
        var linkedFolderPath = getSessionDirectory(controller.clientType(), phoneNumber.toString());
        deleteDirectory(linkedFolderPath.toFile());
    }

    @Override
    public void linkPhoneNumber(@NonNull Controller<?> controller) {
        try {
            var phoneNumber = controller.phoneNumber().orElse(null);
            if(phoneNumber == null){
                return;
            }
            var link = getSessionDirectory(controller.clientType(), phoneNumber.toString());
            Files.writeString(link, controller.uuid().toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            logger.log(WARNING, "Cannot link phone number to uuid", exception);
        }
    }

    // Not using Java NIO api because of a bug
    private void deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return;
        }
        var files = directory.listFiles();
        if (files == null) {
            if (directory.delete()) {
                return;
            }

            logger.log(WARNING, "Cannot delete folder %s".formatted(directory));
            return;
        }
        for (var file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
                continue;
            }
            if (file.delete()) {
                continue;
            }
            logger.log(WARNING, "Cannot delete file %s".formatted(directory));
        }
        if (directory.delete()) {
            return;
        }
        logger.log(WARNING, "Cannot delete folder %s".formatted(directory));
    }

    private void deserializeChat(Store baseStore, Path entry) {
        try {
            var chatPreferences = SmileFile.of(entry);
            var chat = chatPreferences.read(Chat.class).orElseThrow();
            baseStore.addChatDirect(chat);
        } catch (IOException exception) {
            var chatName = entry.getFileName().toString().replaceFirst(CHAT_PREFIX, "").replace(".smile", "");
            logger.log(ERROR, "Chat %s is corrupted, resetting it".formatted(chatName), exception);
            try {
                Files.deleteIfExists(entry);
            } catch (IOException deleteException) {
                logger.log(WARNING, "Cannot delete chat file");
            }
            var result = Chat.ofJid(ContactJid.of(chatName));
            baseStore.addChatDirect(result);
        }
    }

    private Path getHome(ClientType type) {
        var directory = baseDirectory.resolve(type == ClientType.MOBILE ? "mobile" : "web");
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot create directory", exception);
            }
        }

        return directory;
    }

    private Path getSessionDirectory(ClientType clientType, String uuid) {
        return getHome(clientType).resolve(uuid);
    }

    private Path getSessionFile(Store store, String fileName) {
        return getSessionFile(store.clientType(), store.uuid().toString(), fileName);
    }

    private Path getSessionFile(ClientType clientType, String uuid, String fileName) {
        return getSessionDirectory(clientType, uuid).resolve(fileName);
    }

    private record SmileFile(Path file, Semaphore semaphore) {
        private final static ConcurrentHashMap<Path, SmileFile> instances = new ConcurrentHashMap<>();
        private static final Logger logger = System.getLogger("SmileFile");

        private SmileFile {
            try {
                Files.createDirectories(file.getParent());
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot create smile file", exception);
            }
        }

        private static synchronized SmileFile of(@NonNull Path file) {
            var knownInstance = instances.get(file);
            if (knownInstance != null) {
                return knownInstance;
            }

            var instance = new SmileFile(file, new Semaphore(1));
            instances.put(file, instance);
            return instance;
        }

        private <T> Optional<T> read(Class<T> clazz) throws IOException {
            return read(new TypeReference<>() {
                @Override
                public Class<T> getType() {
                    return clazz;
                }
            });
        }

        private <T> Optional<T> read(TypeReference<T> reference) throws IOException {
            if (Files.notExists(file)) {
                return Optional.empty();
            }
            try (var input = new GZIPInputStream(Files.newInputStream(file))) {
                return Optional.of(Smile.readValue(input, reference));
            }
        }

        private CompletableFuture<Void> write(Object input, boolean async) {
            if (!async) {
                writeSync(input);
                return CompletableFuture.completedFuture(null);
            }

            return CompletableFuture.runAsync(() -> writeSync(input)).exceptionallyAsync(throwable -> {
                logger.log(ERROR, "Cannot serialize smile file", throwable);
                return null;
            });
        }

        private void writeSync(Object input) {
            try {
                if (input == null) {
                    return;
                }

                semaphore.acquire();
                try(var byteArrayOutputStream = new ByteArrayOutputStream()) {
                    try (var stream = new GZIPOutputStream(byteArrayOutputStream)) {
                        Smile.writeValueAsBytes(stream, input);
                        Files.write(file, byteArrayOutputStream.toByteArray());
                    }
                }
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot complete file write", exception);
            } catch (InterruptedException exception) {
                throw new RuntimeException("Cannot acquire lock", exception);
            } finally {
                semaphore.release();
            }
        }
    }
}
