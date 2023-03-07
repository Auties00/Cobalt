package it.auties.whatsapp.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.auties.map.SimpleMapModule;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
public class DefaultControllerSerializer implements ControllerSerializer, ControllerDeserializer {
    private static final Path DEFAULT_DIRECTORY = Path.of(System.getProperty("user.home") + "/.whatsapp4j/");
    private static final String CHAT_PREFIX = "chat_";

    private final Path baseDirectory;
    private final Logger logger;
    private final AtomicReference<CompletableFuture<Void>> deserializer;
    private final Map<ContactJid, Integer> hashCodesMap;

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
        this.hashCodesMap = new ConcurrentHashMap<>();
    }

    @Override
    public LinkedList<Integer> findIds() {
        try (var walker = Files.walk(baseDirectory, 1)
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

    private Optional<Integer> parsePathAsId(Path file) {
        try {
            return Optional.of(Integer.parseInt(file.getFileName().toString()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void serializeKeys(Keys keys, boolean async) {
        if (deserializer.get() == null || !deserializer.get().isDone()) {
            return;
        }
        var path = baseDirectory.resolve("%s/keys.smile".formatted(keys.id()));
        var preferences = new SmileFile(path);
        preferences.write(keys, async);
    }

    @Override
    public void serializeStore(Store store, boolean async) {
        if (deserializer.get() == null || !deserializer.get().isDone()) {
            return;
        }
        var path = baseDirectory.resolve("%s/store.smile".formatted(store.id()));
        var preferences = new SmileFile(path);
        preferences.write(store, async);
        store.chats().stream().filter(this::updateHash).forEach(chat -> serializeChat(store, chat, async));
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
        var path = baseDirectory.resolve("%s/%s%s.smile".formatted(store.id(), CHAT_PREFIX, chat.jid()));
        var preferences = new SmileFile(path);
        preferences.write(chat, async);
    }

    @Override
    public Optional<Keys> deserializeKeys(int id) {
        try {
            var path = baseDirectory.resolve("%s/keys.smile".formatted(id));
            var preferences = new SmileFile(path);
            return preferences.read(Keys.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Corrupted keys", exception);
        }
    }

    @Override
    public Optional<Store> deserializeStore(int id) {
        try {
            var path = baseDirectory.resolve("%s/store.smile".formatted(id));
            var preferences = new SmileFile(path);
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
        var directory = baseDirectory.resolve(String.valueOf(store.id()));
        if (Files.notExists(directory)) {
            return CompletableFuture.completedFuture(null);
        }
        try (var walker = Files.walk(directory)) {
            var futures = walker.filter(entry -> entry.getFileName().toString().startsWith(CHAT_PREFIX))
                    .map(entry -> deserializeChat(store, entry))
                    .toArray(CompletableFuture[]::new);
            var result = CompletableFuture.allOf(futures)
                    .thenRunAsync(() -> store.chats().forEach(chat -> hashCodesMap.put(chat.jid(), chat.fullHashCode())));
            deserializer.set(result);
            return result;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot deserialize store", exception);
        }
    }

    private CompletableFuture<Void> deserializeChat(Store baseStore, Path entry) {
        return CompletableFuture.runAsync(() -> {
            try {
                var chatPreferences = new SmileFile(entry);
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
                hashCodesMap.put(result.jid(), result.fullHashCode());
                baseStore.addChatDirect(result);
            }
        });
    }

    private record SmileFile(@NonNull Path file) {
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

        private SmileFile(@NonNull Path file) {
            try {
                this.file = file;
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
                CompletableFuture.completedFuture(null);
                return;
            }

            CompletableFuture.runAsync(() -> writeSync(input)).exceptionallyAsync(this::onError);
        }

        private void writeSync(Object input) {
            try {
                var gzipOutputStream = new GZIPOutputStream(Files.newOutputStream(file, StandardOpenOption.CREATE));
                gzipOutputStream.write(SMILE.writeValueAsBytes(input));
                gzipOutputStream.flush();
                gzipOutputStream.finish();
                gzipOutputStream.close();
            } catch (Throwable exception) {
                throw new RuntimeException("Cannot write to file", exception);
            }
        }

        private Void onError(Throwable exception) {
            exception.printStackTrace();
            return null;
        }
    }
}
