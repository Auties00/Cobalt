package it.auties.whatsapp.controller.builtin;

import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.util.Json;

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
    byte[] encodeKeys(Keys keys) {
        return Json.writeValueAsBytes(keys);
    }

    @Override
    byte[] encodeStore(Store store) {
        return Json.writeValueAsBytes(store);
    }

    @Override
    byte[] encodeChat(Chat chat) {
        return Json.writeValueAsBytes(chat);
    }

    @Override
    byte[] encodeNewsletter(Newsletter newsletter) {
        return Json.writeValueAsBytes(newsletter);
    }

    @Override
    Keys decodeKeys(byte[] keys) {
        return Json.readValue(keys, Keys.class);
    }

    @Override
    Store decodeStore(byte[] store) {
        return Json.readValue(store, Store.class);
    }

    @Override
    Chat decodeChat(byte[] chat) {
        return Json.readValue(chat, Chat.class);
    }

    @Override
    Newsletter decodeNewsletter(byte[] newsletter) {
        return Json.readValue(newsletter, Newsletter.class);
    }
}
