package com.github.auties00.cobalt.sync.crypto;

import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyData;
import com.github.auties00.cobalt.wire.linked.signal.KeyId;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdPatch;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdSnapshot;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.System.Logger.Level;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.SequencedCollection;

/**
 * Validates the snapshot MAC and patch MAC on every app-state sync envelope
 * the relay sends, and computes the same MACs for every outgoing patch.
 *
 * <p>This class collapses the MAC formulas and the wire-level verification
 * routines that call into them: {@link #verifySnapshotMac} and
 * {@link #verifyPatchIntegrity} are the verification entry points, the static
 * {@link #computeSnapshotMac} and {@link #computePatchMac} are the underlying
 * formulas, and {@link #computeOutgoingSnapshotAndPatchMacs} pairs them for the
 * outgoing path.
 *
 * <p>Driven by the collection-handler pipeline that processes a single
 * {@link SyncdPatch} or {@link SyncdSnapshot} envelope at a time. Cobalt does
 * not batch verification across patches; every patch carries its own value MAC
 * list and its own wire snapshot MAC, both of which feed back into the patch
 * MAC chain.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdAntiTampering")
@WhatsAppWebModule(moduleName = "WAWebSyncdEncryptionManager")
public final class MutationIntegrityVerifier {
    /**
     * The logger for {@link MutationIntegrityVerifier}.
     */
    private static final System.Logger LOGGER = Log.get(MutationIntegrityVerifier.class);

    /**
     * The store backing key-id resolution and collection-state lookups.
     */
    private final LinkedWhatsAppStore store;

    /**
     * Constructs a verifier bound to a store.
     *
     * @param store the store from which sync keys, collection versions, and
     *              the mac-mismatch latch are read
     */
    public MutationIntegrityVerifier(LinkedWhatsAppStore store) {
        this.store = store;
    }

    /**
     * Verifies the snapshot MAC on a freshly received {@link SyncdSnapshot}
     * and returns the computed MAC for downstream use.
     *
     * <p>Called once per incoming snapshot, after the caller has decrypted
     * every mutation in the snapshot and accumulated the new LT-Hash. The
     * {@code expectedHash} input is the locally computed LT-Hash; matching it
     * under the snapshot-MAC key against the wire MAC closes the loop between
     * the decrypted records and the relay's authenticated digest.
     *
     * @implNote
     * This implementation diverges from
     * {@code WAWebSyncdAntiTampering.computeLtHashAndValidateSnapshot} in
     * two ways. WA Web computes the LT-Hash inline (as part of the same
     * routine), whereas Cobalt separates LT-Hash recomputation into the
     * caller and only validates the MAC here. WA Web on a snapshot MAC
     * mismatch falls through to a recovery flow with WAM telemetry; Cobalt
     * unconditionally raises
     * {@link WhatsAppWebAppStateSyncException.SnapshotMacMismatch} because
     * recovery is deferred to the configurable
     * {@code WhatsAppClientErrorHandler}.
     *
     * @param collectionName the collection type (drives the MAC-input suffix)
     * @param version        the snapshot version (drives the 8-byte big-endian suffix)
     * @param snapshot       the wire snapshot, source of both the wire MAC and the key id
     * @param expectedHash   the locally computed LT-Hash over the decrypted records
     * @return the computed snapshot MAC, or {@code null} when MAC checks are disabled
     * @throws WhatsAppWebAppStateSyncException.SnapshotMacMismatch if the wire MAC is absent or does not match
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError     if the snapshot lacks a key id or the resolved key has no data
     * @throws WhatsAppWebAppStateSyncException.MissingKey          if the snapshot's key id is not in the local store
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdAntiTampering", exports = "computeLtHashAndValidateSnapshot", adaptation = WhatsAppAdaptation.ADAPTED)
    public byte[] verifySnapshotMac(SyncPatchType collectionName, long version, SyncdSnapshot snapshot, byte[] expectedHash) {
        if (!store.syncStore().checkPatchMacs()) {
            return null;
        }

        var mac = snapshot.mac();
        if(mac.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "snapshot mac missing for {0} at version {1}", collectionName, version);
            throw new WhatsAppWebAppStateSyncException.SnapshotMacMismatch(collectionName, version);
        }

        var keyId = snapshot.keyId()
                .flatMap(KeyId::id);
        if(keyId.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "snapshot missing key id for {0} at version {1}", collectionName, version);
            throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                    "Snapshot missing key id for " + collectionName + " at version " + version,
                    null
            );
        }

        var keyData = store.syncStore().findWebAppStateKeyById(keyId.get())
                .orElseThrow(() -> {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "missing sync key {0} for snapshot on {1}", keyId.get(), collectionName);
                    return new WhatsAppWebAppStateSyncException.MissingKey(keyId.get());
                })
                .keyData()
                .flatMap(AppStateSyncKeyData::keyData)
                .orElseThrow(() -> {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "snapshot sync key {0} has no key data for {1} at version {2}", keyId.get(), collectionName, version);
                    return new WhatsAppWebAppStateSyncException.UnexpectedError(
                            "Snapshot sync key had no key data for " + collectionName + " at version " + version,
                            null
                    );
                });

        try (var keys = MutationKeys.ofSyncKey(keyData)) {
            var expectedMac = computeSnapshotMac(keys.snapshotMacKey(), expectedHash, version, collectionName);
            if (!MessageDigest.isEqual(mac.get(), expectedMac)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "snapshot mac mismatch for {0} at version {1}", collectionName, version);
                throw new WhatsAppWebAppStateSyncException.SnapshotMacMismatch(collectionName, version);
            }
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "snapshot mac verified for {0} at version {1}", collectionName, version);
            return expectedMac;
        }
    }

    /**
     * Verifies the patch MAC and the snapshot MAC on a freshly received
     * {@link SyncdPatch}.
     *
     * <p>Called once per incoming patch envelope. The patch MAC chains over the
     * wire snapshot MAC plus the per-mutation value MACs and is the primary
     * integrity gate; the snapshot MAC re-checks the locally accumulated
     * LT-Hash against the relay's expected digest. The two mismatches are
     * treated differently: a patch MAC mismatch is fatal, while a snapshot MAC
     * mismatch reports {@code false} so the caller can flip the collection into
     * the mac-mismatch state without tearing down the session.
     *
     * @implNote
     * This implementation skips the snapshot MAC validation when the
     * collection is already in the mac-mismatch state, matching the
     * {@code if (E && k) return} short-circuit in the WA Web
     * {@code validateSnapshotMac} helper. The {@code checkPatchMacs} short
     * circuit returning {@code true} unconditionally has no WA Web analogue;
     * it is a Cobalt-only embedder toggle for fixture replay.
     *
     * @param collectionName the collection type
     * @param patch          the wire patch
     * @param computedLtHash the locally accumulated LT-Hash after applying this patch
     * @param patchValueMacs the value MACs from this patch's mutations, in wire order
     * @return {@code true} when the snapshot MAC matched (or was skipped),
     *         {@code false} when the snapshot MAC mismatched and the collection should be flagged
     * @throws WhatsAppWebAppStateSyncException.PatchMacMismatch if the wire patch MAC does not match
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError  if the patch lacks a key id or the key has no data
     * @throws WhatsAppWebAppStateSyncException.MissingKey       if the patch's key id is not in the local store
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdAntiTampering", exports = "computeLtHashAndValidatePatch", adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean verifyPatchIntegrity(SyncPatchType collectionName, SyncdPatch patch, byte[] computedLtHash, SequencedCollection<byte[]> patchValueMacs) {
        if (!store.syncStore().checkPatchMacs()) {
            return true;
        }

        var keyId = patch.keyId()
                .flatMap(KeyId::id);
        if (keyId.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "patch missing key id for {0}", collectionName);
            throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                    "Patch missing key id for " + collectionName,
                    null
            );
        }

        var keyData = store.syncStore().findWebAppStateKeyById(keyId.get())
                .orElseThrow(() -> {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "missing sync key {0} for patch on {1}", keyId.get(), collectionName);
                    return new WhatsAppWebAppStateSyncException.MissingKey(keyId.get());
                })
                .keyData()
                .flatMap(AppStateSyncKeyData::keyData)
                .orElseThrow(() -> {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "patch sync key {0} has no key data for {1}", keyId.get(), collectionName);
                    return new WhatsAppWebAppStateSyncException.UnexpectedError(
                            "Patch sync key had no key data for " + collectionName,
                            null
                    );
                });

        long patchVersion = patch.version()
                .map(version -> version.version().orElse(0L))
                .orElse(0L);

        try (var keys = MutationKeys.ofSyncKey(keyData)) {
            var wireSnapshotMac = patch.snapshotMac().orElse(null);

            var wirePatchMac = patch.patchMac().orElse(null);
            if (wirePatchMac != null) {
                var expectedPatchMac = computePatchMac(keys.patchMacKey(), wireSnapshotMac, patchValueMacs, patchVersion, collectionName);
                if (!MessageDigest.isEqual(wirePatchMac, expectedPatchMac)) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "patch mac mismatch for {0} at version {1}", collectionName, patchVersion);
                    throw new WhatsAppWebAppStateSyncException.PatchMacMismatch(collectionName, patchVersion);
                }
            }

            var alreadyInMacMismatch = store.syncStore().findWebAppState(collectionName).macMismatch();
            if (alreadyInMacMismatch) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "skipping snapshot mac check for {0}: already in mac-mismatch state", collectionName);
                return true;
            }

            if (wireSnapshotMac != null) {
                var expectedSnapshotMac = computeSnapshotMac(keys.snapshotMacKey(), computedLtHash, patchVersion, collectionName);
                if (!MessageDigest.isEqual(wireSnapshotMac, expectedSnapshotMac)) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "snapshot mac mismatch while verifying patch for {0} at version {1}", collectionName, patchVersion);
                    return false;
                }
            }
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "patch integrity verified for {0} at version {1}", collectionName, patchVersion);
        return true;
    }

    /**
     * The paired output of {@link #computeOutgoingSnapshotAndPatchMacs}.
     *
     * @param snapshotMac the snapshot MAC over the new LT-Hash and version
     * @param patchMac    the patch MAC chained over the snapshot MAC and the value MACs
     */
    public record OutgoingMacs(
            byte[] snapshotMac,
            byte[] patchMac
    ) {
    }

    /**
     * Computes the snapshot MAC and patch MAC for an outgoing patch envelope.
     *
     * <p>Called by the outgoing patch builder after the LT-Hash has been
     * recomputed for the new version. The patch MAC chains over the snapshot
     * MAC, so callers must use the {@link OutgoingMacs#snapshotMac()} output
     * (not any pre-existing wire MAC) for the relay to accept the patch.
     *
     * @implNote
     * This implementation accepts the already-derived MAC keys directly; the
     * WA Web routine re-derives them via {@code generateEncryptionKeys}
     * inside the same closure. Holding the keys in the caller avoids a
     * second HKDF expansion per outgoing patch.
     *
     * @param snapshotMacKey the snapshot MAC key
     * @param patchMacKey    the patch MAC key
     * @param newLtHash      the LT-Hash for the new version
     * @param valueMacs      the value MACs from the outgoing mutations, in wire order
     * @param newVersion     the new collection version
     * @param collectionName the collection type
     * @return the paired snapshot and patch MACs
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdAntiTampering", exports = "computeOutgoingSnapshotAndPatchMacs", adaptation = WhatsAppAdaptation.ADAPTED)
    public static OutgoingMacs computeOutgoingSnapshotAndPatchMacs(
            SecretKeySpec snapshotMacKey,
            SecretKeySpec patchMacKey,
            byte[] newLtHash,
            SequencedCollection<byte[]> valueMacs,
            long newVersion,
            SyncPatchType collectionName
    ) {
        var snapshotMac = computeSnapshotMac(snapshotMacKey, newLtHash, newVersion, collectionName);
        var patchMac = computePatchMac(patchMacKey, snapshotMac, valueMacs, newVersion, collectionName);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "computed outgoing macs for {0} at version {1}", collectionName, newVersion);
        return new OutgoingMacs(snapshotMac, patchMac);
    }

    /**
     * Computes the snapshot MAC under the snapshot-MAC key.
     *
     * <p>The formula is
     * {@code HMAC-SHA256(snapshotMacKey, ltHash || version8 || collectionUtf8)}
     * where {@code version8} is an 8-byte big-endian encoding of the version
     * (the upper 4 bytes are zero in practice because versions are unsigned
     * 32-bit counters).
     *
     * @implNote
     * This implementation emits the 8 version bytes inline rather than
     * allocating a temporary buffer. The shift sequence reproduces
     * {@code WAWebSyncdCryptoUtils.to64BitNetworkOrder} byte for byte.
     *
     * @param snapshotMacKey the snapshot MAC key
     * @param ltHash         the LT-Hash to authenticate
     * @param version        the collection version
     * @param type           the collection type
     * @return the 32-byte MAC
     * @throws WhatsAppWebAppStateSyncException.MacComputationFailed if the JCE HMAC primitive fails
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdEncryptionManager", exports = "generateSnapshotMac", adaptation = WhatsAppAdaptation.DIRECT)
    public static byte[] computeSnapshotMac(SecretKeySpec snapshotMacKey, byte[] ltHash, long version, SyncPatchType type) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(snapshotMacKey);

            mac.update(ltHash);

            mac.update((byte) (version >> 56));
            mac.update((byte) (version >> 48));
            mac.update((byte) (version >> 40));
            mac.update((byte) (version >> 32));
            mac.update((byte) (version >> 24));
            mac.update((byte) (version >> 16));
            mac.update((byte) (version >> 8));
            mac.update((byte) version);

            mac.update(type.toBytes());

            return mac.doFinal();
        } catch (GeneralSecurityException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "snapshot mac computation failed for " + type, exception);
            throw new WhatsAppWebAppStateSyncException.MacComputationFailed(exception);
        }
    }

    /**
     * Computes the patch MAC under the patch-MAC key.
     *
     * <p>The formula is
     * {@code HMAC-SHA256(patchMacKey, snapshotMac || valueMac1 || ... || valueMacN || version8 || collectionUtf8)}.
     * The patch MAC therefore depends on the snapshot MAC, which is what binds
     * the per-mutation value MACs and the collection-level LT-Hash digest
     * together into a single per-version authenticator.
     *
     * @implNote
     * This implementation tolerates a {@code null} {@code snapshotMac} by
     * skipping its contribution to the HMAC. WA Web always supplies the
     * wire snapshot MAC; the null branch exists for the very first patch
     * on a fresh collection and for unit tests that exercise the formula
     * without a paired snapshot.
     *
     * @param patchMacKey the patch MAC key
     * @param snapshotMac the snapshot MAC, or {@code null} to omit
     * @param valueMacs   the per-mutation value MACs, in wire order
     * @param version     the collection version
     * @param type        the collection type
     * @return the 32-byte MAC
     * @throws WhatsAppWebAppStateSyncException.MacComputationFailed if the JCE HMAC primitive fails
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdEncryptionManager", exports = "generatePatchMac", adaptation = WhatsAppAdaptation.DIRECT)
    public static byte[] computePatchMac(SecretKeySpec patchMacKey, byte[] snapshotMac, SequencedCollection<byte[]> valueMacs, long version, SyncPatchType type) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(patchMacKey);

            if (snapshotMac != null) {
                mac.update(snapshotMac);
            }

            for (var valueMac : valueMacs) {
                mac.update(valueMac);
            }

            mac.update((byte) (version >> 56));
            mac.update((byte) (version >> 48));
            mac.update((byte) (version >> 40));
            mac.update((byte) (version >> 32));
            mac.update((byte) (version >> 24));
            mac.update((byte) (version >> 16));
            mac.update((byte) (version >> 8));
            mac.update((byte) version);

            mac.update(type.toBytes());

            return mac.doFinal();
        } catch (GeneralSecurityException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "patch mac computation failed for " + type, exception);
            throw new WhatsAppWebAppStateSyncException.MacComputationFailed(exception);
        }
    }

    /**
     * Formats an {@code (indexMac, valueMac)} pair as a colon-delimited hex string.
     *
     * <p>Diagnostic helper consumed by the collection-handler log statements
     * that print per-mutation MAC pairs when a patch or snapshot fails to
     * validate; it is not part of the wire-protocol surface. The truncated form
     * (last 16 hex characters of each side) is the default and the form that
     * flows into production log lines.
     * {@snippet :
     *     // verbose          = "0102030405060708...:090a0b0c0d0e0f10..."
     *     // truncated (true) = "0a1b2c3d4e5f6071:1234567890abcdef"
     * }
     *
     * @param indexMac the index MAC bytes
     * @param valueMac the value MAC bytes
     * @param truncate {@code true} to keep only the last 16 hex characters per side
     * @return the colon-delimited string
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdAntiTampering", exports = "indexAndValueMacToString", adaptation = WhatsAppAdaptation.ADAPTED)
    public static String indexAndValueMacToString(byte[] indexMac, byte[] valueMac, boolean truncate) {
        var indexHex = HexFormat.of().formatHex(indexMac);
        var valueHex = HexFormat.of().formatHex(valueMac);
        var indexSlice = truncate && indexHex.length() > 16
                ? indexHex.substring(indexHex.length() - 16)
                : indexHex;
        var valueSlice = truncate && valueHex.length() > 16
                ? valueHex.substring(valueHex.length() - 16)
                : valueHex;
        return indexSlice + ":" + valueSlice;
    }

    /**
     * Formats an {@code (indexMac, valueMac)} pair with the default truncation.
     *
     * <p>Convenience overload that pins the {@code truncate} parameter of
     * {@link #indexAndValueMacToString(byte[], byte[], boolean)} to
     * {@code true}.
     *
     * @param indexMac the index MAC bytes
     * @param valueMac the value MAC bytes
     * @return the colon-delimited string, truncated to the last 16 hex characters per side
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdAntiTampering", exports = "indexAndValueMacToString", adaptation = WhatsAppAdaptation.DIRECT)
    public static String indexAndValueMacToString(byte[] indexMac, byte[] valueMac) {
        return indexAndValueMacToString(indexMac, valueMac, true);
    }

    /**
     * The direction of a syncd patch relative to this device.
     *
     * <p>The enum has no effect on any wire value (LT-Hash, snapshot MAC, or
     * patch MAC); it is kept so callers that want to log the patch direction
     * have a typed constant.
     *
     * @implNote
     * This implementation does not yet route the direction through to any
     * diagnostic path; {@link #verifyPatchIntegrity} is implicitly
     * {@link #INCOMING} and the outgoing MAC helper does not consult this
     * enum.
     */
    // TODO: plumb the direction through to the debug-data-in-patch logging once that path exists
    @WhatsAppWebModule(moduleName = "WAWebSyncdAntiTampering.flow")
    public enum SyncdPatchDirection {
        /**
         * A patch the relay sent to this device.
         */
        INCOMING,

        /**
         * A patch this device built for upload to the relay.
         */
        OUTGOING
    }
}
