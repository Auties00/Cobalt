package it.auties.whatsapp.util;

import it.auties.protobuf.annotation.ProtobufConverter;
import it.auties.protobuf.annotation.ProtobufMixin;

import java.net.URI;
import java.util.Objects;

public class ProtobufUriMixin implements ProtobufMixin {
    @ProtobufConverter
    public static URI of(String uri) {
        return uri == null ? null : URI.create(uri);
    }

    @ProtobufConverter
    public static String toValue(URI uri) {
        return Objects.toString(uri);
    }
}
