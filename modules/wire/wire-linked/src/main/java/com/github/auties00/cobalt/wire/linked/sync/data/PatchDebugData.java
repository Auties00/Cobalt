package com.github.auties00.cobalt.wire.linked.sync.data;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Diagnostic payload attached to an outgoing app-state patch.
 *
 * <p>App-state sync uses an incremental LT-Hash to prove that a patch applied on
 * the client produces the same collection state as the server's authoritative
 * copy. When the two hashes disagree, the server or receiver needs enough
 * context to investigate the divergence without access to the plaintext
 * mutations. This message carries that context: the previous and new LT-Hash,
 * the patch version, the collection name, a fingerprint of the snapshot MAC
 * key, the number of add, remove and override mutations contained in the
 * patch, and information about the sender platform and device role.
 *
 * <p>The payload is encoded as raw protobuf bytes into the
 * {@code clientDebugData} field of a {@link SyncdPatch} and is intended purely
 * for logging. It never affects the correctness of sync application: if the
 * bytes fail to decode they are silently ignored.
 */
@ProtobufMessage(name = "PatchDebugData")
public final class PatchDebugData {
    /**
     * LT-Hash of the collection before the patch is applied.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] currentLthash;

    /**
     * LT-Hash of the collection after the patch has been applied.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] newLthash;

    /**
     * Encoded patch version counter for this patch.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] patchVersion;

    /**
     * Name of the app-state collection to which the patch applies.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] collectionName;

    /**
     * First four bytes of a hash of the snapshot MAC key, used as a
     * low-collision fingerprint of the key without exposing it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    byte[] firstFourBytesFromAHashOfSnapshotMacKey;

    /**
     * Subtractive component of the new LT-Hash when the patch removes or
     * overrides records. Paired with {@link #newLthash} to support incremental
     * hash reconciliation.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    byte[] newLthashSubtract;

    /**
     * Number of {@code SET} mutations that add new keys in this patch.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT32)
    Integer numberAdd;

    /**
     * Number of {@code REMOVE} mutations in this patch.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.INT32)
    Integer numberRemove;

    /**
     * Number of {@code SET} mutations that override an existing key value.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT32)
    Integer numberOverride;

    /**
     * Platform of the device that produced the patch.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
    Platform senderPlatform;

    /**
     * Whether the producing device is the primary (phone) device of the
     * account, rather than a companion.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    Boolean isSenderPrimary;


    /**
     * Constructs a new debug payload with all fields populated.
     *
     * @param currentLthash the pre-patch LT-Hash
     * @param newLthash the post-patch LT-Hash
     * @param patchVersion the encoded patch version
     * @param collectionName the target collection name
     * @param firstFourBytesFromAHashOfSnapshotMacKey the snapshot MAC key fingerprint
     * @param newLthashSubtract the subtractive component of the new LT-Hash
     * @param numberAdd the count of add mutations
     * @param numberRemove the count of remove mutations
     * @param numberOverride the count of override mutations
     * @param senderPlatform the producing device platform
     * @param isSenderPrimary whether the producer is the primary device
     */
    PatchDebugData(byte[] currentLthash, byte[] newLthash, byte[] patchVersion, byte[] collectionName, byte[] firstFourBytesFromAHashOfSnapshotMacKey, byte[] newLthashSubtract, Integer numberAdd, Integer numberRemove, Integer numberOverride, Platform senderPlatform, Boolean isSenderPrimary) {
        this.currentLthash = currentLthash;
        this.newLthash = newLthash;
        this.patchVersion = patchVersion;
        this.collectionName = collectionName;
        this.firstFourBytesFromAHashOfSnapshotMacKey = firstFourBytesFromAHashOfSnapshotMacKey;
        this.newLthashSubtract = newLthashSubtract;
        this.numberAdd = numberAdd;
        this.numberRemove = numberRemove;
        this.numberOverride = numberOverride;
        this.senderPlatform = senderPlatform;
        this.isSenderPrimary = isSenderPrimary;
    }

    /**
     * Returns the LT-Hash of the collection before the patch was applied.
     *
     * @return the pre-patch hash bytes, or empty if absent
     */
    public Optional<byte[]> currentLthash() {
        return Optional.ofNullable(currentLthash);
    }

    /**
     * Returns the LT-Hash of the collection after the patch was applied.
     *
     * @return the post-patch hash bytes, or empty if absent
     */
    public Optional<byte[]> newLthash() {
        return Optional.ofNullable(newLthash);
    }

    /**
     * Returns the encoded patch version.
     *
     * @return the patch version bytes, or empty if absent
     */
    public Optional<byte[]> patchVersion() {
        return Optional.ofNullable(patchVersion);
    }

    /**
     * Returns the target app-state collection name encoded as UTF-8 bytes.
     *
     * @return the collection name bytes, or empty if absent
     */
    public Optional<byte[]> collectionName() {
        return Optional.ofNullable(collectionName);
    }

    /**
     * Returns a fingerprint of the snapshot MAC key used when producing the
     * patch. The fingerprint is the first four bytes of a hash over the key.
     *
     * @return the fingerprint bytes, or empty if absent
     */
    public Optional<byte[]> firstFourBytesFromAHashOfSnapshotMacKey() {
        return Optional.ofNullable(firstFourBytesFromAHashOfSnapshotMacKey);
    }

    /**
     * Returns the subtractive component of the new LT-Hash, carrying the
     * contribution of removed or overridden records.
     *
     * @return the subtractive hash bytes, or empty if absent
     */
    public Optional<byte[]> newLthashSubtract() {
        return Optional.ofNullable(newLthashSubtract);
    }

    /**
     * Returns the number of add mutations contained in the patch.
     *
     * @return the add count, or empty if not reported
     */
    public OptionalInt numberAdd() {
        return numberAdd == null ? OptionalInt.empty() : OptionalInt.of(numberAdd);
    }

    /**
     * Returns the number of remove mutations contained in the patch.
     *
     * @return the remove count, or empty if not reported
     */
    public OptionalInt numberRemove() {
        return numberRemove == null ? OptionalInt.empty() : OptionalInt.of(numberRemove);
    }

    /**
     * Returns the number of override mutations contained in the patch. An
     * override is a {@code SET} whose key already existed with a different
     * value.
     *
     * @return the override count, or empty if not reported
     */
    public OptionalInt numberOverride() {
        return numberOverride == null ? OptionalInt.empty() : OptionalInt.of(numberOverride);
    }

    /**
     * Returns the platform of the device that produced the patch.
     *
     * @return the sender platform, or empty if not reported
     */
    public Optional<Platform> senderPlatform() {
        return Optional.ofNullable(senderPlatform);
    }

    /**
     * Returns whether the producing device is the primary device of the
     * account. Returns {@code false} when the flag is absent.
     *
     * @return {@code true} if the sender is the primary device
     */
    public boolean isSenderPrimary() {
        return isSenderPrimary != null && isSenderPrimary;
    }

    /**
     * Sets the pre-patch LT-Hash.
     *
     * @param currentLthash the hash bytes
     */
    public void setCurrentLthash(byte[] currentLthash) {
        this.currentLthash = currentLthash;
    }

    /**
     * Sets the post-patch LT-Hash.
     *
     * @param newLthash the hash bytes
     */
    public void setNewLthash(byte[] newLthash) {
        this.newLthash = newLthash;
    }

    /**
     * Sets the encoded patch version.
     *
     * @param patchVersion the patch version bytes
     */
    public void setPatchVersion(byte[] patchVersion) {
        this.patchVersion = patchVersion;
    }

    /**
     * Sets the target collection name.
     *
     * @param collectionName the collection name bytes
     */
    public void setCollectionName(byte[] collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * Sets the snapshot MAC key fingerprint.
     *
     * @param firstFourBytesFromAHashOfSnapshotMacKey the fingerprint bytes
     */
    public void setFirstFourBytesFromAHashOfSnapshotMacKey(byte[] firstFourBytesFromAHashOfSnapshotMacKey) {
        this.firstFourBytesFromAHashOfSnapshotMacKey = firstFourBytesFromAHashOfSnapshotMacKey;
    }

    /**
     * Sets the subtractive component of the new LT-Hash.
     *
     * @param newLthashSubtract the subtractive hash bytes
     */
    public void setNewLthashSubtract(byte[] newLthashSubtract) {
        this.newLthashSubtract = newLthashSubtract;
    }

    /**
     * Sets the number of add mutations reported by this payload.
     *
     * @param numberAdd the add count
     */
    public void setNumberAdd(Integer numberAdd) {
        this.numberAdd = numberAdd;
    }

    /**
     * Sets the number of remove mutations reported by this payload.
     *
     * @param numberRemove the remove count
     */
    public void setNumberRemove(Integer numberRemove) {
        this.numberRemove = numberRemove;
    }

    /**
     * Sets the number of override mutations reported by this payload.
     *
     * @param numberOverride the override count
     */
    public void setNumberOverride(Integer numberOverride) {
        this.numberOverride = numberOverride;
    }

    /**
     * Sets the producing device's platform.
     *
     * @param senderPlatform the sender platform
     */
    public void setSenderPlatform(Platform senderPlatform) {
        this.senderPlatform = senderPlatform;
    }

    /**
     * Sets whether the producing device is the account's primary device.
     *
     * @param isSenderPrimary {@code true} if the sender is the primary device
     */
    public void setSenderPrimary(Boolean isSenderPrimary) {
        this.isSenderPrimary = isSenderPrimary;
    }

    /**
     * Identifier of the WhatsApp client platform that produced the patch.
     *
     * <p>Values cover regular and Business (SMB) variants of Android and iOS,
     * Web, desktop wrappers, wearable companions, and internal test clients.
     */
    @ProtobufEnum(name = "PatchDebugData.Platform")
    public static enum Platform {
        /**
         * Regular WhatsApp on Android.
         */
        ANDROID(0),
        /**
         * WhatsApp Business on Android.
         */
        SMBA(1),
        /**
         * Regular WhatsApp on iPhone.
         */
        IPHONE(2),
        /**
         * WhatsApp Business on iPhone.
         */
        SMBI(3),
        /**
         * WhatsApp Web running in a browser.
         */
        WEB(4),
        /**
         * WhatsApp on Universal Windows Platform (UWP).
         */
        UWP(5),
        /**
         * WhatsApp desktop app on macOS (Darwin).
         */
        DARWIN(6),
        /**
         * WhatsApp on iPad.
         */
        IPAD(7),
        /**
         * WhatsApp companion on Wear OS.
         */
        WEAROS(8),
        /**
         * WhatsApp on Samsung Galaxy watches (Tizen family).
         */
        WASG(9),
        /**
         * Alternate wearable platform identifier.
         */
        WEARM(10),
        /**
         * WhatsApp C API (internal / server-side integration).
         */
        CAPI(11);

        /**
         * Constructs a platform constant with the given protobuf enum index.
         *
         * @param index the protobuf wire index
         */
        Platform(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index assigned to this platform.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this platform constant.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return this.index;
        }
    }
}
