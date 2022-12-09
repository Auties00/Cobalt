package it.auties.whatsapp.serialization;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.util.LocalFileSystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;

class DefaultControllerProvider implements ControllerSerializerProvider, ControllerDeserializerProvider {
    private static final String CHAT_PREFIX = "chat_";

    private final AtomicReference<CompletableFuture<Void>> deserializer;
    public DefaultControllerProvider(){
        this.deserializer = new AtomicReference<>();
    }

    @Override
    public LinkedList<Integer> findIds() {
        try (var walker = Files.walk(LocalFileSystem.home(), 1)
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
            return Optional.of(Integer.parseInt(file.getFileName()
                    .toString()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void serializeKeys(Keys keys, boolean async) {
        if(deserializer.get() == null || !deserializer.get().isDone()){
            return;
        }

        var preferences = SmileFile.of("%s/keys.smile", keys.id());
        preferences.write(keys, async);
    }

    @Override
    public void serializeStore(Store store, boolean async) {
        if(deserializer.get() == null || !deserializer.get().isDone()){
            return;
        }

        var preferences = SmileFile.of("%s/store.smile", store.id());
        preferences.write(store, async);
        store.chats()
                .stream()
                .filter(this::updateHash)
                .forEach(chat -> serializeChat(store, chat));
    }

    private boolean updateHash(Chat entry) {
        if(entry.lastHashCode() == -1){
            return true;
        }

        var newHashCode = entry.hashCode();
        if (newHashCode == entry.lastHashCode()) {
            return false;
        }

        entry.lastHashCode(newHashCode);
        return true;
    }

    private void serializeChat(Store store, Chat chat) {
        var preferences = SmileFile.of("%s/%s%s.smile", store.id(), CHAT_PREFIX, chat.jid());
        preferences.write(chat, true);
    }

    @Override
    public Optional<Keys> deserializeKeys(int id) {
        var preferences = SmileFile.of("%s/keys.smile", id);
        try {
            return preferences.read(Keys.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Corrupted keys", exception);
        }
    }

    @Override
    public Optional<Store> deserializeStore(int id) {
        try {
            var preferences = SmileFile.of("%s/store.smile", id);
            return preferences.read(Store.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Corrupted store", exception);
        }
    }

    @Override
    public synchronized CompletableFuture<Void> attributeStore(Store store) {
        var oldTask = deserializer.get();
        if(oldTask != null) {
            return oldTask;
        }

        var directory = LocalFileSystem.of(String.valueOf(store.id()));
        if(Files.notExists(directory)){
            return CompletableFuture.completedFuture(null);
        }

        try(var walker = Files.walk(directory)){
            var futures = walker.filter(entry -> entry.getFileName().toString().startsWith(CHAT_PREFIX))
                    .map(entry -> deserializeChat(store, entry))
                    .toArray(CompletableFuture[]::new);
            var result = CompletableFuture.allOf(futures);
            deserializer.set(result);
            return result;
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot deserialize store", exception);
        }
    }

    private CompletableFuture<Void> deserializeChat(Store baseStore, Path entry) {
        return CompletableFuture.runAsync(() -> {
            try {
                var chatPreferences = SmileFile.of(entry);
                var chat = chatPreferences.read(Chat.class)
                        .orElseThrow(() -> new NoSuchElementException("Corrupted chat at %s".formatted(entry)));
                baseStore.addChatDirect(chat);
            } catch (IOException exception){
                var chatName = entry.getFileName()
                        .toString()
                        .replaceFirst(CHAT_PREFIX, "")
                        .replace(".smile", "");
                var logger = System.getLogger("Deserializer");
                logger.log(ERROR, "Chat %s is corrupted, resetting it".formatted(chatName), exception);
                try {
                    Files.deleteIfExists(entry);
                } catch (IOException deleteException) {
                    logger.log(WARNING, "Cannot delete chat file");
                }

                baseStore.addChatDirect(Chat.ofJid(ContactJid.of(chatName)));
            }
        });
    }
}
