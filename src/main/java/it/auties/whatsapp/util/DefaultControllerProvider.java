package it.auties.whatsapp.util;

import it.auties.whatsapp.controller.ControllerProvider;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.chat.Chat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultControllerProvider implements ControllerProvider {
    @Override
    public LinkedList<Integer> ids() {
        try (var walker = Files.walk(Preferences.home(), 1)
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
        var preferences = Preferences.of("%s/keys.smile", keys.id());
        preferences.write(keys, async);
    }

    @Override
    public void serializeStore(Store store, boolean async) {
        var preferences = Preferences.of("%s/store.smile", store.id());
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
        var preferences = store.chatPreferences(chat);
        preferences.write(chat, true);
    }
}
