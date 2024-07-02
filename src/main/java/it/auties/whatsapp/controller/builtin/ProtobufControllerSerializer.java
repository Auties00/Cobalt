package it.auties.whatsapp.controller.builtin;

import it.auties.whatsapp.controller.*;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatSpec;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.newsletter.NewsletterSpec;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ProtobufControllerSerializer extends FileControllerSerializer {
    private static final Path DEFAULT_SERIALIZER_PATH = Path.of(System.getProperty("user.home") + "/.cobalt/");
    private static final Map<Path, ProtobufControllerSerializer> serializers = new ConcurrentHashMap<>();
    static {
        serializers.put(DEFAULT_SERIALIZER_PATH, new ProtobufControllerSerializer(DEFAULT_SERIALIZER_PATH));
    }

    public static ControllerSerializer ofDefaultPath() {
        return Objects.requireNonNull(serializers.get(DEFAULT_SERIALIZER_PATH));
    }

    public static ControllerSerializer of(Path baseDirectory) {
        var known = serializers.get(baseDirectory);
        if(known != null) {
            return known;
        }

        var result = new ProtobufControllerSerializer(baseDirectory);
        serializers.put(baseDirectory, result);
        return result;
    }

    private ProtobufControllerSerializer(Path baseDirectory) {
        super(baseDirectory);
    }

    @Override
    String fileExtension() {
        return ".proto";
    }

    @Override
    byte[] encodeKeys(Keys keys) {
        return KeysSpec.encode(keys);
    }

    @Override
    byte[] encodeStore(Store store) {
        return StoreSpec.encode(store);
    }

    @Override
    byte[] encodeChat(Chat chat) {
        return ChatSpec.encode(chat);
    }

    @Override
    byte[] encodeNewsletter(Newsletter newsletter) {
        return NewsletterSpec.encode(newsletter);
    }

    @Override
    Keys decodeKeys(byte[] keys) {
        return KeysSpec.decode(keys);
    }

    @Override
    Store decodeStore(byte[] store) {
        return StoreSpec.decode(store);
    }

    @Override
    Chat decodeChat(byte[] chat) {
        return ChatSpec.decode(chat);
    }

    @Override
    Newsletter decodeNewsletter(byte[] newsletter) {
        return NewsletterSpec.decode(newsletter);
    }
}
