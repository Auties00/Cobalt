package it.auties.whatsapp.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.util.Smile;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
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
    private final AtomicReference<CompletableFuture<Void>> deserializer;
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
        try {
            Files.createDirectories(baseDirectory);
        } catch (IOException exception) {
            logger.log(WARNING, "Cannot create base directory at %s: %s".formatted(baseDirectory, exception.getMessage()));
        }

        Validate.isTrue(Files.isDirectory(baseDirectory), "Expected a directory as base path: %s", baseDirectory);
        this.deserializer = new AtomicReference<>();
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
            throw new UncheckedIOException("Cannot get last modification date", exception);
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

        var task = deserializer.get();
        if (task != null && !task.isDone()) {
            return;
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

        if (cachedPhoneNumbers != null && !cachedPhoneNumbers.contains(store.phoneNumber())) {
            cachedPhoneNumbers.add(store.phoneNumber());
        }

        var task = deserializer.get();
        if (task != null && !task.isDone()) {
            return;
        }
        var path = getSessionFile(store, "store.smile");
        var preferences = SmileFile.of(path);
        preferences.write(store, async);
        for (var chat : store.chats()) {
            serializeChat(store, chat, async);
        }
    }

    private void serializeChat(Store store, Chat chat, boolean async) {
        var path = getSessionFile(store, "%s%s.smile".formatted(CHAT_PREFIX, chat.uuid()));
        var preferences = SmileFile.of(path);
        preferences.write(chat, async);
    }

    @Override
    public Optional<Keys> deserializeKeys(@NonNull ClientType type, UUID id) {
        return deserializeKeysFromId(type, Objects.toString(id));
    }

    @Override
    public Optional<Keys> deserializeKeys(@NonNull ClientType type, long phoneNumber) {
        return deserializeKeysFromId(type, String.valueOf(phoneNumber));
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
        return deserializeStoreFromId(type, Objects.toString(id));
    }

    @Override
    public Optional<Store> deserializeStore(@NonNull ClientType type, long phoneNumber) {
        return deserializeStoreFromId(type, String.valueOf(phoneNumber));
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
        var oldTask = deserializer.get();
        if (oldTask != null) {
            return oldTask;
        }
        var directory = getSessionFile(store.clientType(), store.uuid().toString(), "keys.smile");
        if (Files.notExists(directory)) {
            return CompletableFuture.completedFuture(null);
        }
        try (var walker = Files.walk(directory)) {
            var futures = walker.filter(entry -> entry.getFileName().toString().startsWith(CHAT_PREFIX))
                    .map(entry -> CompletableFuture.runAsync(() -> deserializeChat(store, entry)))
                    .toArray(CompletableFuture[]::new);
            var result = CompletableFuture.allOf(futures);
            deserializer.set(result);
            return result;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot deserialize store", exception);
        }
    }

    @Override
    public void deleteSession(@NonNull ClientType type, UUID id) {
        var folderPath = getSession(type, Objects.toString(id));
        deleteDirectory(folderPath.toFile());
    }

    @Override
    public void linkPhoneNumber(@NonNull Store store) {
        try {
            var link = getSession(store.clientType(), store.phoneNumber().toString());
            if(Files.exists(link)){
                return;
            }
            var original = getSession(store.clientType(), store.uuid().toString());
            Files.createSymbolicLink(link, original);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot create link between store and phone number", exception);
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
        var directory = baseDirectory.resolve(type == ClientType.APP_CLIENT ? "mobile" : "web");
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot create directory", exception);
            }
        }

        return directory;
    }

    private Path getSession(ClientType clientType, String uuid) {
        return getHome(clientType).resolve(uuid);
    }

    private Path getSessionFile(Store store, String fileName) {
        return getSessionFile(store.clientType(), store.uuid().toString(), fileName);
    }

    private Path getSessionFile(ClientType clientType, String uuid, String fileName) {
        return getSession(clientType, uuid).resolve(fileName);
    }

    private record SmileFile(Path file, Semaphore semaphore) {
        private final static ConcurrentHashMap<Path, SmileFile> instances = new ConcurrentHashMap<>();

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

        private void write(Object input, boolean async) {
            if (!async) {
                writeSync(input);
                return;
            }

            CompletableFuture.runAsync(() -> writeSync(input)).exceptionallyAsync(throwable -> {
                throwable.printStackTrace();
                return null;
            });
        }

        private void writeSync(Object input) {
            try {
                if (input == null) {
                    return;
                }

                semaphore.acquire();
                try (var stream = new GZIPOutputStream(Files.newOutputStream(file))) {
                    Smile.writeValueAsBytes(stream, input);
                    stream.flush();
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
