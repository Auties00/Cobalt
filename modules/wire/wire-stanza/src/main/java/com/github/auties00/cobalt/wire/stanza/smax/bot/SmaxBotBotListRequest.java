package com.github.auties00.cobalt.wire.stanza.smax.bot;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The outbound {@code <iq xmlns="bot" type="get">} stanza builder for fetching the
 * AI-bot directory.
 *
 * <p>Driving code uses this to fetch the WA Web bot directory. The relay answers with a
 * {@link SmaxBotBotListResponse} carrying either a V2 directory (legacy shape, indexed
 * bot entries with theme overrides), a V3 directory (current shape with section
 * display-type hints and a digest), or an error envelope. The optional revision and
 * digest fields tune whether the relay returns the legacy V2 shape and whether it may
 * short-circuit an unchanged directory.
 *
 * @implNote
 * This implementation flattens the WA Web smax mixin chain (botList-IQ plus base-IQ-get)
 * into a single {@link #toStanza()} call; the per-arg {@code <bot jid=...>} children are
 * inlined as a loop rather than going through a separate factory.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBotBotListRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutBotBotListIQMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutBotBaseIQGetRequestMixin")
public final class SmaxBotBotListRequest implements SmaxStanza.Request {
    /**
     * The optional protocol revision; typically {@code "2"} or {@code "3"}.
     *
     * <p>The WA Web initialiser passes {@code "2"} when requesting the legacy V2 shape
     * and omits the attribute to let the relay pick the latest revision; {@code null}
     * preserves the latter behaviour.
     */
    private final String botV;

    /**
     * The optional client-side directory digest.
     *
     * <p>Lets the relay short-circuit with an empty V3 reply when the local directory
     * snapshot matches the server's; {@code null} forces a full fetch.
     */
    private final String botBhash;

    /**
     * The list of bot JIDs to scope the query to; empty for an unconstrained directory
     * fetch.
     *
     * <p>Each JID is emitted as a {@code <bot jid=...>} child. The scoped form refreshes
     * specific bots; an empty list is the standard directory-fetch case.
     */
    private final List<Jid> botArgs;

    /**
     * Constructs a bot-directory request for dispatch through the smax send pipeline.
     *
     * @implNote
     * This implementation defensively copies the bot-arg list via
     * {@link List#copyOf(java.util.Collection)} so caller mutations do not affect the
     * request.
     *
     * @param botV     the optional protocol revision; may be {@code null}
     * @param botBhash the optional digest; may be {@code null}
     * @param botArgs  the bot-JID scope list; never {@code null}, may be empty
     * @throws NullPointerException if {@code botArgs} is {@code null}
     */
    public SmaxBotBotListRequest(String botV, String botBhash, List<Jid> botArgs) {
        this.botV = botV;
        this.botBhash = botBhash;
        Objects.requireNonNull(botArgs, "botArgs cannot be null");
        this.botArgs = List.copyOf(botArgs);
    }

    /**
     * Returns the optional protocol revision.
     *
     * <p>Read by {@link #toStanza()} when deciding whether to stamp the
     * {@code <bot v=...>} attribute.
     *
     * @return an {@link Optional} carrying the revision, or {@link Optional#empty()}
     *         when omitted
     */
    public Optional<String> botV() {
        return Optional.ofNullable(botV);
    }

    /**
     * Returns the optional client-side directory digest.
     *
     * <p>Read by {@link #toStanza()} when deciding whether to stamp the
     * {@code <bot bhash=...>} attribute.
     *
     * @return an {@link Optional} carrying the digest, or {@link Optional#empty()} when
     *         omitted
     */
    public Optional<String> botBhash() {
        return Optional.ofNullable(botBhash);
    }

    /**
     * Returns the bot-JID scope list.
     *
     * <p>Read by {@link #toStanza()} when fanning the entries into {@code <bot jid=...>}
     * children.
     *
     * @return an unmodifiable list; never {@code null}, may be empty
     */
    public List<Jid> botArgs() {
        return botArgs;
    }

    /**
     * Builds the outbound {@code <iq>} stanza ready for dispatch.
     *
     * <p>The stanza has shape
     * {@snippet lang=xml :
     * <iq xmlns="bot" type="get" to="s.whatsapp.net">
     *   <bot v="2"? bhash="..."?>
     *     <bot jid="..."/>
     *     ...
     *   </bot>
     * </iq>
     * }
     * The {@code v} and {@code bhash} attributes are stamped only when the corresponding
     * field is present, and the {@code to} attribute addresses {@link JidServer#user()}.
     * The dispatch layer stamps the {@code id} attribute.
     *
     * @implNote
     * This implementation inlines the per-arg {@code <bot jid=...>} fanout that WA Web
     * factors out into a helper; the result is shape-equivalent.
     *
     * @return a {@link StanzaBuilder} carrying the partially-built IQ envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBotBotListRequest",
            exports = "makeBotListRequest", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBotBotListRequest",
            exports = "makeBotListRequestBotBot",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBotBotListIQMixin",
            exports = "mergeBotListIQMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBotBaseIQGetRequestMixin",
            exports = "mergeBaseIQGetRequestMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var topBotBuilder = new StanzaBuilder()
                .description("bot");
        if (botV != null) {
            topBotBuilder.attribute("v", botV);
        }
        if (botBhash != null) {
            topBotBuilder.attribute("bhash", botBhash);
        }
        for (var argJid : botArgs) {
            var argNode = new StanzaBuilder()
                    .description("bot")
                    .attribute("jid", argJid)
                    .build();
            topBotBuilder.content(argNode);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "bot")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(topBotBuilder.build());
    }

    /**
     * Compares this request to another for value equality on every payload field.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a {@link SmaxBotBotListRequest} with
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
        var that = (SmaxBotBotListRequest) obj;
        return Objects.equals(this.botV, that.botV)
                && Objects.equals(this.botBhash, that.botBhash)
                && Objects.equals(this.botArgs, that.botArgs);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(botV, botBhash, botArgs);
    }

    /**
     * Returns a debug-friendly representation of this request.
     *
     * <p>The format is intended for logging and is not part of the contract.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "SmaxBotBotListRequest[botV=" + botV
                + ", botBhash=" + botBhash
                + ", botArgs=" + botArgs + ']';
    }
}
