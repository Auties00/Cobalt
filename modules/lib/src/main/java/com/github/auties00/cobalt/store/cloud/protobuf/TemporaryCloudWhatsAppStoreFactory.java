package com.github.auties00.cobalt.store.cloud.protobuf;

import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStore;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * The {@link CloudWhatsAppStoreFactory} that produces Cloud sessions held entirely in RAM.
 *
 * @apiNote
 * Cobalt embedders obtain the singleton through {@link CloudWhatsAppStoreFactory#temporary()}; suitable
 * for tests, ephemeral bots, scratch programs, and any scenario where the credentials and per-chat read
 * markers should not survive a JVM restart.
 *
 * @implNote
 * This implementation is stateless and exposes a singleton because nothing inside the factory varies per
 * session. The two {@code load} entries always return empty since there is no on-disk surface to consult,
 * and {@link #create(String, String)} returns a store with no persistence directory attached, so its
 * {@link CloudWhatsAppStore#save()} and {@link CloudWhatsAppStore#delete()} are no-ops.
 */
public final class TemporaryCloudWhatsAppStoreFactory implements CloudWhatsAppStoreFactory {
    /**
     * The singleton instance.
     *
     * @apiNote
     * Obtained through {@link CloudWhatsAppStoreFactory#temporary()}; the factory is stateless so sharing
     * one instance is safe across threads and sessions.
     */
    public static final TemporaryCloudWhatsAppStoreFactory INSTANCE = new TemporaryCloudWhatsAppStoreFactory();

    /**
     * Constructs the singleton.
     *
     * @apiNote
     * Private; the only legitimate instance is {@link #INSTANCE}.
     */
    private TemporaryCloudWhatsAppStoreFactory() {

    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns {@link Optional#empty()} because transient sessions have no
     * on-disk presence to load.
     */
    @Override
    public Optional<CloudWhatsAppStore> load(String phoneNumberId) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns {@link Optional#empty()} because transient sessions have no
     * on-disk presence to enumerate.
     */
    @Override
    public Optional<CloudWhatsAppStore> loadLatest() {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation seeds the store with the two credentials plus the default
     * {@link CloudApiVersion} and attaches no persistence directory.
     */
    @Override
    public CloudWhatsAppStore create(String accessToken, String phoneNumberId) {
        Objects.requireNonNull(accessToken, "accessToken cannot be null");
        Objects.requireNonNull(phoneNumberId, "phoneNumberId cannot be null");
        return new ProtobufCloudWhatsAppStoreBuilder()
                .accessToken(accessToken)
                .phoneNumberId(phoneNumberId)
                .apiVersion(CloudApiVersion.DEFAULT.version())
                .build();
    }
}
