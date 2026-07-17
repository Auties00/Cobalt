package com.github.auties00.cobalt.store.linked.protobuf.temporary;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.store.linked.protobuf.*;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStoreFactory;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientSixPartsKeys;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevice;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The {@link LinkedWhatsAppStoreFactory} that produces {@link TemporaryStore} sessions held entirely
 * in RAM.
 *
 * @apiNote
 * Cobalt embedders obtain the singleton through {@link LinkedWhatsAppStoreFactory#temporary()};
 * suitable for tests, ephemeral bots, scratch programs, and any scenario where session state
 * should not survive a JVM restart.
 *
 * @implNote
 * This implementation is stateless and exposes a singleton instance because nothing inside the
 * factory varies per session. The three {@code load} entries always return empty since there is
 * no on-disk surface to consult; the three {@code create} entries delegate to
 * {@link #newStore(LinkedWhatsAppClientType, UUID, Long, SignalIdentityKeyPair, SignalIdentityKeyPair, byte[], Jid)}
 * which builds a {@link TemporaryStore} with empty collections seeded by the supplied identity
 * scalars.
 */
public final class TemporaryLinkedWhatsAppStoreFactory implements LinkedWhatsAppStoreFactory {
    /**
     * The logger for {@link TemporaryLinkedWhatsAppStoreFactory}.
     */
    private static final System.Logger LOGGER = Log.get(TemporaryLinkedWhatsAppStoreFactory.class);

    /**
     * The singleton instance.
     *
     * @apiNote
     * Obtained through {@link LinkedWhatsAppStoreFactory#temporary()}; the factory is stateless so
     * sharing one instance is safe across threads and sessions.
     */
    public static final TemporaryLinkedWhatsAppStoreFactory INSTANCE = new TemporaryLinkedWhatsAppStoreFactory();

    /**
     * Constructs the singleton.
     *
     * @apiNote
     * Private; the only legitimate instance is {@link #INSTANCE}.
     */
    private TemporaryLinkedWhatsAppStoreFactory() {

    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns {@link Optional#empty()} because transient sessions
     * have no on-disk presence to load.
     */
    @Override
    public Optional<LinkedWhatsAppStore> load(LinkedWhatsAppClientType clientType, UUID uuid) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns {@link Optional#empty()} because transient sessions
     * have no on-disk presence to load.
     */
    @Override
    public Optional<LinkedWhatsAppStore> load(LinkedWhatsAppClientType clientType, long phoneNumber) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns {@link Optional#empty()} because transient sessions
     * have no on-disk presence to enumerate.
     */
    @Override
    public Optional<LinkedWhatsAppStore> loadLatest(LinkedWhatsAppClientType clientType) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation generates a random UUID when {@code uuid} is {@code null} and forwards
     * to {@link #newStore} with the identity-bearing scalars all left {@code null}.
     */
    @Override
    public LinkedWhatsAppStore create(LinkedWhatsAppClientType clientType, UUID uuid) {
        Objects.requireNonNull(clientType, "clientType cannot be null");
        return newStore(clientType, Objects.requireNonNullElseGet(uuid, UUID::randomUUID), null, null, null, null, null);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation assigns a fresh random UUID and forwards to {@link #newStore} with the
     * identity-bearing scalars left {@code null}; the phone number alone is retained.
     */
    @Override
    public LinkedWhatsAppStore create(LinkedWhatsAppClientType clientType, long phoneNumber) {
        Objects.requireNonNull(clientType, "clientType cannot be null");
        return newStore(clientType, UUID.randomUUID(), phoneNumber, null, null, null, null);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation seeds the store with the noise and identity key pairs, identity id and
     * derived user JID from {@code sixPartsKeys} so a previously exported six-parts key blob can
     * be used to bootstrap a transient session without a fresh pairing flow.
     */
    @Override
    public LinkedWhatsAppStore create(LinkedWhatsAppClientType clientType, LinkedWhatsAppClientSixPartsKeys sixPartsKeys) {
        Objects.requireNonNull(clientType, "clientType cannot be null");
        Objects.requireNonNull(sixPartsKeys, "sixPartsKeys cannot be null");
        var phoneNumber = sixPartsKeys.phoneNumber();
        return newStore(clientType, UUID.randomUUID(), phoneNumber, sixPartsKeys.noiseKeyPair(), sixPartsKeys.identityKeyPair(), sixPartsKeys.identityId(), Jid.of(phoneNumber));
    }

    /**
     * Builds a fresh {@link TemporaryStore} with empty collections plus the supplied identity
     * scalars.
     *
     * @apiNote
     * Internal helper shared by every {@code create(...)} overload; centralises the
     * client-type-to-device mapping and the empty-collection initialisation so the three public
     * entry points read uniformly.
     *
     * @implNote
     * This implementation pre-allocates every collection with the {@code ConcurrentHashMap} or
     * {@code LinkedHashMap} variant required by the {@link ProtobufWhatsAppStore} contract; the
     * session directory is intentionally {@code null} because the transient variant has no disk
     * surface.
     *
     * @param clientType      the client type
     * @param uuid            the session UUID
     * @param phoneNumber     the session phone number, or {@code null}
     * @param noiseKeyPair    the Noise XX static key pair, or {@code null}
     * @param identityKeyPair the Signal identity key pair, or {@code null}
     * @param identityId      the identity-id bytes, or {@code null}
     * @param jid             the user JID, or {@code null}
     * @return a freshly built store
     */
    private static LinkedWhatsAppStore newStore(LinkedWhatsAppClientType clientType, UUID uuid, Long phoneNumber, SignalIdentityKeyPair noiseKeyPair, SignalIdentityKeyPair identityKeyPair, byte[] identityId, Jid jid) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "creating temporary store for {0} session {1}", clientType, uuid);
        var device = switch (clientType) {
            case WEB -> LinkedWhatsAppClientDevice.desktop();
            case MOBILE -> LinkedWhatsAppClientDevice.ios(false);
        };
        return new TemporaryStore(
                new ProtobufLinkedWhatsAppSignalStoreBuilder()
                        .noiseKeyPair(noiseKeyPair)
                        .identityKeyPair(identityKeyPair)
                        .identityId(identityId)
                        .build(),
                new ProtobufLinkedWhatsAppAccountStoreBuilder()
                        .uuid(uuid)
                        .phoneNumber(phoneNumber)
                        .clientType(clientType)
                        .initializationTimeStamp(Instant.now())
                        .device(device)
                        .jid(jid)
                        .build(),
                new ProtobufLinkedWhatsAppContactStoreBuilder().build(),
                new ProtobufLinkedWhatsAppSyncStoreBuilder().build(),
                new ProtobufLinkedWhatsAppSettingsStoreBuilder().build(),
                null,
                new ProtobufLinkedWebSessionStoreBuilder().build(),
                new TemporaryLinkedWhatsAppWamStore(null, null, null, null, null, null),
                new TemporaryLinkedWhatsAppChatStore(null, null));
    }
}
