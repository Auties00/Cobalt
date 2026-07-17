package com.github.auties00.cobalt.wire.stanza.smax.message;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Objects;
import java.util.Optional;

/**
 * Closes the set of addressing modes a {@link SmaxMessagePublishNewsletterRequest} can carry.
 *
 * <p>A publish is addressed in one of two ways. {@link WithServerId} references a previously
 * delivered newsletter message by stanza id plus that message's server id; it is used for a
 * question-response, reaction, reaction-revoke, or poll-vote tied to that message. {@link WithClientIdOnly}
 * addresses a brand-new post by stanza id only, optionally carrying a msg-meta-origin marker and an
 * optional sender content-type-media RCAT payload. In both arms the caller pre-builds the inner
 * content as a {@link Stanza}, so this type does not model every newsletter publish payload shape.
 *
 * @implNote
 * This implementation models the WhatsApp Web
 * {@code clientNewsletterAndServerOrNewsletterIDMixinGroup} disjunction as a sealed interface with two
 * final implementations.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutMessagePublishClientNewsletterAndServerOrNewsletterIDMixinGroup")
public sealed interface SmaxMessagePublishNewsletterPayload permits SmaxMessagePublishNewsletterPayload.WithServerId, SmaxMessagePublishNewsletterPayload.WithClientIdOnly {

    /**
     * Represents the addressing variant for a publish that references a previously delivered
     * newsletter message by its server id.
     *
     * <p>This variant is used for question-response, reaction, reaction-revoke, and poll-vote
     * publishes; {@link #innerContent()} carries the pre-built variant payload shape.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutMessagePublishNewsletterClientAndServerIDMixin")
    @WhatsAppWebModule(moduleName = "WASmaxOutMessagePublishNewsletterQuestionResponsePublishOrReactionOrReactionRevokeOrPollVoteMixinGroup")
    final class WithServerId implements SmaxMessagePublishNewsletterPayload {
        /**
         * Holds the locally-generated publish stanza id.
         */
        private final String stanzaId;

        /**
         * Holds the targeted message's server id within the newsletter.
         */
        private final long messageServerId;

        /**
         * Holds the pre-built inner content payload, one of the four
         * {@code WASmaxOutMessagePublishNewsletter*} variants (question-response-publish, reaction,
         * reaction-revoke, poll-vote); the caller selects the variant by constructing the appropriate
         * stanza tree before passing it in.
         */
        private final Stanza innerContent;

        /**
         * Constructs a server-id-addressed payload targeting a specific previously delivered newsletter
         * message.
         *
         * @param stanzaId        the publish stanza id; never {@code null}
         * @param messageServerId the targeted message's server id
         * @param innerContent    the variant-shaped inner payload; never {@code null}
         * @throws NullPointerException if {@code stanzaId} or {@code innerContent} is {@code null}
         */
        public WithServerId(String stanzaId, long messageServerId, Stanza innerContent) {
            this.stanzaId = Objects.requireNonNull(stanzaId, "stanzaId cannot be null");
            this.messageServerId = messageServerId;
            this.innerContent = Objects.requireNonNull(innerContent, "innerContent cannot be null");
        }

        /**
         * Returns the publish stanza id.
         *
         * @return the id; never {@code null}
         */
        public String stanzaId() {
            return stanzaId;
        }

        /**
         * Returns the targeted message's server id.
         *
         * @return the server id
         */
        public long messageServerId() {
            return messageServerId;
        }

        /**
         * Returns the pre-built inner content stanza read by
         * {@link SmaxMessagePublishNewsletterRequest#toStanza()} when fanning out the publish.
         *
         * @return the stanza; never {@code null}
         */
        public Stanza innerContent() {
            return innerContent;
        }

        /**
         * Compares this payload to another object for value equality across every field.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link WithServerId} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (WithServerId) obj;
            return this.messageServerId == that.messageServerId
                    && Objects.equals(this.stanzaId, that.stanzaId)
                    && Objects.equals(this.innerContent, that.innerContent);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(stanzaId, messageServerId, innerContent);
        }

        /**
         * Returns a debug-friendly representation of this payload.
         *
         * <p>The format is intended for logging and is not part of any stable contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxMessagePublishNewsletterPayload.WithServerId[stanzaId=" + stanzaId
                    + ", messageServerId=" + messageServerId
                    + ", innerContent=" + innerContent + ']';
        }
    }

    /**
     * Represents the addressing variant for a brand-new newsletter post that does not reference a
     * prior message.
     *
     * <p>This variant is used for posting a new text, media, poll-creation, edit, revoke, or question
     * message. The optional fields cover the origin marker (CTWA, ad, forward, and similar) and the
     * media RCAT payload carried when the post holds non-text content.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutMessagePublishNewsletterClientIDMixin")
    @WhatsAppWebModule(moduleName = "WASmaxOutMessagePublishNewsletterClientIdContent")
    @WhatsAppWebModule(moduleName = "WASmaxOutMessagePublishMsgMetaOriginMixin")
    @WhatsAppWebModule(moduleName = "WASmaxOutMessagePublishSenderContentTypeMediaRCATMixin")
    final class WithClientIdOnly implements SmaxMessagePublishNewsletterPayload {
        /**
         * Holds the locally-generated publish stanza id.
         */
        private final String stanzaId;

        /**
         * Holds the optional msg-meta-origin tag folded into the {@code <meta origin="..."/>} child;
         * {@code null} when the publish is not an origin-tagged broadcast.
         */
        private final String originTag;

        /**
         * Holds the optional media content id folded into the sender content-type-media RCAT
         * {@code <plaintext mediatype="url" content_id="..."/>} child; {@code null} when the publish
         * does not carry a URL media payload.
         */
        private final String mediaContentId;

        /**
         * Holds the pre-built inner client-id content payload.
         */
        private final Stanza clientIdContent;

        /**
         * Constructs a brand-new-post payload for a text, media, poll-creation, edit, revoke, or
         * question message.
         *
         * <p>The {@code <meta origin>} and {@code <plaintext mediatype="url" content_id>} children are
         * built from {@code originTag} and {@code mediaContentId} by this payload itself; the caller
         * passes the raw values rather than pre-building the stanza children.
         *
         * @param stanzaId        the publish stanza id; never {@code null}
         * @param originTag       the optional msg-meta-origin tag; may be {@code null}
         * @param mediaContentId  the optional URL media content id; may be {@code null}
         * @param clientIdContent the inner client-id content stanza; never {@code null}
         * @throws NullPointerException if {@code stanzaId} or {@code clientIdContent} is {@code null}
         */
        public WithClientIdOnly(String stanzaId,
                                String originTag,
                                String mediaContentId,
                                Stanza clientIdContent) {
            this.stanzaId = Objects.requireNonNull(stanzaId, "stanzaId cannot be null");
            this.originTag = originTag;
            this.mediaContentId = mediaContentId;
            this.clientIdContent = Objects.requireNonNull(clientIdContent, "clientIdContent cannot be null");
        }

        /**
         * Returns the publish stanza id.
         *
         * @return the id; never {@code null}
         */
        public String stanzaId() {
            return stanzaId;
        }

        /**
         * Returns the optional msg-meta-origin tag folded into the {@code <meta origin>} child.
         *
         * @return an {@link Optional} carrying the tag, or {@link Optional#empty()} when omitted
         */
        public Optional<String> originTag() {
            return Optional.ofNullable(originTag);
        }

        /**
         * Returns the optional media content id folded into the sender content-type-media RCAT
         * {@code <plaintext mediatype="url" content_id>} child.
         *
         * @return an {@link Optional} carrying the content id, or {@link Optional#empty()} when omitted
         */
        public Optional<String> mediaContentId() {
            return Optional.ofNullable(mediaContentId);
        }

        /**
         * Returns the optional {@code <meta origin="..."/>} child read by
         * {@link SmaxMessagePublishNewsletterRequest#toStanza()} when folding the marker into the message
         * body, built on demand from {@link #originTag}.
         *
         * @return an {@link Optional} carrying the stanza, or {@link Optional#empty()} when omitted
         */
        @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishMsgMetaOriginMixin", exports = "applyMixin",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Optional<Stanza> msgMetaOrigin() {
            return Optional.ofNullable(originTag)
                    .map(tag -> new StanzaBuilder()
                            .description("meta")
                            .attribute("origin", tag)
                            .build());
        }

        /**
         * Returns the optional sender content-type-media RCAT
         * {@code <plaintext mediatype="url" content_id="..."/>} child read by
         * {@link SmaxMessagePublishNewsletterRequest#toStanza()} when folding the media payload into the
         * message body, built on demand from {@link #mediaContentId}.
         *
         * @return an {@link Optional} carrying the stanza, or {@link Optional#empty()} when no media
         *         payload is being sent
         */
        @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishSenderContentTypeMediaRCATMixin", exports = "applyMixin",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Optional<Stanza> senderContentTypeMediaRcat() {
            return Optional.ofNullable(mediaContentId)
                    .map(id -> new StanzaBuilder()
                            .description("plaintext")
                            .attribute("mediatype", "url")
                            .attribute("content_id", id)
                            .build());
        }

        /**
         * Returns the pre-built inner client-id content stanza.
         *
         * @return the stanza; never {@code null}
         */
        public Stanza clientIdContent() {
            return clientIdContent;
        }

        /**
         * Compares this payload to another object for value equality across every field.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link WithClientIdOnly} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (WithClientIdOnly) obj;
            return Objects.equals(this.stanzaId, that.stanzaId)
                    && Objects.equals(this.originTag, that.originTag)
                    && Objects.equals(this.mediaContentId, that.mediaContentId)
                    && Objects.equals(this.clientIdContent, that.clientIdContent);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(stanzaId, originTag, mediaContentId, clientIdContent);
        }

        /**
         * Returns a debug-friendly representation of this payload.
         *
         * <p>The format is intended for logging and is not part of any stable contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxMessagePublishNewsletterPayload.WithClientIdOnly[stanzaId=" + stanzaId
                    + ", originTag=" + originTag
                    + ", mediaContentId=" + mediaContentId
                    + ", clientIdContent=" + clientIdContent + ']';
        }
    }
}
