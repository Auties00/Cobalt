package it.auties.whatsapp.controller.builtin;

import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.util.Json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class JsonControllerSerializer extends FileControllerSerializer {
    private static final Path DEFAULT_SERIALIZER_PATH = Path.of(System.getProperty("user.home") + "/.cobalt/");
    private static final Map<Path, JsonControllerSerializer> serializers = new ConcurrentHashMap<>();
    static {
        serializers.put(DEFAULT_SERIALIZER_PATH, new JsonControllerSerializer(DEFAULT_SERIALIZER_PATH));
    }

    public static ControllerSerializer ofDefaultPath() {
        return Objects.requireNonNull(serializers.get(DEFAULT_SERIALIZER_PATH));
    }

    public static ControllerSerializer of(Path baseDirectory) {
        var known = serializers.get(baseDirectory);
        if(known != null) {
            return known;
        }

        var result = new JsonControllerSerializer(baseDirectory);
        serializers.put(baseDirectory, result);
        return result;
    }

    private JsonControllerSerializer(Path baseDirectory) {
        super(baseDirectory);
    }

    @Override
    String fileExtension() {
        return ".json";
    }

    @Override
    void encodeKeys(Keys keys, Path path) {
        try {
            Json.writeValueAsBytes(keys, Files.newOutputStream(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    void encodeStore(Store store, Path path) {
        try {
            Json.writeValueAsBytes(store, Files.newOutputStream(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    void encodeChat(Chat chat, Path path) {
        try {
            Json.writeValueAsBytes(chat, Files.newOutputStream(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    void encodeNewsletter(Newsletter newsletter, Path path) {
        try {
            Json.writeValueAsBytes(newsletter, Files.newOutputStream(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    Keys decodeKeys(Path keys) throws IOException {
        return Json.readValue(Channels.newInputStream(FileChannel.open(keys)), Keys.class);
    }

    @Override
    Store decodeStore(Path store) throws IOException {
        return Json.readValue(Channels.newInputStream(FileChannel.open(store)), Store.class);
    }

    @Override
    Chat decodeChat(Path chat) throws IOException {
        return Json.readValue(Channels.newInputStream(FileChannel.open(chat)), Chat.class);
    }

    @Override
    Newsletter decodeNewsletter(Path newsletter) throws IOException {
        return Json.readValue(Channels.newInputStream(FileChannel.open(newsletter)), Newsletter.class);
    }
}
