package it.auties.whatsapp.util;

import it.auties.protobuf.annotation.ProtobufConverter;
import it.auties.protobuf.annotation.ProtobufMixin;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Protobuf {
    public static class FutureMixin implements ProtobufMixin {
        @ProtobufConverter
        public static <T> CompletableFuture<T> of(T value) {
            return CompletableFuture.completedFuture(value);
        }

        @ProtobufConverter
        public static <T> T toValue(CompletableFuture<T> uri) {
            return uri.join();
        }
    }

    public static class URIMixin implements ProtobufMixin {
        @ProtobufConverter
        public static URI of(String uri) {
            return uri == null ? null : URI.create(uri);
        }

        @ProtobufConverter
        public static String toValue(URI uri) {
            return Objects.toString(uri);
        }
    }

    public static class UUIDMixin implements ProtobufMixin {
        @ProtobufConverter
        public static UUID of(String uuid) {
            return UUID.fromString(uuid);
        }

        @ProtobufConverter
        public static String toValue(UUID uuid) {
            return Objects.toString(uuid);
        }
    }
}
