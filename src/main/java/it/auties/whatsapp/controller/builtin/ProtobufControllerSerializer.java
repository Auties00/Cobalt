package it.auties.whatsapp.controller.builtin;

import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;
import it.auties.whatsapp.controller.*;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatSpec;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.newsletter.NewsletterSpec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProtobufControllerSerializer extends FileControllerSerializer {
    private static final Path DEFAULT_SERIALIZER_PATH = Path.of(System.getProperty("user.home") + "/.cobalt/");

    public ProtobufControllerSerializer() {
        this(DEFAULT_SERIALIZER_PATH);
    }

    public ProtobufControllerSerializer(Path baseDirectory) {
        super(baseDirectory);
    }

    @Override
    String fileExtension() {
        return ".proto";
    }

    @Override
    void encodeKeys(Keys keys, Path path) {
        try {
            var tempFile = Files.createTempFile(path.getFileName().toString(), ".tmp");
            try(var stream = Files.newOutputStream(tempFile)) {
                KeysSpec.encode(keys, ProtobufOutputStream.toStream(stream));
                stream.flush();
            }
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        }catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    void encodeStore(Store store, Path path) {
        try {
            var tempFile = Files.createTempFile(path.getFileName().toString(), ".tmp");
            try(var stream = Files.newOutputStream(tempFile)) {
                StoreSpec.encode(store, ProtobufOutputStream.toStream(stream));
                stream.flush();
            }
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        }catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    void encodeChat(Chat chat, Path path) {
        try {
            var tempFile = Files.createTempFile(path.getFileName().toString(), ".tmp");
            try(var stream = Files.newOutputStream(tempFile)) {
                ChatSpec.encode(chat, ProtobufOutputStream.toStream(stream));
                stream.flush();
            }
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        }catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    void encodeNewsletter(Newsletter newsletter, Path path) {
        try {
            var tempFile = Files.createTempFile(path.getFileName().toString(), ".tmp");
            try(var stream = Files.newOutputStream(tempFile)) {
                NewsletterSpec.encode(newsletter, ProtobufOutputStream.toStream(stream));
                stream.flush();
            }
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        }catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
    
    @Override
    Keys decodeKeys(Path keys) throws IOException {
        try(var stream = Files.newInputStream(keys)) {
            return KeysSpec.decode(ProtobufInputStream.fromStream(stream));
        }
    }

    @Override
    Store decodeStore(Path store) throws IOException {
        try(var stream = Files.newInputStream(store)) {
            return StoreSpec.decode(ProtobufInputStream.fromStream(stream));
        }
    }

    @Override
    Chat decodeChat(Path chat) throws IOException {
        try(var stream = Files.newInputStream(chat)) {
            return ChatSpec.decode(ProtobufInputStream.fromStream(stream));
        }
    }

    @Override
    Newsletter decodeNewsletter(Path newsletter) throws IOException {
        try(var stream = Files.newInputStream(newsletter)) {
            return NewsletterSpec.decode(ProtobufInputStream.fromStream(stream));
        }
    }
}
