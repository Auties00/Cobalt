package it.auties.whatsapp.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.auties.map.SimpleMapModule;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.ContactJid;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.fasterxml.jackson.annotation.PropertyAccessor.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_INDEX;
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
    private static final Map<ContactJid, Integer> hashCodesMap = new ConcurrentHashMap<>();

    private final Path baseDirectory;
    private final Logger logger;
    private final AtomicReference<CompletableFuture<Void>> deserializer;

    /**
     * Creates a provider using the default path
     */
    public DefaultControllerSerializer() {
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
        }catch (IOException exception){
            logger.log(WARNING, "Cannot create base directory at %s: %s".formatted(baseDirectory, exception.getMessage()));
        }

        Validate.isTrue(Files.isDirectory(baseDirectory), "Expected a directory as base path: %s", baseDirectory);
        this.deserializer = new AtomicReference<>();
    }

    @Override
    public LinkedList<UUID> findIds(@NonNull ClientType type) {
        try (var walker = Files.walk(getDirectoryFromType(type), 1)
                .sorted(Comparator.comparing(this::getLastModifiedTime))) {
            return walker.map(this::parsePathAsId)
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

    @Override
    public void serializeKeys(Keys keys, boolean async) {
        var task = deserializer.get();
        if (task != null && !task.isDone()) {
            return;
        }
        var path = getDirectoryFromType(keys.clientType()).resolve("%s/keys.smile".formatted(keys.uuid()));
        var preferences = SmileFile.of(path);
        preferences.write(keys, async);
    }

    @Override
    public void serializeStore(Store store, boolean async) {
        var task = deserializer.get();
        if (task != null && !task.isDone()) {
            return;
        }
        var path = getDirectoryFromType(store.clientType()).resolve("%s/store.smile".formatted(store.uuid()));
        var preferences = SmileFile.of(path);
        preferences.write(store, async);
        store.chats()
                .stream()
                .filter(this::updateHash)
                .forEach(chat -> serializeChat(store, chat, async));
    }

    private boolean updateHash(Chat entry) {
        var lastHashCode = hashCodesMap.get(entry.jid());
        var newHashCode = entry.fullHashCode();
        if (lastHashCode == null) {
            hashCodesMap.put(entry.jid(), newHashCode);
            return true;
        }
        if (newHashCode == lastHashCode) {
            return false;
        }
        hashCodesMap.put(entry.jid(), newHashCode);
        return true;
    }

    private void serializeChat(Store store, Chat chat, boolean async) {
        var path = getDirectoryFromType(store.clientType()).resolve("%s/%s%s.smile".formatted(store.uuid(), CHAT_PREFIX, chat.uuid()));
        var preferences = SmileFile.of(path);
        preferences.write(chat, async);
    }

    @Override
    public Optional<Keys> deserializeKeys(@NonNull ClientType type, UUID id) {
        try {
            var path = getDirectoryFromType(type).resolve("%s/keys.smile".formatted(id));
            var preferences = SmileFile.of(path);
            return preferences.read(Keys.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Corrupted keys", exception);
        }
    }

    @Override
    public Optional<Store> deserializeStore(@NonNull ClientType type, UUID id) {
        try {
            var path = getDirectoryFromType(type).resolve("%s/store.smile".formatted(id));
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
        var directory = getDirectoryFromType(store.clientType()).resolve(String.valueOf(store.uuid()));
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
        var folderPath = getDirectoryFromType(type).resolve(id.toString());
        deleteDirectory(folderPath.toFile());
    }

    // Not using Java NIO api because of a bug
    private void deleteDirectory(File directory){
        if(directory == null || !directory.exists()) {
            return;
        }
        var files = directory.listFiles();
        if(files == null) {
            if (directory.delete()) {
                return;
            }

            logger.log(WARNING, "Cannot delete folder %s".formatted(directory));
            return;
        }
        for(var file : files) {
            if(file.isDirectory()) {
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
            hashCodesMap.put(chat.jid(), chat.fullHashCode());
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
            hashCodesMap.put(result.jid(), result.fullHashCode());
        }
    }

    private Path getDirectoryFromType(ClientType type) {
        var directory = baseDirectory.resolve(type == ClientType.APP_CLIENT ? "mobile" : "web");
        if(!Files.exists(directory)){
            try {
                Files.createDirectories(directory);
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot create directory", exception);
            }
        }

        return directory;
    }

    private record SmileFile(Path file, Semaphore semaphore) {
        private final static ConcurrentHashMap<Path, SmileFile> instances = new ConcurrentHashMap<>();
        private final static ObjectMapper SMILE = new SmileMapper()
                .registerModule(new Jdk8Module())
                .registerModule(new SimpleMapModule())
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(NON_DEFAULT)
                .enable(WRITE_ENUMS_USING_INDEX)
                .enable(FAIL_ON_EMPTY_BEANS)
                .enable(ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .disable(FAIL_ON_UNKNOWN_PROPERTIES)
                .setVisibility(ALL, ANY)
                .setVisibility(GETTER, NONE)
                .setVisibility(IS_GETTER, NONE);

        private static synchronized SmileFile of(@NonNull Path file){
            var knownInstance = instances.get(file);
            if (knownInstance != null) {
                return knownInstance;
            }

            var instance = new SmileFile(file, new Semaphore(1));
            instances.put(file, instance);
            return instance;
        }

        private SmileFile {
            try {
                Files.createDirectories(file.getParent());
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot create smile file", exception);
            }
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
            var stream = Files.newInputStream(file);
            return Optional.of(SMILE.readValue(new GZIPInputStream(stream), reference));
        }

        private void write(Object input, boolean async) {
            if (!async) {
                writeSync(input);
                return;
            }

            CompletableFuture.runAsync(() -> writeSync(input))
                    .exceptionallyAsync(throwable -> {
                        throwable.printStackTrace();
                        return null;
                    });
        }

        private void writeSync(Object input) {
            try {
                semaphore.acquire();
                var serialized = SMILE.writeValueAsBytes(input);
                var compressedStream = new ByteArrayOutputStream(serialized.length);
                try (compressedStream) {
                    try (var zipStream = new GZIPOutputStream(compressedStream, 65536)) {
                        zipStream.write(serialized);
                    }
                }
                if(Files.notExists(file.getParent())) {
                    Files.createDirectories(file.getParent());
                }
                Files.write(file, compressedStream.toByteArray(), StandardOpenOption.CREATE);
            } catch (IOException exception){
                throw new UncheckedIOException("Cannot complete file write", exception);
            }catch (InterruptedException exception){
                throw new RuntimeException("Cannot acquire lock", exception);
            }finally {
                semaphore.release();
            }
        }
    }
}
