package com.github.auties00.cobalt.wire.stanza.iq.ctwa;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.Objects;

/**
 * Requests the relay-side context record for a Click-To-WhatsApp (CTWA) ad click.
 *
 * <p>This is the outbound {@code <iq xmlns="fb:thrift_iq" type="get">} stanza for the CTWA
 * chat-open flow: when a user taps a "Chat on WhatsApp" ad on Facebook or Instagram, the ad
 * funnel hands the client an {@code (account_number, code, expected_source_url)} triple that
 * identifies the ad creative. This request asks the relay to resolve that triple into the
 * headline, body, thumbnail, and (when integrated) the WAMO automated greeting message that
 * the chat composer should render. The reply is parsed by {@link IqQueryCtwaContextResponse}.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryCtwaContextJob")
public final class IqQueryCtwaContextRequest implements IqStanza.Request {
    /**
     * Holds the business account's phone number as a legacy-formatted JID string.
     *
     * <p>Cobalt accepts a pre-formatted string so callers may use whichever JID factory they
     * already have wired up; the value is serialised verbatim into the {@code <account_number>}
     * grandchild.
     */
    private final String accountNumber;

    /**
     * Holds the CTWA redirect code carried by the originating ad funnel.
     */
    private final String code;

    /**
     * Holds the expected source URL the client received from the ad funnel.
     *
     * <p>The relay echoes this back as an anti-spoofing check: a mismatch between the URL the
     * relay knows for {@code (code, accountNumber)} and the URL the client received causes the
     * relay to refuse the query.
     */
    private final String expectedSourceUrl;

    /**
     * Constructs a new query-CTWA-context request from the three ad-funnel fields.
     *
     * <p>All three arguments originate from the ad funnel; the request fails relay-side if any
     * of them is empty or otherwise fails the relay's anti-spoofing check.
     *
     * @param accountNumber     the business phone as a legacy JID string
     * @param code              the redirect code
     * @param expectedSourceUrl the expected source URL
     * @throws NullPointerException if any argument is {@code null}
     */
    public IqQueryCtwaContextRequest(String accountNumber, String code, String expectedSourceUrl) {
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber cannot be null");
        this.code = Objects.requireNonNull(code, "code cannot be null");
        this.expectedSourceUrl = Objects.requireNonNull(expectedSourceUrl, "expectedSourceUrl cannot be null");
    }

    /**
     * Returns the business phone as a legacy JID string.
     *
     * @return the account number, never {@code null}
     */
    public String accountNumber() {
        return accountNumber;
    }

    /**
     * Returns the CTWA redirect code.
     *
     * @return the code, never {@code null}
     */
    public String code() {
        return code;
    }

    /**
     * Returns the expected source URL.
     *
     * @return the URL, never {@code null}
     */
    public String expectedSourceUrl() {
        return expectedSourceUrl;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces an {@code <iq xmlns="fb:thrift_iq" type="get">} envelope addressed to
     * {@link JidServer#user()} and wrapping three grandchildren in fixed order:
     * {@code <account_number>}, {@code <code>}, and {@code <expected_source_url>}.
     *
     * @implNote
     * This implementation omits the {@code smax_id="CtwaGetContext"} attribute that WA Web
     * stamps on the envelope; the relay does not require it for legacy-IQ dispatch and the
     * value is not surfaced anywhere in Cobalt's stanza model.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope and the three
     *         grandchildren
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebQueryCtwaContextJob",
            exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var accountNumberNode = new StanzaBuilder()
                .description("account_number")
                .content(accountNumber)
                .build();
        var codeNode = new StanzaBuilder()
                .description("code")
                .content(code)
                .build();
        var expectedSourceUrlNode = new StanzaBuilder()
                .description("expected_source_url")
                .content(expectedSourceUrl)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "fb:thrift_iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(accountNumberNode, codeNode, expectedSourceUrlNode);
    }

    /**
     * Compares this request to another object for value equality across all three fields.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is an {@link IqQueryCtwaContextRequest} with equal
     *         {@code accountNumber}, {@code code}, and {@code expectedSourceUrl}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqQueryCtwaContextRequest) obj;
        return Objects.equals(this.accountNumber, that.accountNumber)
                && Objects.equals(this.code, that.code)
                && Objects.equals(this.expectedSourceUrl, that.expectedSourceUrl);
    }

    /**
     * Returns a hash code derived from all three fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountNumber, code, expectedSourceUrl);
    }

    /**
     * Returns a debug string listing all three fields.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "IqQueryCtwaContextRequest[accountNumber=" + accountNumber
                + ", code=" + code
                + ", expectedSourceUrl=" + expectedSourceUrl + ']';
    }
}
