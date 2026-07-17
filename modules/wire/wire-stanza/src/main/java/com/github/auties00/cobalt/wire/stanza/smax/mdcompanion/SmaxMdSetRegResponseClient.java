package com.github.auties00.cobalt.wire.stanza.smax.mdcompanion;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the outbound
 * {@code <iq type="result"><pair-device-sign><device-identity/>[<key_attestation/>][<gpia/>]</pair-device-sign></iq>}
 * reply a regular (non-hosted) companion emits after verifying a {@link SmaxMdSetRegResponse}.
 *
 * <p>The companion countersigns the {@code ADVSignedDeviceIdentity} payload extracted from the
 * inbound pair-success; the device-identity bytes here are the freshly signed variant, and the
 * {@code key-index} is taken from the inner ADV {@code keyIndex} field. Hosted-account companions
 * use {@link SmaxMdSetRegResponseHostedClient} instead, and rejection flows use
 * {@link SmaxMdSetRegResponseError}.
 *
 * @implNote This implementation folds the WA Web regular-companion bundle mixin into the builder:
 * the outer envelope is pinned to {@code <iq to="s.whatsapp.net" type="result">} with the original
 * {@code id} echoed, and the inner {@code <pair-device-sign/>} carries the device-identity child
 * plus the optional {@code <key_attestation/>} (with optional {@code key_id} attribute) and
 * {@code <gpia/>} children when their bytes are present. Absent children are omitted entirely
 * rather than emitted as empty placeholders.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutMdSetRegResponseClientResponse")
@WhatsAppWebModule(moduleName = "WASmaxOutMdRegularCompanionSetRegResponseBundleMixin")
public final class SmaxMdSetRegResponseClient implements SmaxStanza.Request {
    /**
     * Holds the {@code id} of the inbound IQ being replied to.
     *
     * <p>Echoed into the outbound {@code <iq id="..."/>} attribute so the relay can pair request
     * and response.
     */
    private final String iqId;

    /**
     * Holds the {@code key-index} attribute carried on the outbound {@code <device-identity/>}.
     *
     * <p>Mirrors the ADV {@code keyIndex} field decoded from the inner
     * {@code ADVSignedDeviceIdentity}; tells the relay which ADV signing slot is now active for
     * this companion.
     */
    private final int deviceIdentityKeyIndex;

    /**
     * Holds the signed device-identity bytes carried in {@code <device-identity/>}.
     *
     * <p>The re-encoded {@code ADVSignedDeviceIdentity} produced by the companion after stripping
     * the throwaway {@code accountSignatureKey} and signing with the companion's own identity key.
     */
    private final byte[] deviceIdentity;

    /**
     * Holds the optional key-attestation bytes carried in {@code <key_attestation/>}.
     *
     * <p>Present for devices that ship hardware-backed key attestation (Android StrongBox, iOS
     * Secure Enclave); omitted for plain software-keyed companions.
     */
    private final byte[] keyAttestation;

    /**
     * Holds the optional {@code key_id} attribute carried on the {@code <key_attestation/>} child.
     *
     * <p>Echoes the platform-specific attestation key identifier so the server can rotate trusted
     * keys; only meaningful when {@link #keyAttestation()} is also present.
     */
    private final String keyAttestationKeyId;

    /**
     * Holds the optional Google Play Integrity Attestation (GPIA) bytes carried in {@code <gpia/>}.
     *
     * <p>Present for Android companions that ship Play Integrity tokens; omitted for other
     * platforms.
     */
    private final byte[] gpia;

    /**
     * Constructs a regular pair-success reply.
     *
     * <p>Callers typically populate {@code iqId} and {@code deviceIdentity} from the matching
     * {@link SmaxMdSetRegResponse} and supply attestation material only when the host platform
     * offers it.
     *
     * @param iqId                   the inbound IQ id; never {@code null}
     * @param deviceIdentityKeyIndex the ADV key-index
     * @param deviceIdentity         the signed device-identity bytes; never {@code null}
     * @param keyAttestation         the optional key-attestation bytes; may be {@code null}
     * @param keyAttestationKeyId    the optional {@code key_id} attribute; may be {@code null}
     * @param gpia                   the optional GPIA bytes; may be {@code null}
     * @throws NullPointerException if {@code iqId} or {@code deviceIdentity} is {@code null}
     */
    public SmaxMdSetRegResponseClient(String iqId, int deviceIdentityKeyIndex, byte[] deviceIdentity,
                          byte[] keyAttestation, String keyAttestationKeyId, byte[] gpia) {
        this.iqId = Objects.requireNonNull(iqId, "iqId cannot be null");
        this.deviceIdentityKeyIndex = deviceIdentityKeyIndex;
        this.deviceIdentity = Objects.requireNonNull(deviceIdentity, "deviceIdentity cannot be null");
        this.keyAttestation = keyAttestation;
        this.keyAttestationKeyId = keyAttestationKeyId;
        this.gpia = gpia;
    }

    /**
     * Returns the IQ id.
     *
     * @return the id; never {@code null}
     */
    public String iqId() {
        return iqId;
    }

    /**
     * Returns the ADV key-index emitted on {@code <device-identity key-index="..."/>}.
     *
     * @return the key-index value
     */
    public int deviceIdentityKeyIndex() {
        return deviceIdentityKeyIndex;
    }

    /**
     * Returns the signed device-identity bytes.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] deviceIdentity() {
        return deviceIdentity;
    }

    /**
     * Returns the optional key-attestation bytes.
     *
     * @return an {@link Optional} carrying the bytes, or empty when the companion has no
     *         hardware-attested key material
     */
    public Optional<byte[]> keyAttestation() {
        return Optional.ofNullable(keyAttestation);
    }

    /**
     * Returns the optional key-attestation {@code key_id} attribute.
     *
     * @return an {@link Optional} carrying the value, or empty when absent
     */
    public Optional<String> keyAttestationKeyId() {
        return Optional.ofNullable(keyAttestationKeyId);
    }

    /**
     * Returns the optional Google Play Integrity Attestation bytes.
     *
     * @return an {@link Optional} carrying the bytes, or empty for non-Android companions
     */
    public Optional<byte[]> gpia() {
        return Optional.ofNullable(gpia);
    }

    /**
     * Builds the outbound regular pair-success reply stanza.
     *
     * <p>Returns the unfinished {@link StanzaBuilder} so the dispatch path can stamp the wire-level
     * identifiers before flushing, matching the contract of {@link SmaxStanza.Request#toStanza()}.
     *
     * @implNote This implementation produces exactly the four shapes the upstream optional-child
     * helper produces: device-identity alone, plus key-attestation only, plus GPIA only, or all
     * three. The {@code key_id} attribute on {@code <key_attestation/>} is itself optional and is
     * omitted when {@link #keyAttestationKeyId()} is empty.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutMdSetRegResponseClientResponse",
            exports = "makeSetRegResponseClientResponse",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var deviceIdentityNode = new StanzaBuilder()
                .description("device-identity")
                .attribute("key-index", deviceIdentityKeyIndex)
                .content(deviceIdentity)
                .build();
        var pairDeviceSignBuilder = new StanzaBuilder()
                .description("pair-device-sign");
        if (keyAttestation != null) {
            var keyAttestationBuilder = new StanzaBuilder()
                    .description("key_attestation")
                    .content(keyAttestation);
            if (keyAttestationKeyId != null) {
                keyAttestationBuilder.attribute("key_id", keyAttestationKeyId);
            }
            if (gpia != null) {
                var gpiaNode = new StanzaBuilder()
                        .description("gpia")
                        .content(gpia)
                        .build();
                pairDeviceSignBuilder.content(deviceIdentityNode, keyAttestationBuilder.build(), gpiaNode);
            } else {
                pairDeviceSignBuilder.content(deviceIdentityNode, keyAttestationBuilder.build());
            }
        } else if (gpia != null) {
            var gpiaNode = new StanzaBuilder()
                    .description("gpia")
                    .content(gpia)
                    .build();
            pairDeviceSignBuilder.content(deviceIdentityNode, gpiaNode);
        } else {
            pairDeviceSignBuilder.content(deviceIdentityNode);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("id", iqId)
                .attribute("to", "s.whatsapp.net")
                .attribute("type", "result")
                .content(pairDeviceSignBuilder.build());
    }

    /**
     * Compares this reply to another object for value equality.
     *
     * <p>Two replies are equal when their IQ id, key-index, key-attestation key-id, and the three
     * byte-payload fields all match, with byte arrays compared element by element.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is an equal reply
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxMdSetRegResponseClient) obj;
        return this.deviceIdentityKeyIndex == that.deviceIdentityKeyIndex
                && Objects.equals(this.iqId, that.iqId)
                && Arrays.equals(this.deviceIdentity, that.deviceIdentity)
                && Arrays.equals(this.keyAttestation, that.keyAttestation)
                && Objects.equals(this.keyAttestationKeyId, that.keyAttestationKeyId)
                && Arrays.equals(this.gpia, that.gpia);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>The byte-payload fields contribute through {@link Arrays#hashCode(byte[])} so equal
     * contents yield equal codes.
     *
     * @return the hash code derived from all fields
     */
    @Override
    public int hashCode() {
        var result = Objects.hash(iqId, deviceIdentityKeyIndex, keyAttestationKeyId);
        result = 31 * result + Arrays.hashCode(deviceIdentity);
        result = 31 * result + Arrays.hashCode(keyAttestation);
        result = 31 * result + Arrays.hashCode(gpia);
        return result;
    }

    /**
     * Returns a debug string listing every field of the reply.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxMdSetRegResponseClient[iqId=" + iqId
                + ", deviceIdentityKeyIndex=" + deviceIdentityKeyIndex
                + ", deviceIdentity=" + Arrays.toString(deviceIdentity)
                + ", keyAttestation=" + Arrays.toString(keyAttestation)
                + ", keyAttestationKeyId=" + keyAttestationKeyId
                + ", gpia=" + Arrays.toString(gpia) + ']';
    }
}
