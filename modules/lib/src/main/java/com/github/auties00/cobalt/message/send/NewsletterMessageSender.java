package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.ack.AckParser;
import com.github.auties00.cobalt.ack.AckResult;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.send.stanza.MetaStanza;
import com.github.auties00.cobalt.message.send.stanza.NewsletterStanza;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.FutureProofMessageType;
import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactMessage;
import com.github.auties00.cobalt.wire.linked.message.media.*;
import com.github.auties00.cobalt.wire.linked.message.poll.PollCreationMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollResultSnapshotMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollUpdateMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.message.system.QuestionResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessage;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wam.WamService;

import java.lang.System.Logger.Level;
import java.util.Optional;

/**
 * Publishes a {@link NewsletterMessageInfo} into a newsletter channel.
 *
 * <p>Newsletter messages bypass the Signal envelope: the serialised protobuf
 * payload travels inside a {@code <plaintext>} child of the outer
 * {@code <message to="...@newsletter">} stanza. The wire stanza {@code type}
 * attribute classifies the payload as {@code text}, {@code media},
 * {@code poll}, or {@code reaction}; each method on this sender builds the SMAX
 * shape for one specific content kind.
 */
@WhatsAppWebModule(moduleName = "WAWebNewsletterSendMessageQueryJob")
@WhatsAppWebModule(moduleName = "WASmaxMessagePublishNewsletterRPC")
@WhatsAppWebModule(moduleName = "WASmaxOutMessagePublishNewsletterClientIdContent")
final class NewsletterMessageSender extends MessageSender<NewsletterMessageInfo> {
    /**
     * The logger for {@link NewsletterMessageSender}.
     */
    private static final System.Logger LOGGER = Log.get(NewsletterMessageSender.class);

    /**
     * Defines the {@code edit} attribute value stamped onto a newsletter
     * text-or-media edit ({@code "3"}), distinct from the regular message-edit
     * ({@code "1"}) and pin-in-chat ({@code "2"}) values used by
     * {@link MessageSender#resolveEditAttribute(LinkedMessageContainer)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebAck", exports = "EDIT_ATTR",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final String NEWSLETTER_MSG_EDIT = "3";

    /**
     * Constructs a {@link NewsletterMessageSender} bound to the supplied
     * dependencies.
     *
     * <p>Constructed once by {@link MessageSendingService}; embedders should not
     * instantiate directly.
     *
     * @param client         the {@link LinkedWhatsAppClient} used to dispatch stanzas
     * @param abPropsService the {@link ABPropsService} consulted by the base
     *                       sender
     * @param wamService     the {@link WamService} shared with the base sender
     */
    @WhatsAppWebExport(moduleName = "WAWebNewsletterSendMessageQueryJob", exports = "querySendNewsletterMessage",
            adaptation = WhatsAppAdaptation.ADAPTED)
    NewsletterMessageSender(LinkedWhatsAppClient client, ABPropsService abPropsService, WamService wamService) {
        super(client, abPropsService, wamService);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Routes by the payload's content kind to the matching SMAX stanza
     * builder; question and question-reply containers take a dedicated branch
     * because the meta child uses a different per-shape builder on
     * {@link MetaStanza}.
     *
     * @throws WhatsAppMessageException.Send.Unknown when the payload's content
     *                                               kind is unsupported on
     *                                               newsletters
     */
    @WhatsAppWebExport(moduleName = "WAWebNewsletterSendMessageQueryJob", exports = "querySendNewsletterMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    AckResult doSend(Jid newsletterJid, NewsletterMessageInfo messageInfo) {
        var container = messageInfo.message();
        var containerType = container.futureProofContentType();

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "publishing newsletter message {0} to {1}, contentType={2}",
                    messageInfo.key().id().orElse(null), newsletterJid, containerType);
        }

        if (containerType == FutureProofMessageType.QUESTION || containerType == FutureProofMessageType.QUESTION_REPLY) {
            var innerContent = container.content();
            var mediaSubtype = resolveSmaxMediaType(innerContent);
            var isMedia = !"text".equals(mediaSubtype);
            var payload = LinkedMessageContainerSpec.encode(LinkedMessageContainer.of(innerContent));
            var metaNode = containerType == FutureProofMessageType.QUESTION
                    ? MetaStanza.buildNewsletterQuestion()
                    : MetaStanza.buildNewsletterQuestionReply();
            var plaintextNode = isMedia
                    ? NewsletterStanza.buildPlaintext(payload, mediaSubtype)
                    : NewsletterStanza.buildPlaintext(payload);
            var stanzaBuilder = new StanzaBuilder()
                    .description("message")
                    .attribute("id", messageInfo.key().id().orElseThrow())
                    .attribute("to", newsletterJid)
                    .attribute("type", isMedia ? "media" : "text");
            if (isMedia) {
                stanzaBuilder.attribute("media_id", messageInfo.mediaHandle().orElse(null));
            }
            var stanza = stanzaBuilder.content(metaNode, plaintextNode);
            var ackNode = client.sendNode(stanza);
            var ack = AckParser.parse(ackNode);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "newsletter publish result for {0}: success={1}", newsletterJid, ack.isSuccess());
            }
            return ack;
        } else {
            var message = container.content();

            var stanza = switch (message) {
                case ProtocolMessage p when p.type().orElse(null) == ProtocolMessage.Type.REVOKE ->
                        buildRevoke(messageInfo, newsletterJid);

                case ProtocolMessage p when p.type().orElse(null) == ProtocolMessage.Type.MESSAGE_EDIT ->
                        buildEdit(messageInfo, newsletterJid);

                case PollCreationMessage _ ->
                        buildPoll(messageInfo, newsletterJid, "creation");

                case PollResultSnapshotMessage _ ->
                        buildPoll(messageInfo, newsletterJid, "result_snapshot");

                case ReactionMessage r ->
                        buildReaction(messageInfo, newsletterJid, r);

                case PollUpdateMessage p ->
                        buildPollVote(messageInfo, newsletterJid, p);

                case QuestionResponseMessage _ -> buildQuestionResponse(messageInfo, newsletterJid, container);

                case ExtendedTextMessage t when t.matchedText().isEmpty() ->
                        buildText(messageInfo, newsletterJid);

                case MediaMessage _ -> buildMedia(messageInfo, newsletterJid, resolveSmaxMediaType(message));

                default -> {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "unsupported newsletter message type for {0}", newsletterJid);
                    }
                    throw new WhatsAppMessageException.Send.Unknown("Invalid message type");
                }
            };

            var ackNode = client.sendNode(stanza);
            var ack = AckParser.parse(ackNode);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "newsletter publish result for {0}: success={1}", newsletterJid, ack.isSuccess());
            }
            return ack;
        }
    }

    /**
     * Builds the SMAX stanza for a plain-text newsletter post.
     *
     * <p>Drives the text branch of the publish switch. The outer
     * {@code type="text"} attribute classifies the payload; the body lives in
     * the single {@code <plaintext>} child.
     *
     * @param info          the outgoing {@link NewsletterMessageInfo}
     * @param newsletterJid the newsletter {@link Jid}
     * @return the {@code <message>} {@link StanzaBuilder}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishNewsletterTextMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    private StanzaBuilder buildText(NewsletterMessageInfo info, Jid newsletterJid) {
        var payload = LinkedMessageContainerSpec.encode(info.message());
        var plaintextNode = NewsletterStanza.buildPlaintext(payload);
        return new StanzaBuilder()
                .description("message")
                .attribute("id", info.key().id().orElseThrow())
                .attribute("to", newsletterJid)
                .attribute("type", "text")
                .content(plaintextNode);
    }

    /**
     * Builds the SMAX stanza for a newsletter question-response message.
     *
     * <p>The {@code server_id} attribute carries the parent question's server id
     * so the server can join the response to its question; the meta child
     * carries the question-response marker.
     *
     * @param messageInfo   the outgoing {@link NewsletterMessageInfo}
     * @param newsletterJid the newsletter {@link Jid}
     * @param container     the question-response {@link LinkedMessageContainer}
     * @return the {@code <message>} {@link StanzaBuilder}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishNewsletterQuestionResponsePublishMixin",
            exports = "applyMixin", adaptation = WhatsAppAdaptation.DIRECT)
    private static StanzaBuilder buildQuestionResponse(NewsletterMessageInfo messageInfo, Jid newsletterJid, LinkedMessageContainer container) {
        var payload = LinkedMessageContainerSpec.encode(container);
        var metaNode = MetaStanza.buildNewsletterQuestionResponse();
        var plaintextNode = NewsletterStanza.buildPlaintext(payload);
        return new StanzaBuilder()
                .description("message")
                .attribute("id", messageInfo.key().id().orElseThrow())
                .attribute("to", newsletterJid)
                .attribute("type", "text")
                .attribute("server_id", messageInfo.serverId())
                .content(metaNode, plaintextNode);
    }

    /**
     * Builds the SMAX stanza for a media (or URL) newsletter post.
     *
     * <p>The outer {@code type} is always {@code "media"}; the specific media
     * classification (image, video, gif, audio, ptt, document, sticker, vcard,
     * url, and so on) is stamped on the inner {@code <plaintext>}'s
     * {@code mediatype} attribute, and the optional media handle is written to
     * the outer {@code media_id} attribute.
     *
     * @param info          the outgoing {@link NewsletterMessageInfo}
     * @param newsletterJid the newsletter {@link Jid}
     * @param mediaType     the SMAX media-subtype string
     * @return the {@code <message>} {@link StanzaBuilder}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishNewsletterMediaPublishMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    private StanzaBuilder buildMedia(
            NewsletterMessageInfo info, Jid newsletterJid, String mediaType
    ) {
        var payload = LinkedMessageContainerSpec.encode(info.message());
        var plaintextNode = NewsletterStanza.buildPlaintext(payload, mediaType);
        return new StanzaBuilder()
                .description("message")
                .attribute("id", info.key().id().orElseThrow())
                .attribute("to", newsletterJid)
                .attribute("type", "media")
                .attribute("media_id", info.mediaHandle().orElse(null))
                .content(plaintextNode);
    }

    /**
     * Builds the SMAX stanza for a poll-creation or poll-result-snapshot
     * newsletter post.
     *
     * <p>Shared between the {@link PollCreationMessage} and
     * {@link PollResultSnapshotMessage} branches; the {@code polltype} attribute
     * on the {@code <meta>} child distinguishes the two sub-shapes.
     *
     * @param info          the outgoing {@link NewsletterMessageInfo}
     * @param newsletterJid the newsletter {@link Jid}
     * @param polltype      the poll-type marker ({@code "creation"} or
     *                      {@code "result_snapshot"})
     * @return the {@code <message>} {@link StanzaBuilder}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishContentTypePollCreationMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishContentTypePollResultSnapshotMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    private StanzaBuilder buildPoll(
            NewsletterMessageInfo info, Jid newsletterJid, String polltype
    ) {
        var payload = LinkedMessageContainerSpec.encode(info.message());
        var metaNode = new StanzaBuilder()
                .description("meta")
                .attribute("polltype", polltype)
                .build();
        var plaintextNode = NewsletterStanza.buildPlaintext(payload);
        return new StanzaBuilder()
                .description("message")
                .attribute("id", info.key().id().orElseThrow())
                .attribute("to", newsletterJid)
                .attribute("type", "poll")
                .content(metaNode, plaintextNode);
    }

    /**
     * Builds the SMAX stanza for an admin-revoke newsletter post.
     *
     * <p>The {@code edit="8"} marker is hard-coded by WA Web's
     * {@code applyMixin} regardless of the resolved edit attribute; the stanza
     * carries a single empty {@code <plaintext/>} child.
     *
     * @param info          the outgoing {@link NewsletterMessageInfo}
     * @param newsletterJid the newsletter {@link Jid}
     * @return the {@code <message>} {@link StanzaBuilder}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishNewsletterRevokeMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    private StanzaBuilder buildRevoke(NewsletterMessageInfo info, Jid newsletterJid) {
        var plaintextNode = new StanzaBuilder()
                .description("plaintext")
                .build();
        return new StanzaBuilder()
                .description("message")
                .attribute("id", info.key().id().orElseThrow())
                .attribute("to", newsletterJid)
                .attribute("type", "text")
                .attribute("edit", "8")
                .content(plaintextNode);
    }

    /**
     * Builds the SMAX stanza for a newsletter text or media edit.
     *
     * <p>The edit marker is the hard-coded {@link #NEWSLETTER_MSG_EDIT}
     * ({@code "3"}); media edits also stamp the inner {@code mediatype}
     * attribute on {@code <plaintext>} and the {@code media_id} attribute on the
     * outer {@code <message>}.
     *
     * @param info          the outgoing {@link NewsletterMessageInfo}
     * @param newsletterJid the newsletter {@link Jid}
     * @return the {@code <message>} {@link StanzaBuilder}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishNewsletterEditMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    private StanzaBuilder buildEdit(NewsletterMessageInfo info, Jid newsletterJid) {
        var protocolMessage = (ProtocolMessage) info.message().content();
        var editedMessage = protocolMessage.editedMessage();
        var editedContent = editedMessage.map(LinkedMessageContainer::content).orElse(null);

        var mediaSubtype = editedContent != null ? resolveSmaxMediaType(editedContent) : "text";
        var isMediaEdit = !"text".equals(mediaSubtype);
        var stanzaType = isMediaEdit ? "media" : "text";

        var payload = LinkedMessageContainerSpec.encode(info.message());
        var plaintextNode = isMediaEdit
                ? NewsletterStanza.buildPlaintext(payload, mediaSubtype)
                : NewsletterStanza.buildPlaintext(payload);

        var builder = new StanzaBuilder()
                .description("message")
                .attribute("id", info.key().id().orElseThrow())
                .attribute("to", newsletterJid)
                .attribute("type", stanzaType)
                .attribute("edit", NEWSLETTER_MSG_EDIT);
        if (isMediaEdit) {
            builder.attribute("media_id", info.mediaHandle().orElse(null));
        }
        return builder.content(plaintextNode);
    }

    /**
     * Builds the SMAX stanza for a reaction (or reaction-revoke) on an existing
     * newsletter message.
     *
     * <p>The {@code server_id} attribute carries the parent message's server id;
     * an empty or {@code null} reaction code triggers the revoke shape with
     * {@code edit="7"} and an empty {@code <reaction/>} child, matching WA Web's
     * reaction-revoke mixin.
     *
     * @param info          the outgoing {@link NewsletterMessageInfo}
     * @param newsletterJid the newsletter {@link Jid}
     * @param reaction      the {@link ReactionMessage} payload
     * @return the {@code <message>} {@link StanzaBuilder}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishContentTypeReactionMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishNewsletterReactionMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishNewsletterReactionRevokeMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    private StanzaBuilder buildReaction(
            NewsletterMessageInfo info, Jid newsletterJid, ReactionMessage reaction
    ) {
        var parentServerId = resolveParentServerId(reaction.key().orElse(null));

        var isRevoke = reaction.text().filter(t -> !t.isEmpty()).isEmpty();

        var builder = new StanzaBuilder()
                .description("message")
                .attribute("id", info.key().id().orElseThrow())
                .attribute("to", newsletterJid)
                .attribute("type", "reaction")
                .attribute("server_id", parentServerId);
        if (isRevoke) {
            builder.attribute("edit", "7");
            builder.content(buildReactionRevoke());
        } else {
            builder.content(buildReactionContent(reaction.text().get()));
        }
        return builder;
    }

    /**
     * Builds the {@code <reaction code="..."/>} child carrying the supplied
     * emoji code.
     *
     * <p>Used by the non-revoke branch of {@link #buildReaction}; the code is
     * the canonical reaction string (typically a single emoji).
     *
     * @param s the reaction emoji code
     * @return the {@code <reaction>} {@link Stanza}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishContentTypeReactionMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Stanza buildReactionContent(String s) {
        return new StanzaBuilder()
                .description("reaction")
                .attribute("code", s)
                .build();
    }

    /**
     * Builds the empty {@code <reaction/>} child that accompanies the
     * {@code edit="7"} reaction-revoke marker.
     *
     * <p>Used by the revoke branch of {@link #buildReaction}; the child
     * intentionally carries no attributes.
     *
     * @return the empty {@code <reaction>} {@link Stanza}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishNewsletterReactionRevokeMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Stanza buildReactionRevoke() {
        return new StanzaBuilder()
                .description("reaction")
                .build();
    }

    /**
     * Returns the {@code server_id} of the parent newsletter message referenced
     * by the supplied {@link MessageKey}.
     *
     * <p>Newsletters carry a per-message {@code server_id}; reactions, poll
     * votes, and question responses pin the parent's id into their stanza so the
     * server can link the child to its parent.
     *
     * @param targetKey the target message {@link MessageKey}, or {@code null}
     * @return the parent server id, or {@code 0} when the parent cannot be
     *         resolved
     */
    @WhatsAppWebExport(moduleName = "WAWebNewsletterSendMessageQueryJob", exports = "querySendNewsletterMessage",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private int resolveParentServerId(MessageKey targetKey) {
        if (targetKey == null) {
            return 0;
        }
        return store.chatStore().findMessageByKey(targetKey)
                .filter(msg -> msg instanceof NewsletterMessageInfo)
                .map(msg -> ((NewsletterMessageInfo) msg).serverId())
                .orElse(0);
    }

    /**
     * Builds the SMAX stanza for a poll-vote on an existing newsletter poll.
     *
     * <p>The {@code server_id} attribute carries the parent poll-creation
     * message's server id, and a {@code <votes>} child wraps one {@code <vote>}
     * per selected option. Returns {@code null} when the parent poll cannot be
     * resolved.
     *
     * @param info          the outgoing {@link NewsletterMessageInfo}
     * @param newsletterJid the newsletter {@link Jid}
     * @param pollUpdate    the {@link PollUpdateMessage} payload
     * @return the {@code <message>} {@link StanzaBuilder}, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishContentTypePollVoteMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishNewsletterPollVoteMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private StanzaBuilder buildPollVote(
            NewsletterMessageInfo info, Jid newsletterJid, PollUpdateMessage pollUpdate
    ) {
        var pollKey = pollUpdate.pollCreationMessageKey();
        if (pollKey.isEmpty()) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "poll vote missing pollCreationMessageKey for {0}", newsletterJid);
            }
            return null;
        }

        var parentMessage = store.chatStore().findMessageByKey(pollKey.get());
        if (parentMessage.isEmpty()
                || !(parentMessage.get() instanceof NewsletterMessageInfo parentNewsletter)
                || !(parentNewsletter.message().content() instanceof PollCreationMessage pollCreationMessage)) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "poll vote parent poll not resolved for {0}", newsletterJid);
            }
            return null;
        }

        var voteChildren = pollCreationMessage.options()
                .stream()
                .map(PollCreationMessage.Option::optionName)
                .flatMap(Optional::stream)
                .map(name -> new StanzaBuilder()
                        .description("vote")
                        .content(name)
                        .build())
                .toList();
        var votesNode = new StanzaBuilder()
                .description("votes")
                .content(voteChildren)
                .build();
        return new StanzaBuilder()
                .description("message")
                .attribute("id", info.key().id().orElseThrow())
                .attribute("to", newsletterJid)
                .attribute("type", "poll")
                .attribute("server_id", parentNewsletter.serverId())
                .content(new StanzaBuilder()
                        .description("meta")
                        .attribute("polltype", "vote")
                        .build(), votesNode);
    }

    /**
     * Returns the SMAX {@code mediatype} attribute value for the supplied
     * message.
     *
     * <p>Distinct from
     * {@link MessageSender#resolveStanzaType(LinkedMessageContainer)} (which returns
     * the outer-stanza classification): this returns the specific media-subtype
     * string written to the inner {@code <plaintext>} child. Defaults to
     * {@code "text"} for anything that is not a recognised media payload.
     *
     * @param message the newsletter {@link Message} payload
     * @return the SMAX media subtype, or {@code "text"}
     */
    @WhatsAppWebExport(moduleName = "WAWebNewsletterSendMessageQueryJob", exports = "querySendNewsletterMessage",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static String resolveSmaxMediaType(Message message) {
        return switch (message) {
            case ImageMessage _ -> "image";
            case VideoMessage v -> v.gifPlayback() ? "gif" : "video";
            case AudioMessage a -> a.ptt() ? "ptt" : "audio";
            case DocumentMessage _ -> "document";
            case StickerMessage _ -> "sticker";
            case StickerPackMessage _ -> "sticker_pack";
            case ContactMessage _ -> "vcard";
            case ExtendedTextMessage t when t.matchedText().isPresent() -> "url";
            default -> "text";
        };
    }
}
