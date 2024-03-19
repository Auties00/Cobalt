package it.auties.whatsapp.util;

import it.auties.protobuf.annotation.ProtobufConverter;
import it.auties.protobuf.annotation.ProtobufMixin;

import java.util.concurrent.CompletableFuture;

public class ProtobufFutureMixin implements ProtobufMixin {
    @ProtobufConverter
    public static <T> CompletableFuture<T> of(T value) {
        return CompletableFuture.completedFuture(value);
    }

    @ProtobufConverter
    public static <T> T toValue(CompletableFuture<T> uri) {
        return uri.join();
    }
}
