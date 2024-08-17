package it.auties.whatsapp.registration.cloudVerification;

import java.util.concurrent.CompletableFuture;

public interface CloudVerificationClient extends AutoCloseable {
    CompletableFuture<String> getPushCode();
    CompletableFuture<String> getPushToken(boolean business);

    @Override
    void close();
}
