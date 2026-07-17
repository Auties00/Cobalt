package com.github.auties00.cobalt.wire.stanza.smax.mdcompanion;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Arrays;
import java.util.Objects;

/**
 * Models the outbound
 * {@code <iq type="result"><hosted-pair-set><device-identity/></hosted-pair-set></iq>} reply a
 * hosted (Meta-hosted, business-platform) companion emits after verifying a
 * {@link SmaxMdSetRegResponse}.
 *
 * <p>Hosted accounts, recognised by the ADV {@code ADVEncryptionType.HOSTED} signal inside the
 * inbound device identity, reply with the bare {@code <hosted-pair-set/>} envelope rather than the
 * regular {@code <pair-device-sign/>} envelope: key-attestation and GPIA are not signalled, and
 * there is no {@code key-index} attribute on the inner {@code <device-identity/>}. Non-hosted
 * companions use {@link SmaxMdSetRegResponseClient} instead, and rejection flows use
 * {@link SmaxMdSetRegResponseError}.
 *
 * @implNote This implementation folds the WA Web hosted-companion bundle mixin into the builder:
 * the outer envelope is pinned to {@code <iq to="s.whatsapp.net" type="result">} and the inner
 * {@code <hosted-pair-set/>} carries a single {@code <device-identity/>} child with no attributes.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutMdSetRegResponseHostedClientResponse")
@WhatsAppWebModule(moduleName = "WASmaxOutMdHostedCompanionSetRegResponseBundleMixin")
public final class SmaxMdSetRegResponseHostedClient implements SmaxStanza.Request {
    /**
     * Holds the {@code id} of the inbound IQ being replied to.
     *
     * <p>Echoed into the outbound {@code <iq id="..."/>} attribute.
     */
    private final String iqId;

    /**
     * Holds the signed device-identity bytes carried in the inner {@code <device-identity/>}.
     *
     * <p>The re-encoded {@code ADVSignedDeviceIdentity} produced by the hosted companion; the
     * upstream signing path additionally prefixes the device identity with the
     * {@code ADV_HOSTED_PREFIX_DEVICE_IDENTITY_ACCOUNT_SIGNATURE} constant before HMAC
     * verification.
     */
    private final byte[] deviceIdentity;

    /**
     * Constructs a hosted pair-success reply.
     *
     * <p>Callers typically derive {@code iqId} from the matching {@link SmaxMdSetRegResponse} and
     * obtain {@code deviceIdentity} from the post-pair signing pipeline.
     *
     * @param iqId           the inbound IQ id; never {@code null}
     * @param deviceIdentity the signed device-identity bytes; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SmaxMdSetRegResponseHostedClient(String iqId, byte[] deviceIdentity) {
        this.iqId = Objects.requireNonNull(iqId, "iqId cannot be null");
        this.deviceIdentity = Objects.requireNonNull(deviceIdentity, "deviceIdentity cannot be null");
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
     * Returns the signed device-identity bytes.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] deviceIdentity() {
        return deviceIdentity;
    }

    /**
     * Builds the outbound hosted pair-success reply stanza.
     *
     * <p>Returns the unfinished {@link StanzaBuilder} so the dispatch path can stamp the wire-level
     * identifiers before flushing, matching the contract of {@link SmaxStanza.Request#toStanza()}.
     *
     * @implNote This implementation does not emit the {@code key-index} attribute on the inner
     * {@code <device-identity/>}; the upstream hosted mixin omits attributes entirely.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutMdSetRegResponseHostedClientResponse",
            exports = "makeSetRegResponseHostedClientResponse",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var deviceIdentityNode = new StanzaBuilder()
                .description("device-identity")
                .content(deviceIdentity)
                .build();
        var hostedPairSetNode = new StanzaBuilder()
                .description("hosted-pair-set")
                .content(deviceIdentityNode)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("id", iqId)
                .attribute("to", "s.whatsapp.net")
                .attribute("type", "result")
                .content(hostedPairSetNode);
    }

    /**
     * Compares this reply to another object for value equality.
     *
     * <p>Two replies are equal when their IQ id matches and their device-identity bytes match
     * element by element.
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
        var that = (SmaxMdSetRegResponseHostedClient) obj;
        return Objects.equals(this.iqId, that.iqId)
                && Arrays.equals(this.deviceIdentity, that.deviceIdentity);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>The device-identity bytes contribute through {@link Arrays#hashCode(byte[])} so equal
     * contents yield equal codes.
     *
     * @return the hash code derived from the IQ id and device-identity bytes
     */
    @Override
    public int hashCode() {
        var result = Objects.hash(iqId);
        result = 31 * result + Arrays.hashCode(deviceIdentity);
        return result;
    }

    /**
     * Returns a debug string listing the IQ id and device-identity bytes.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxMdSetRegResponseHostedClient[iqId=" + iqId
                + ", deviceIdentity=" + Arrays.toString(deviceIdentity) + ']';
    }
}
