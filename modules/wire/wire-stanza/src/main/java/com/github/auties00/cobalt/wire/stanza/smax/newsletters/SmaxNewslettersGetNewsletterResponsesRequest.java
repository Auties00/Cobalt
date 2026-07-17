package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * Fetches the per-subscriber responses to a newsletter question post.
 *
 * <p>Combine an optional {@link SmaxNewslettersGetNewsletterResponsesFilter} (contacts or replied)
 * and an optional free-text search string. The relay echoes the matching
 * {@link SmaxNewslettersGetNewsletterResponsesResponse}. The resulting IQ has shape:
 * {@snippet :
 *     <iq xmlns="newsletter" type="get" to="<newsletterJid>">
 *         <question_responses server_id="120" count="50" before="<cursor>">
 *             <filters><contacts/></filters>
 *             <search text="abc"/>
 *         </question_responses>
 *     </iq>
 * }</p>
 */
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersGetNewsletterResponsesRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersNewsletterIQGetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersBaseIQGetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersBeforeQuestionResponseMixinMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersFilterQuestionResponseMixinMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersSearchQuestionResponseMixinMixin")
public final class SmaxNewslettersGetNewsletterResponsesRequest implements SmaxStanza.Request {
    /**
     * The newsletter {@link Jid} being queried; routed verbatim into the IQ's {@code to} attribute.
     */
    private final Jid newsletterJid;

    /**
     * The server-id of the question message whose responses are being fetched.
     */
    private final long questionResponsesServerId;

    /**
     * The cap on returned {@code <question_response>} entries per round-trip.
     */
    private final int questionResponsesCount;

    /**
     * The optional opaque pagination cursor, a previous slice's tail-cursor handed back verbatim by
     * the caller.
     */
    private final String questionResponsesBefore;

    /**
     * The optional contacts or replied filter; {@code null} disables filtering.
     */
    private final SmaxNewslettersGetNewsletterResponsesFilter filter;

    /**
     * The optional free-text search string applied against the response payloads.
     */
    private final String searchText;

    /**
     * Constructs a new request.
     *
     * @implNote This implementation does not enforce a minimum {@code searchText} length; callers
     * targeting WhatsApp Web parity should drop search strings shorter than three characters before
     * invoking, since the relay only filters on three or more.
     *
     * @param newsletterJid             the newsletter {@link Jid}; never {@code null}
     * @param questionResponsesServerId the question's server-id
     * @param questionResponsesCount    the per-call entry cap
     * @param questionResponsesBefore   the optional pagination cursor; may be {@code null}
     * @param filter                    the optional filter; may be {@code null}
     * @param searchText                the optional search string; may be {@code null}
     * @throws NullPointerException if {@code newsletterJid} is {@code null}
     */
    public SmaxNewslettersGetNewsletterResponsesRequest(Jid newsletterJid, long questionResponsesServerId,
                   int questionResponsesCount, String questionResponsesBefore,
                   SmaxNewslettersGetNewsletterResponsesFilter filter, String searchText) {
        this.newsletterJid = Objects.requireNonNull(newsletterJid, "newsletterJid cannot be null");
        this.questionResponsesServerId = questionResponsesServerId;
        this.questionResponsesCount = questionResponsesCount;
        this.questionResponsesBefore = questionResponsesBefore;
        this.filter = filter;
        this.searchText = searchText;
    }

    /**
     * Returns the newsletter {@link Jid} being queried.
     *
     * @return the {@link Jid}; never {@code null}
     */
    public Jid newsletterJid() {
        return newsletterJid;
    }

    /**
     * Returns the question's server-id.
     *
     * @return the question server-id
     */
    public long questionResponsesServerId() {
        return questionResponsesServerId;
    }

    /**
     * Returns the per-call entry cap.
     *
     * @return the entry cap
     */
    public int questionResponsesCount() {
        return questionResponsesCount;
    }

    /**
     * Returns the optional opaque pagination cursor.
     *
     * @return an {@link Optional} carrying the cursor, or empty when requesting the first slice
     */
    public Optional<String> questionResponsesBefore() {
        return Optional.ofNullable(questionResponsesBefore);
    }

    /**
     * Returns the optional contacts or replied filter.
     *
     * @return an {@link Optional} carrying the filter, or empty when no filter is applied
     */
    public Optional<SmaxNewslettersGetNewsletterResponsesFilter> filter() {
        return Optional.ofNullable(filter);
    }

    /**
     * Returns the optional free-text search string.
     *
     * @return an {@link Optional} carrying the search string, or empty when no search is applied
     */
    public Optional<String> searchText() {
        return Optional.ofNullable(searchText);
    }

    /**
     * Builds the outbound {@code <iq>} stanza carrying the {@code <question_responses>} payload.
     *
     * <p>The {@code <filters>} block is emitted only when {@link #filter()} is present; the
     * {@code <search>} child is emitted only when {@link #searchText()} is present.</p>
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <question_responses>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutNewslettersGetNewsletterResponsesRequest",
            exports = "makeGetNewsletterResponsesRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var children = new ArrayList<Stanza>();
        if (filter != null) {
            var filtersBuilder = new StanzaBuilder()
                    .description("filters");
            switch (filter) {
                case SmaxNewslettersGetNewsletterResponsesFilter.Contacts ignored -> filtersBuilder.content(new StanzaBuilder()
                        .description("contacts")
                        .build());
                case SmaxNewslettersGetNewsletterResponsesFilter.Replied ignored -> filtersBuilder.content(new StanzaBuilder()
                        .description("replied")
                        .build());
            }
            children.add(filtersBuilder.build());
        }
        if (searchText != null) {
            children.add(new StanzaBuilder()
                    .description("search")
                    .attribute("text", searchText)
                    .build());
        }
        var qrBuilder = new StanzaBuilder()
                .description("question_responses")
                .attribute("server_id", questionResponsesServerId)
                .attribute("count", questionResponsesCount);
        if (questionResponsesBefore != null) {
            qrBuilder.attribute("before", questionResponsesBefore);
        }
        if (!children.isEmpty()) {
            qrBuilder.content(children);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "newsletter")
                .attribute("to", newsletterJid)
                .attribute("type", "get")
                .content(qrBuilder.build());
    }

    /**
     * Compares two requests for value equality on every field.
     *
     * @param obj the reference object to compare against
     * @return {@code true} when {@code obj} is a request with equal field values
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxNewslettersGetNewsletterResponsesRequest) obj;
        return this.questionResponsesServerId == that.questionResponsesServerId
                && this.questionResponsesCount == that.questionResponsesCount
                && Objects.equals(this.newsletterJid, that.newsletterJid)
                && Objects.equals(this.questionResponsesBefore, that.questionResponsesBefore)
                && Objects.equals(this.filter, that.filter)
                && Objects.equals(this.searchText, that.searchText);
    }

    /**
     * Returns the hash code derived from every field.
     *
     * @return the combined hash of every field
     */
    @Override
    public int hashCode() {
        return Objects.hash(newsletterJid, questionResponsesServerId, questionResponsesCount,
                questionResponsesBefore, filter, searchText);
    }

    /**
     * Returns a debug representation including every field.
     *
     * @return a record-like rendering of this request
     */
    @Override
    public String toString() {
        return "SmaxNewslettersGetNewsletterResponsesRequest[newsletterJid=" + newsletterJid
                + ", questionResponsesServerId=" + questionResponsesServerId
                + ", questionResponsesCount=" + questionResponsesCount
                + ", questionResponsesBefore=" + questionResponsesBefore
                + ", filter=" + filter
                + ", searchText=" + searchText + ']';
    }
}
