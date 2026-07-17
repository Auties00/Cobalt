package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Builds the outbound {@code GetBusinessEligibility} IQ request stanza.
 * <p>
 * The request refreshes the per-broadcast Meta-Verified, marketing-messages and GenAI
 * eligibility surfaces for the active business account. Each of the three feature toggles
 * opts the reply into the matching projection; a toggle left {@code null} omits its attribute
 * and lets the relay choose whether to surface that feature by default. The {@code from}
 * attribute is echoed only when a companion linked device proxies the request on behalf of
 * the active user.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBizMarketingMessageGetBusinessEligibilityRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutBizMarketingMessageHackBaseIQGetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutBizMarketingMessageBaseIQGetRequestMixin")
public final class SmaxGetBusinessEligibilityRequest implements SmaxStanza.Request {
    /**
     * The optional {@code meta_verified} attribute on the {@code <features/>} child.
     * <p>
     * When non-{@code null} the attribute is emitted and the reply carries the
     * {@link SmaxGetBusinessEligibilityResponse.Success.MetaVerified} projection.
     */
    private final String featuresMetaVerified;

    /**
     * The optional {@code marketing_messages} attribute on the {@code <features/>} child.
     * <p>
     * When non-{@code null} the attribute is emitted and the reply carries the
     * {@link SmaxGetBusinessEligibilityResponse.Success.MarketingMessages} projection.
     */
    private final String featuresMarketingMessages;

    /**
     * The optional {@code genai} attribute on the {@code <features/>} child.
     * <p>
     * When non-{@code null} the attribute is emitted and the reply carries the
     * {@link SmaxGetBusinessEligibilityResponse.Success.Genai} projection.
     */
    private final String featuresGenai;

    /**
     * The optional {@code from} attribute echoed onto the outbound IQ.
     * <p>
     * The active user {@link Jid} is the only legal value; {@code null} omits the attribute,
     * which is the default because the upstream RPC never propagates an {@code iqFrom}.
     */
    private final Jid fromUserJid;

    /**
     * Constructs a request with all three feature toggles unset and no {@code from} echo.
     * <p>
     * Probes the relay for whichever features it surfaces by default, with no opt-in selection.
     */
    public SmaxGetBusinessEligibilityRequest() {
        this(null, null, null, null);
    }

    /**
     * Constructs a request with the three optional feature toggles and no {@code from} echo.
     *
     * @param featuresMetaVerified      the optional Meta-Verified toggle attribute; may be {@code null}
     * @param featuresMarketingMessages the optional marketing-messages toggle attribute; may be {@code null}
     * @param featuresGenai             the optional GenAI toggle attribute; may be {@code null}
     */
    public SmaxGetBusinessEligibilityRequest(String featuresMetaVerified,
                   String featuresMarketingMessages,
                   String featuresGenai) {
        this(featuresMetaVerified, featuresMarketingMessages, featuresGenai, null);
    }

    /**
     * Constructs a request with the three optional feature toggles and an optional {@code from} echo.
     * <p>
     * The {@code fromUserJid} overload is used when a companion linked device proxies the
     * request on behalf of the active user.
     *
     * @param featuresMetaVerified      the optional Meta-Verified toggle attribute; may be {@code null}
     * @param featuresMarketingMessages the optional marketing-messages toggle attribute; may be {@code null}
     * @param featuresGenai             the optional GenAI toggle attribute; may be {@code null}
     * @param fromUserJid               the optional user {@link Jid} to echo onto the {@code from} attribute; may be {@code null}
     */
    public SmaxGetBusinessEligibilityRequest(String featuresMetaVerified,
                   String featuresMarketingMessages,
                   String featuresGenai,
                   Jid fromUserJid) {
        this.featuresMetaVerified = featuresMetaVerified;
        this.featuresMarketingMessages = featuresMarketingMessages;
        this.featuresGenai = featuresGenai;
        this.fromUserJid = fromUserJid;
    }

    /**
     * Returns the optional Meta-Verified toggle.
     *
     * @return an {@link Optional} carrying the toggle, or empty when the attribute was omitted
     */
    public Optional<String> featuresMetaVerified() {
        return Optional.ofNullable(featuresMetaVerified);
    }

    /**
     * Returns the optional marketing-messages toggle.
     *
     * @return an {@link Optional} carrying the toggle, or empty when the attribute was omitted
     */
    public Optional<String> featuresMarketingMessages() {
        return Optional.ofNullable(featuresMarketingMessages);
    }

    /**
     * Returns the optional GenAI toggle.
     *
     * @return an {@link Optional} carrying the toggle, or empty when the attribute was omitted
     */
    public Optional<String> featuresGenai() {
        return Optional.ofNullable(featuresGenai);
    }

    /**
     * Returns the optional {@code from} echo.
     *
     * @return an {@link Optional} carrying the user {@link Jid}, or empty when no echo was supplied
     */
    public Optional<Jid> fromUserJid() {
        return Optional.ofNullable(fromUserJid);
    }

    /**
     * Builds the outbound IQ stanza ready for dispatch.
     * <p>
     * Stamps the {@code xmlns="w:biz"} envelope with {@code to} addressed to the user server
     * and {@code type="get"}, emits a {@code <features/>} child carrying only the toggle
     * attributes whose value is non-{@code null}, and echoes the optional {@code from}
     * attribute when supplied. The {@code id} attribute is appended by Cobalt's send path.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <features/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBizMarketingMessageGetBusinessEligibilityRequest",
            exports = "makeGetBusinessEligibilityRequest", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizMarketingMessageHackBaseIQGetRequestMixin",
            exports = "mergeHackBaseIQGetRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizMarketingMessageBaseIQGetRequestMixin",
            exports = "mergeBaseIQGetRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var featuresBuilder = new StanzaBuilder()
                .description("features");
        if (featuresMetaVerified != null) {
            featuresBuilder.attribute("meta_verified", featuresMetaVerified);
        }
        if (featuresMarketingMessages != null) {
            featuresBuilder.attribute("marketing_messages", featuresMarketingMessages);
        }
        if (featuresGenai != null) {
            featuresBuilder.attribute("genai", featuresGenai);
        }
        var featuresNode = featuresBuilder.build();
        var builder = new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz")
                .attribute("to", JidServer.user())
                .attribute("type", "get");
        if (fromUserJid != null) {
            builder.attribute("from", fromUserJid);
        }
        return builder.content(featuresNode);
    }

    /**
     * Compares this request to another object for value equality across all four fields.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxGetBusinessEligibilityRequest} with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGetBusinessEligibilityRequest) obj;
        return Objects.equals(this.featuresMetaVerified, that.featuresMetaVerified)
                && Objects.equals(this.featuresMarketingMessages, that.featuresMarketingMessages)
                && Objects.equals(this.featuresGenai, that.featuresGenai)
                && Objects.equals(this.fromUserJid, that.fromUserJid);
    }

    /**
     * Returns a hash code derived from all four fields.
     *
     * @return the combined hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(featuresMetaVerified, featuresMarketingMessages, featuresGenai, fromUserJid);
    }

    /**
     * Returns a debug rendering listing the three toggles and the optional {@code from} echo.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxGetBusinessEligibilityRequest[featuresMetaVerified=" + featuresMetaVerified
                + ", featuresMarketingMessages=" + featuresMarketingMessages
                + ", featuresGenai=" + featuresGenai
                + ", fromUserJid=" + fromUserJid + ']';
    }
}
