package com.github.auties00.cobalt.wire.stanza.iq.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Outbound legacy {@code <iq xmlns="privacy" type="set"><tokens><token/>...</tokens></iq>} stanza
 * that issues a batch of pre-shared privacy tokens against the supplied peer JID.
 * <p>
 * Dispatching this request mints trusted-contact tokens for a peer. WA Web's caller
 * {@code WAWebSendTcTokenChatAction.sendTcToken} mints one on the first reply to a peer and again
 * whenever the peer's device identity changes ({@code WAWebSendTcTokenWhenDeviceIdentityChange}),
 * and the resulting token gates downstream call and messages reputation features that depend on
 * cross-device trust pinning. The {@code lid_trusted_token_issue_to_lid} AB prop selects whether the
 * peer JID should be the peer's LID or the legacy phone-number identity.
 *
 * @implNote
 * This implementation only models the request side; the response carries no payload beyond the
 * envelope echo, which {@link IqSetPrivacyTokensResponse.Success} captures as a marker. The
 * {@link #timestampSeconds} field is the issuance timestamp on the wire, not a server-assigned
 * value; WA Web's caller passes {@code WATimeUtils.unixTime()}.
 */
@WhatsAppWebModule(moduleName = "WAWebSetPrivacyTokensJob")
public final class IqSetPrivacyTokensRequest implements IqStanza.Request {
    /**
     * The peer JID emitted into every token's {@code jid} attribute; the token grants
     * trusted-contact status to this peer.
     */
    private final Jid userJid;

    /**
     * The issuance timestamp (seconds since epoch) emitted as the {@code t} attribute on every
     * token.
     */
    private final long timestampSeconds;

    /**
     * The token types being issued; one {@code <token>} grandchild per entry.
     */
    private final List<IqSetPrivacyTokensTokenType> tokenTypes;

    /**
     * Constructs a new request.
     * <p>
     * Typical callers pass a single-element list containing
     * {@link IqSetPrivacyTokensTokenType#TRUSTED_CONTACT}; WA Web does not currently emit any other
     * type. {@code userJid} is the peer the token is being issued against, typically the peer's LID
     * or phone-number identity per the {@code lid_trusted_token_issue_to_lid} AB prop.
     *
     * @implNote
     * This implementation defensively copies {@code tokenTypes} via
     * {@link List#copyOf(java.util.Collection)} and rejects an empty list at construction time; the
     * relay would reject an empty {@code <tokens/>} envelope at parse time but Cobalt surfaces the
     * misuse synchronously.
     *
     * @param userJid          the peer JID; never {@code null}
     * @param timestampSeconds the issuance timestamp in seconds since epoch
     * @param tokenTypes       the token types to issue; never {@code null} and never empty
     * @throws NullPointerException     if {@code userJid} or {@code tokenTypes} is {@code null}
     * @throws IllegalArgumentException if {@code tokenTypes} is empty
     */
    public IqSetPrivacyTokensRequest(Jid userJid, long timestampSeconds, List<IqSetPrivacyTokensTokenType> tokenTypes) {
        Objects.requireNonNull(userJid, "userJid cannot be null");
        Objects.requireNonNull(tokenTypes, "tokenTypes cannot be null");
        if (tokenTypes.isEmpty()) {
            throw new IllegalArgumentException("tokenTypes cannot be empty");
        }
        this.userJid = userJid;
        this.timestampSeconds = timestampSeconds;
        this.tokenTypes = List.copyOf(tokenTypes);
    }

    /**
     * Returns the peer JID this token is being issued against.
     *
     * @return the peer JID; never {@code null}
     */
    public Jid userJid() {
        return userJid;
    }

    /**
     * Returns the issuance timestamp (seconds since epoch).
     * <p>
     * The value is emitted as the {@code t} attribute on every {@code <token>} grandchild. WA Web
     * mirrors it from {@code WATimeUtils.unixTime()} and persists it client-side as
     * {@code tcTokenSenderTimestamp} on the chat row, so the next call to
     * {@code WAWebTrustedContactsUtils.shouldSendNewToken} can decide whether to mint a fresh token.
     *
     * @return the timestamp in seconds since epoch
     */
    public long timestampSeconds() {
        return timestampSeconds;
    }

    /**
     * Returns the token types being issued.
     *
     * @return an unmodifiable list; never {@code null} and never empty
     */
    public List<IqSetPrivacyTokensTokenType> tokenTypes() {
        return tokenTypes;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation serialises every token type into a
     * {@code <token jid=USER_JID t=TS type=TYPE/>} grandchild and wraps the lot in a
     * {@code <tokens>} envelope inside the canonical
     * {@code <iq xmlns="privacy" to="s.whatsapp.net" type="set">} stanza. The
     * {@link #timestampSeconds} field is stringified directly (decimal seconds, no padding) to match
     * WA Web's {@code CUSTOM_STRING(String(r))} construction.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSetPrivacyTokensJob",
            exports = "issuePrivacyToken", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var tokenNodes = new ArrayList<Stanza>();
        for (var type : tokenTypes) {
            var tokenNode = new StanzaBuilder()
                    .description("token")
                    .attribute("jid", userJid)
                    .attribute("t", String.valueOf(timestampSeconds))
                    .attribute("type", type.wire())
                    .build();
            tokenNodes.add(tokenNode);
        }
        var tokensNode = new StanzaBuilder()
                .description("tokens")
                .content(tokenNodes)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "privacy")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(tokensNode);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation compares every typed field by value.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSetPrivacyTokensRequest) obj;
        return this.timestampSeconds == that.timestampSeconds
                && Objects.equals(this.userJid, that.userJid)
                && Objects.equals(this.tokenTypes, that.tokenTypes);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation hashes every typed field consistently with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(userJid, timestampSeconds, tokenTypes);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits a debug-only representation of every typed field; the format is not
     * stable and must not be parsed.
     */
    @Override
    public String toString() {
        return "IqSetPrivacyTokensRequest[userJid=" + userJid
                + ", timestampSeconds=" + timestampSeconds
                + ", tokenTypes=" + tokenTypes + ']';
    }
}
