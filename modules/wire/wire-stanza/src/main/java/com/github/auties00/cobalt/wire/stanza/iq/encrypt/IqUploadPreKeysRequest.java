package com.github.auties00.cobalt.wire.stanza.iq.encrypt;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.cobalt.wire.core.util.RandomIdUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Builds the {@code <iq xmlns="encrypt" type="set"/>} that uploads the local Signal pre-key bundle
 * to the relay.
 *
 * <p>This request replenishes the server-side stash of one-time pre-keys during steady-state
 * operation. WA Web fires it on two triggers: a {@code <notification type="encrypt"><count
 * value="N"/></notification>} push that signals the server-side stash is low, and a
 * {@link IqDigestKeyResponse digest mismatch} that invalidates the local bundle and forces a fresh
 * upload. WA Web's batch size is fixed at {@code UPLOAD_KEYS_COUNT = 812}; Cobalt accepts any
 * non-empty list. The request body packs five flat children: {@code <registration/>} (four-byte
 * big-endian), {@code <type/>} (one byte), {@code <identity/>} (thirty-two bytes), {@code <list/>}
 * (one {@code <key/>} per pre-key) and {@code <skey/>} (the signed pre-key subtree).
 */
@WhatsAppWebModule(moduleName = "WAWebUploadPreKeysJob")
public final class IqUploadPreKeysRequest implements IqStanza.Request {
    /**
     * The local device's registration id; serialised as a four-byte big-endian unsigned integer
     * into the {@code <registration/>} child.
     */
    private final int registrationId;

    /**
     * The single-byte Signal key-bundle type marker carried by the {@code <type/>} child.
     */
    private final byte keyBundleType;

    /**
     * The thirty-two-byte long-term identity public key, written verbatim into the
     * {@code <identity/>} child.
     */
    private final byte[] identityPublicKey;

    /**
     * The non-empty list of one-time pre-keys, each rendered into one {@code <key/>} grandchild
     * under the {@code <list/>} wrapper.
     */
    private final List<IqUploadPreKeysPreKey> preKeys;

    /**
     * The currently advertised signed pre-key, rendered into the {@code <skey/>} child.
     */
    private final IqUploadPreKeysSignedPreKey signedPreKey;

    /**
     * Constructs an upload request for the supplied key bundle.
     *
     * <p>The {@code preKeys} list is defensively copied; the caller is responsible for marking each
     * uploaded {@code id} against the local Signal store so identifiers are not reused.
     *
     * @param registrationId    the local device's registration id
     * @param keyBundleType     the Signal key-bundle type marker
     * @param identityPublicKey the local identity public key bytes
     * @param preKeys           the one-time pre-keys to upload, non-empty
     * @param signedPreKey      the current signed pre-key
     * @throws NullPointerException     if any reference argument is {@code null}
     * @throws IllegalArgumentException if {@code preKeys} is empty
     */
    public IqUploadPreKeysRequest(int registrationId, byte keyBundleType, byte[] identityPublicKey,
                                  List<IqUploadPreKeysPreKey> preKeys, IqUploadPreKeysSignedPreKey signedPreKey) {
        Objects.requireNonNull(identityPublicKey, "identityPublicKey cannot be null");
        Objects.requireNonNull(preKeys, "preKeys cannot be null");
        Objects.requireNonNull(signedPreKey, "signedPreKey cannot be null");
        if (preKeys.isEmpty()) {
            throw new IllegalArgumentException("preKeys cannot be empty");
        }
        this.registrationId = registrationId;
        this.keyBundleType = keyBundleType;
        this.identityPublicKey = identityPublicKey;
        this.preKeys = List.copyOf(preKeys);
        this.signedPreKey = signedPreKey;
    }

    /**
     * Returns the registration id.
     *
     * @return the registration id
     */
    public int registrationId() {
        return registrationId;
    }

    /**
     * Returns the Signal key-bundle type marker.
     *
     * @return the type marker
     */
    public byte keyBundleType() {
        return keyBundleType;
    }

    /**
     * Returns the identity public key bytes.
     *
     * @return the thirty-two-byte identity public key
     */
    public byte[] identityPublicKey() {
        return identityPublicKey;
    }

    /**
     * Returns the unmodifiable list of one-time pre-keys.
     *
     * @return the pre-keys
     */
    public List<IqUploadPreKeysPreKey> preKeys() {
        return preKeys;
    }

    /**
     * Returns the signed pre-key.
     *
     * @return the signed pre-key
     */
    public IqUploadPreKeysSignedPreKey signedPreKey() {
        return signedPreKey;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces an {@code <iq>} addressed to {@link JidServer#user()} with {@code xmlns="encrypt"}
     * and {@code type="set"}, carrying the five flat children ({@code <registration/>},
     * {@code <type/>}, {@code <identity/>}, {@code <list/>}, {@code <skey/>}) in order. Each pre-key
     * is rendered through {@link IqUploadPreKeysPreKey#toStanza()} and appended to the {@code <list/>}
     * payload.
     *
     * @implNote
     * The registration id is packed via
     * {@link DataUtils#intToBytes(int, int) DataUtils.intToBytes(registrationId, 4)} so the wire
     * content is four bytes regardless of magnitude.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUploadPreKeysJob",
            exports = "uploadPreKeys", adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var registrationNode = new StanzaBuilder()
                .description("registration")
                .content(DataUtils.intToBytes(registrationId, 4))
                .build();
        var typeBytes = new byte[]{keyBundleType};
        var typeNode = new StanzaBuilder()
                .description("type")
                .content(typeBytes)
                .build();
        var identityNode = new StanzaBuilder()
                .description("identity")
                .content(identityPublicKey)
                .build();
        var keyNodes = new ArrayList<Stanza>(preKeys.size());
        for (var preKey : preKeys) {
            keyNodes.add(preKey.toStanza());
        }
        var listNode = new StanzaBuilder()
                .description("list")
                .content(keyNodes)
                .build();
        var skeyNode = signedPreKey.toNode();
        return new StanzaBuilder()
                .description("iq")
                .attribute("id", RandomIdUtils.newId())
                .attribute("xmlns", "encrypt")
                .attribute("type", "set")
                .attribute("to", JidServer.user())
                .content(registrationNode, typeNode, identityNode, listNode, skeyNode);
    }

    /**
     * Compares this request to another instance for equality.
     *
     * @param obj the candidate instance
     * @return {@code true} when {@code obj} is an {@code IqUploadPreKeysRequest} carrying
     *         identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqUploadPreKeysRequest) obj;
        return this.registrationId == that.registrationId
                && this.keyBundleType == that.keyBundleType
                && Arrays.equals(this.identityPublicKey, that.identityPublicKey)
                && Objects.equals(this.preKeys, that.preKeys)
                && Objects.equals(this.signedPreKey, that.signedPreKey);
    }

    /**
     * Returns a hash code derived from every carried field.
     *
     * @return the combined hash
     */
    @Override
    public int hashCode() {
        var result = Objects.hash(registrationId, keyBundleType, preKeys, signedPreKey);
        result = 31 * result + Arrays.hashCode(identityPublicKey);
        return result;
    }

    /**
     * Returns the record-style rendering for this request.
     *
     * @return the rendered string
     */
    @Override
    public String toString() {
        return "IqUploadPreKeysRequest[registrationId=" + registrationId
                + ", keyBundleType=" + keyBundleType
                + ", identityPublicKey=" + Arrays.toString(identityPublicKey)
                + ", preKeys=" + preKeys
                + ", signedPreKey=" + signedPreKey + ']';
    }
}
