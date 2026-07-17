package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.Objects;

/**
 * Builds the {@code <iq xmlns="w:biz:catalog" type="get">} stanza that checks whether a
 * buyer-supplied postcode falls inside a merchant's service area.
 *
 * <p>The stanza is sent from the cart-postcode entry surface after the buyer types or pastes their
 * postcode. The caller first encrypts the postcode with the merchant's direct-connection cypher and
 * then ships this stanza to ask the relay whether the encrypted address resolves to a serviceable
 * location; the relay never sees the plaintext postcode.
 */
@WhatsAppWebModule(moduleName = "WAWebVerifyPostcodeJob")
public final class IqVerifyPostcodeRequest implements IqStanza.Request {
    /**
     * Holds the merchant JID stamped into the {@code biz_jid} attribute of the
     * {@code <verify_postcode/>} child.
     */
    private final Jid businessJid;

    /**
     * Holds the opaque encrypted-postcode blob produced by the buyer-side direct-connection
     * encryption flow, stamped as the body of the {@code <direct_connection_encrypted_info/>}
     * grandchild.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * Constructs a request from the merchant JID and the already-encrypted postcode blob.
     *
     * <p>The blob is produced by the buyer-side direct-connection encryption flow before this
     * constructor is called, so the relay never sees the plaintext postcode.
     *
     * @param businessJid                   the merchant JID; never {@code null}
     * @param directConnectionEncryptedInfo the encrypted-postcode blob; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public IqVerifyPostcodeRequest(Jid businessJid, String directConnectionEncryptedInfo) {
        this.businessJid = Objects.requireNonNull(businessJid, "businessJid cannot be null");
        this.directConnectionEncryptedInfo = Objects.requireNonNull(
                directConnectionEncryptedInfo, "directConnectionEncryptedInfo cannot be null");
    }

    /**
     * Returns the merchant JID.
     *
     * <p>The value is routed verbatim into the {@code biz_jid} attribute of the resulting
     * {@code <verify_postcode/>} child.
     *
     * @return the merchant JID; never {@code null}
     */
    public Jid businessJid() {
        return businessJid;
    }

    /**
     * Returns the encrypted-postcode blob the stanza stamps.
     *
     * <p>The relay treats the blob as opaque and forwards it to the merchant's direct-connection
     * service for decryption.
     *
     * @return the blob; never {@code null}
     */
    public String directConnectionEncryptedInfo() {
        return directConnectionEncryptedInfo;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation materialises a {@code <direct_connection_encrypted_info/>} grandchild
     * wrapped in a {@code <verify_postcode biz_jid/>} child and the {@code w:biz:catalog get} IQ
     * frame routed to the WhatsApp service. WA Web routes the same call through
     * {@code WAWebGraphQLVerifyPostcodeJob.verifyPostcode} when the
     * {@code isGraphQLForVerifyPostcodeEnabled} gating flag is on, but Cobalt keeps the WAP-IQ
     * payload as the single transport.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebVerifyPostcodeJob",
            exports = "VerifyPostcode", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var encryptedInfo = new StanzaBuilder()
                .description("direct_connection_encrypted_info")
                .content(directConnectionEncryptedInfo)
                .build();
        var verifyPostcode = new StanzaBuilder()
                .description("verify_postcode")
                .attribute("biz_jid", businessJid)
                .content(encryptedInfo)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz:catalog")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(verifyPostcode);
    }

    /**
     * Compares this request with another for value equality on the merchant JID and encrypted blob.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an equal request
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqVerifyPostcodeRequest) obj;
        return Objects.equals(this.businessJid, that.businessJid)
                && Objects.equals(this.directConnectionEncryptedInfo, that.directConnectionEncryptedInfo);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(businessJid, directConnectionEncryptedInfo);
    }

    /**
     * Returns a diagnostic string naming the merchant JID and the encrypted blob.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "IqVerifyPostcodeRequest[businessJid=" + businessJid
                + ", directConnectionEncryptedInfo=" + directConnectionEncryptedInfo + ']';
    }
}
