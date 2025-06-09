package it.auties.whatsapp.model.media;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

@ProtobufMessage(name = "MediaData")
public final class MediaData {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String localPath;

    MediaData(String localPath) {
        this.localPath = Objects.requireNonNull(localPath, "localPath cannot be null");
    }

    public String localPath() {
        return this.localPath;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MediaData that
                && Objects.equals(localPath, that.localPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localPath);
    }

    @Override
    public String toString() {
        return "MediaData[" +
                "localPath=" + localPath +
                ']';
    }
}