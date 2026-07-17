package com.github.auties00.cobalt.wire.linked.device.pairing;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Describes the set of client side capabilities and migration states that a companion
 * advertises to WhatsApp's server during the pairing handshake.
 *
 * <p>When a new companion is being linked to a primary device the server needs to know
 * which optional schemas the client already supports, which database migrations it has
 * already applied and which data paths it can safely participate in. Carrying these
 * booleans on the pairing payload lets the server decide whether to push legacy data,
 * newer LID scoped data, or a mix of both, and whether snapshot recovery and thumbnail
 * sync can be used without further coordination.
 *
 * <p>All fields are tri state on the wire, so an unset field is different from an
 * explicit {@code false}. The accessor methods collapse this to plain {@code boolean} by
 * treating {@code null} as {@code false}, since the server interprets absent fields the
 * same way.
 */
@ProtobufMessage(name = "ClientPairingProps")
public final class ClientPairingProps {
    /**
     * Indicates whether the client has already migrated its chat database to the LID
     * (Linked Identity) addressing scheme.
     *
     * <p>When {@code true} the server will deliver chat records using LID scoped JIDs
     * directly, otherwise it falls back to legacy phone number scoped addressing while
     * the client finishes migrating. Serialised as wire index {@code 1}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean isChatDbLidMigrated;

    /**
     * Indicates whether the Syncd component on the client is running in a pure LID
     * session, meaning all sync traffic is addressed via LIDs rather than mixed
     * phone number and LID records.
     *
     * <p>Serialised as wire index {@code 2}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    Boolean isSyncdPureLidSession;

    /**
     * Indicates whether the client supports Syncd snapshot recovery, a fast path that
     * lets the server send a compacted snapshot of the latest app state instead of a
     * full mutation log.
     *
     * <p>Serialised as wire index {@code 3}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    Boolean isSyncdSnapshotRecoveryEnabled;

    /**
     * Indicates whether the client supports receiving thumbnail data as part of history
     * sync. When enabled the server can include small preview images alongside the
     * historical messages that are being replayed.
     *
     * <p>Serialised as wire index {@code 4}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean isHsThumbnailSyncEnabled;

    /**
     * Opaque payload that carries the snapshot of the client's subscription
     * sync state, transmitted to the server during pairing so it can resume
     * the subscription tracker without replaying the full event log.
     *
     * <p>The format is private to the syncd subsystem and is treated as raw
     * bytes by this layer. Serialised as wire index {@code 5}.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    byte[] subscriptionSyncPayload;

    /**
     * Full protobuf constructor invoked by the generated builder and the deserializer.
     *
     * @param isChatDbLidMigrated            chat database LID migration flag
     * @param isSyncdPureLidSession          pure LID Syncd session flag
     * @param isSyncdSnapshotRecoveryEnabled snapshot recovery capability flag
     * @param isHsThumbnailSyncEnabled       history sync thumbnail capability flag
     * @param subscriptionSyncPayload        snapshot of the subscription sync state, or
     *                                       {@code null} when the client does not need to
     *                                       resume an existing subscription tracker
     */
    ClientPairingProps(Boolean isChatDbLidMigrated, Boolean isSyncdPureLidSession, Boolean isSyncdSnapshotRecoveryEnabled, Boolean isHsThumbnailSyncEnabled, byte[] subscriptionSyncPayload) {
        this.isChatDbLidMigrated = isChatDbLidMigrated;
        this.isSyncdPureLidSession = isSyncdPureLidSession;
        this.isSyncdSnapshotRecoveryEnabled = isSyncdSnapshotRecoveryEnabled;
        this.isHsThumbnailSyncEnabled = isHsThumbnailSyncEnabled;
        this.subscriptionSyncPayload = subscriptionSyncPayload;
    }

    /**
     * Returns whether the chat database has been migrated to LID addressing.
     *
     * @return {@code true} if the client advertised the flag as {@code true}, otherwise
     *         {@code false} (including when the field was absent on the wire)
     */
    public boolean isChatDbLidMigrated() {
        return isChatDbLidMigrated != null && isChatDbLidMigrated;
    }

    /**
     * Returns whether the Syncd session is running in pure LID mode.
     *
     * @return {@code true} if the client advertised the flag as {@code true}, otherwise
     *         {@code false} (including when the field was absent on the wire)
     */
    public boolean isSyncdPureLidSession() {
        return isSyncdPureLidSession != null && isSyncdPureLidSession;
    }

    /**
     * Returns whether Syncd snapshot recovery is enabled on this client.
     *
     * @return {@code true} if the client advertised the flag as {@code true}, otherwise
     *         {@code false} (including when the field was absent on the wire)
     */
    public boolean isSyncdSnapshotRecoveryEnabled() {
        return isSyncdSnapshotRecoveryEnabled != null && isSyncdSnapshotRecoveryEnabled;
    }

    /**
     * Returns whether history sync thumbnails are supported on this client.
     *
     * @return {@code true} if the client advertised the flag as {@code true}, otherwise
     *         {@code false} (including when the field was absent on the wire)
     */
    public boolean isHsThumbnailSyncEnabled() {
        return isHsThumbnailSyncEnabled != null && isHsThumbnailSyncEnabled;
    }

    /**
     * Returns the snapshot of the client's subscription sync state.
     *
     * @return the opaque payload bytes, or {@link Optional#empty()} when the field was
     *         absent on the wire
     */
    public Optional<byte[]> subscriptionSyncPayload() {
        return Optional.ofNullable(subscriptionSyncPayload);
    }

    /**
     * Replaces the chat database LID migration flag.
     *
     * @param isChatDbLidMigrated the new flag value, or {@code null} to clear it
     */
    public void setChatDbLidMigrated(Boolean isChatDbLidMigrated) {
        this.isChatDbLidMigrated = isChatDbLidMigrated;
    }

    /**
     * Replaces the pure LID Syncd session flag.
     *
     * @param isSyncdPureLidSession the new flag value, or {@code null} to clear it
     */
    public void setSyncdPureLidSession(Boolean isSyncdPureLidSession) {
        this.isSyncdPureLidSession = isSyncdPureLidSession;
    }

    /**
     * Replaces the Syncd snapshot recovery capability flag.
     *
     * @param isSyncdSnapshotRecoveryEnabled the new flag value, or {@code null} to
     *                                       clear it
     */
    public void setSyncdSnapshotRecoveryEnabled(Boolean isSyncdSnapshotRecoveryEnabled) {
        this.isSyncdSnapshotRecoveryEnabled = isSyncdSnapshotRecoveryEnabled;
    }

    /**
     * Replaces the history sync thumbnail capability flag.
     *
     * @param isHsThumbnailSyncEnabled the new flag value, or {@code null} to clear it
     */
    public void setHsThumbnailSyncEnabled(Boolean isHsThumbnailSyncEnabled) {
        this.isHsThumbnailSyncEnabled = isHsThumbnailSyncEnabled;
    }

    /**
     * Replaces the subscription sync payload snapshot.
     *
     * @param subscriptionSyncPayload the new payload bytes, or {@code null} to clear it
     */
    public void setSubscriptionSyncPayload(byte[] subscriptionSyncPayload) {
        this.subscriptionSyncPayload = subscriptionSyncPayload;
    }
}
