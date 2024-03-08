package it.auties.whatsapp.registration.apns;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

record ApnsListener(Function<ApnsPacket, Boolean> filter, CompletableFuture<ApnsPacket> future) {
    ApnsListener(Function<ApnsPacket, Boolean> filter) {
        this(filter, new CompletableFuture<>());
    }
}
