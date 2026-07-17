package com.github.auties00.cobalt.sync.crypto;

import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionDataBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionDataSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionData;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * A single mutation that has been encrypted and authenticated for upload.
 *
 * <p>Produced by the outgoing patch builder that runs every time the local
 * device wants to push an app-state change (chat archive, mute, pin, label,
 * and similar) up to the relay. The four fields are the subset of the
 * encryption-wrapper result that the patch serializer actually places on the
 * wire: {@link #indexMac} goes into the {@code SyncdRecord.index.blob} slot
 * and {@link #encryptedValue} into {@code SyncdRecord.value.blob}, while
 * {@link #keyId} and {@link #operation} flow into the surrounding
 * {@code SyncdMutation} envelope.
 *
 * @param indexMac       the HMAC-SHA256 of the UTF-8 index bytes
 * @param encryptedValue the wire payload {@code IV (16) || ciphertext || valueMac (32)}
 * @param keyId          the sync key id used for encryption
 * @param operation      the sync operation
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdEncryptMutations")
@WhatsAppWebModule(moduleName = "WAWebSyncdEncryptMutationsWrapper")
@WhatsAppWebModule(moduleName = "WAWebSyncdRequestEncode")
public record EncryptedMutation(
        byte[] indexMac,
        byte[] encryptedValue,
        byte[] keyId,
        SyncdOperation operation
) {
    /**
     * The logger for {@link EncryptedMutation}.
     */
    private static final System.Logger LOGGER = Log.get(EncryptedMutation.class);

    /**
     * Encrypts a pending mutation into its wire form.
     *
     * <p>Called from the outgoing patch builder once per pending mutation. SET
     * mutations encrypt under the device's currently active sync key; REMOVE
     * mutations re-encrypt under the original key id of the matching SET (the
     * caller resolves that lookup before invoking this factory and passes the
     * resolved {@code keys} and {@code keyId}).
     *
     * @implNote
     * This implementation merges three responsibilities that WA Web splits
     * across {@code WAWebSyncdEncryptMutations.syncdEncryptMutation},
     * {@code WAWebSyncdEncryptMutationsWrapper.encryptMutation}, and
     * {@code WAWebSyncdRequestEncode.encodeSyncActionData}:
     * <ul>
     *   <li>The protobuf encoding of {@link SyncActionData} via
     *       {@link SyncActionDataSpec#encode}; the WA Web fatal-error WAM
     *       counter is not emitted (Cobalt does not run WAM), but the
     *       throw on serialization failure is preserved as a
     *       {@link WhatsAppWebAppStateSyncException.UnexpectedError}.</li>
     *   <li>The AES-CBC encryption with a fresh IV, the HMAC-SHA512 MAC
     *       over the AAD prefix, and the concatenation into the wire
     *       layout.</li>
     *   <li>The reshape of the WA Web wrapper result; in Cobalt the only
     *       fields kept are those that the outgoing serializer consumes,
     *       and the value MAC is recomputable on demand via
     *       {@link #valueMac()} rather than stored separately.</li>
     * </ul>
     * REMOVE-mutation key resolution and per-{@code keyId} {@code keyData}
     * caching live in {@code WAWebSyncdEncryptMutationsWrapper} on the WA
     * Web side; on the Cobalt side they live in the caller, which holds a
     * single {@link MutationKeys} per pipeline pass and reuses it across
     * every mutation that shares the key id.
     *
     * @param patch the pending mutation, with its action payload and operation
     * @param keys  the derived mutation keys for the relevant key id
     * @param keyId the raw sync key id bytes
     * @return the wire-ready {@link EncryptedMutation}
     * @throws GeneralSecurityException if a JCE primitive fails
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError if the action data fails to encode
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdEncryptMutations", exports = "syncdEncryptMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdEncryptMutationsWrapper", exports = "encryptMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdRequestEncode", exports = "encodeSyncActionData", adaptation = WhatsAppAdaptation.ADAPTED)
    public static EncryptedMutation of(
            SyncPendingMutation patch,
            MutationKeys keys,
            byte[] keyId
    ) throws GeneralSecurityException {
        var mutation = patch.mutation();
        var indexBytes = mutation.index().getBytes(StandardCharsets.UTF_8);
        var padding = MutationKeys.generatePadding(indexBytes.length, 0);
        var actionData = new SyncActionDataBuilder()
                .index(indexBytes)
                .value(mutation.value().orElse(null))
                .padding(padding)
                .version(mutation.actionVersion())
                .build();

        byte[] plaintext;
        try {
            plaintext = SyncActionDataSpec.encode(actionData);
        } catch (Exception exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "action data protobuf serialization failed", exception);
            throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                    "action data protobuf serialization failed", exception
            );
        }

        var ivAndCipherText = keys.generateCipherText(plaintext);

        var associatedData = MutationKeys.generateAssociatedData(mutation.operation(), keyId);

        var valueMac = keys.generateMac(associatedData, ivAndCipherText);

        var encryptedValue = new byte[ivAndCipherText.length + valueMac.length];
        System.arraycopy(ivAndCipherText, 0, encryptedValue, 0, ivAndCipherText.length);
        System.arraycopy(valueMac, 0, encryptedValue, ivAndCipherText.length, valueMac.length);

        var indexMacResult = keys.generateIndexMac(indexBytes);

        if (Log.TRACE) LOGGER.log(Level.TRACE, "encrypted mutation built: operation={0}, keyId={1}, valueLen={2}", mutation.operation(), keyId, encryptedValue.length);

        return new EncryptedMutation(indexMacResult, encryptedValue, keyId, mutation.operation());
    }

    /**
     * Returns the trailing 32 bytes of {@link #encryptedValue}.
     *
     * <p>Feeds the outgoing-patch MAC chaining: the patch MAC is computed over
     * the concatenation of the snapshot MAC and every mutation's value MAC, and
     * the value MAC for an outgoing mutation lives at the tail of its own
     * ciphertext. The result equals
     * {@link MutationKeys#valueMacFromIndexAndValueCipherText(byte[])}
     * applied to {@link #encryptedValue}.
     *
     * @return the 32-byte value MAC
     */
    public byte[] valueMac() {
        return MutationKeys.valueMacFromIndexAndValueCipherText(encryptedValue);
    }
}
