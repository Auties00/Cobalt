package it.auties.whatsapp.controller.builtin;

import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;
import it.auties.whatsapp.controller.*;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatSpec;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.newsletter.NewsletterSpec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
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
    void encodeKeys(Keys keys, Path path) {
        try(var stream = Files.newOutputStream(path)) {
            KeysSpec.encode(keys, new ProtobufOutputStream(stream));
            stream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    void encodeStore(Store store, Path path) {
        try(var stream = Files.newOutputStream(path)) {
            StoreSpec.encode(store, new ProtobufOutputStream(stream));
            stream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    void encodeChat(Chat chat, Path path) {
        try(var stream = Files.newOutputStream(path)) {
            ChatSpec.encode(chat, new ProtobufOutputStream(stream));
            stream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    void encodeNewsletter(Newsletter newsletter, Path path) {
        try(var stream = Files.newOutputStream(path)) {
            NewsletterSpec.encode(newsletter, new ProtobufOutputStream(stream));
            stream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    @Override
    Keys decodeKeys(Path keys) throws IOException {
        return KeysSpec.decode(new ProtobufInputStream(keys));
    }

    @Override
    Store decodeStore(Path store) throws IOException {
        return StoreSpec.decode(new ProtobufInputStream(store));
    }

    @Override
    Chat decodeChat(Path chat) throws IOException {
        return ChatSpec.decode(new ProtobufInputStream(chat));
    }

    @Override
    Newsletter decodeNewsletter(Path newsletter) throws IOException {
        return NewsletterSpec.decode(new ProtobufInputStream(newsletter));
    }
}
