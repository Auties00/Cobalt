package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.bot.metrics.BotMetricsEntryPoint;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.*;
import com.github.auties00.cobalt.wire.core.message.*;
import com.github.auties00.cobalt.wire.linked.message.event.EncEventResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.event.EventMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollCreationMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollResultSnapshotMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollUpdateMessage;
import com.github.auties00.cobalt.wire.linked.message.security.SecretEncMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.message.system.history.MessageHistoryNotice;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Builds the optional {@code <meta>} child of an outgoing {@code <message>} stanza carrying routing and classification
 * attributes the server uses for analytics, comment threading, AI thread bookkeeping, hosted-business routing, status
 * privacy, and view-once handling.
 * <p>
 * Composed by {@link ChatFanoutStanza} and {@link GroupSkmsgFanoutStanza} once per outgoing message. The set of
 * attributes the stanza may carry is the union of every signal the sender writes onto {@code <meta>}: {@code origin}
 * (LID-origin code or bot-entry-point name when the recipient is Meta AI), {@code destination_id} (the bot metrics
 * destination id), {@code sender_intent="hosted"} (when the recipient is a hosted business), {@code polltype}
 * ({@code "creation"} / {@code "vote"} / {@code "result_snapshot"}), {@code event_type} ({@code "creation"} /
 * {@code "response"} / {@code "edit"}), {@code thread_msg_id} and {@code thread_msg_sender_jid} (comment-thread reply
 * target), {@code appdata} ({@code "member_tag"} / {@code "default"} / {@code "group_history"}),
 * {@code view_once="true"} for view-once media, {@code conversation_thread_id} for AI threads, {@code tag_reason}
 * ({@code "user_delete"} / {@code "user_update"}) for member-label changes, and {@code status_setting} for status
 * messages. When none of the signals applies the build method returns {@code null} and the caller suppresses the empty
 * child entirely. The newsletter helpers ({@link #buildNewsletterQuestion()}, {@link #buildNewsletterQuestionReply()},
 * {@link #buildNewsletterQuestionResponse()}) build a specialised {@code <meta questiontype="...">} child for SMAX
 * newsletter publishes.
 */
@WhatsAppWebModule(moduleName = "WAWebSendMsgMetaNode")
public final class MetaStanza {
    /**
     * The logger for {@link MetaStanza}.
     */
    private static final System.Logger LOGGER = Log.get(MetaStanza.class);

    /**
     * Holds the store consulted for chat LID origin and verified business name lookup.
     */
    private final LinkedWhatsAppStore store;

    /**
     * Constructs a builder backed by the given store.
     * <p>
     * The builder is stateless and reusable.
     *
     * @param store the {@link LinkedWhatsAppStore}
     * @throws NullPointerException if {@code store} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgMetaNode", exports = "genMetaNode",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public MetaStanza(LinkedWhatsAppStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    /**
     * Builds the {@code <meta>} child for an E2E-encrypted message.
     * <p>
     * Returns {@code null} when every gating signal is absent so the caller can suppress the {@code <meta>} child
     * entirely. The {@code statusSetting} input is non-null only for status messages (typically one of
     * {@code "contacts"}, {@code "allowlist"}, {@code "denylist"}); the {@code hashedAiThreadId} input is the HMAC of the
     * AI conversation thread id and is non-null only inside an AI thread.
     *
     * @implNote This implementation extracts the thread-message target by scanning {@link ChatMessageContextInfo#threadId()}
     * for the first non-AI entry. The view-once flag is read from {@link LinkedMessageContainer#futureProofContentType()}
     * rather than a dedicated media flag; the two are semantically equivalent in the wrappers that reach this builder.
     *
     * @param chatJid          the recipient chat {@link Jid}
     * @param container        the outgoing {@link LinkedMessageContainer}
     * @param statusSetting    the status-privacy label, or {@code null} for non-status messages
     * @param hashedAiThreadId the HMAC-hashed AI thread id, or {@code null} when not in an AI thread
     * @return the {@code <meta>} {@link Stanza}, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgMetaNode", exports = "genMetaNode",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza buildChat(Jid chatJid, LinkedMessageContainer container, String statusSetting, String hashedAiThreadId) {
        var message = container.content();

        var polltype = resolvePollType(message);
        var eventType = resolveEventType(message);
        var appdata = resolveAppdata(message);
        var tagReason = resolveTagReason(message);

        var viewOnce = container.futureProofContentType() == FutureProofMessageType.VIEW_ONCE ? "true" : null;

        var botMetrics = container.messageContextInfo()
                .flatMap(ChatMessageContextInfo::botMetadata)
                .map(bot -> bot.botMetricsMetadata().orElse(null))
                .orElse(null);

        var destinationId = botMetrics != null
                ? botMetrics.destinationId().orElse(null)
                : null;

        var botOrigin = botMetrics != null
                ? botMetrics.destinationEntryPoint()
                        .map(MetaStanza::resolveBotOrigin)
                        .orElse(null)
                : null;
        var origin = botOrigin != null && isMetaAiBot(chatJid)
                ? botOrigin
                : resolveOrigin(chatJid);

        var senderIntent = isHostedRecipient(chatJid) ? "hosted" : null;

        String threadMsgId = null;
        Jid threadMsgSenderJid = null;
        var deviceInfo = container.messageContextInfo().orElse(null);
        if (deviceInfo != null) {
            var threads = deviceInfo.threadId();
            for (var thread : threads) {
                if (thread.threadType().orElse(null) == MessageThreadId.ThreadType.AI_THREAD) {
                    continue;
                }
                var threadKey = thread.threadKey();
                var keyId = threadKey.flatMap(MessageKey::id)
                        .filter(entry -> !entry.isEmpty());
                if (keyId.isPresent()) {
                    threadMsgId = keyId.get();
                    threadMsgSenderJid = threadKey.flatMap(MessageKey::senderJid)
                            .orElse(null);
                    break;
                }
            }
        }

        if (polltype == null && eventType == null && viewOnce == null
                && origin == null && destinationId == null
                && senderIntent == null && appdata == null
                && threadMsgId == null && hashedAiThreadId == null
                && tagReason == null && statusSetting == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "meta build skipped for chat {0}: no signals present", chatJid);
            return null;
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "meta stanza built for chat {0} polltype={1} eventType={2} appdata={3}",
                    chatJid, polltype, eventType, appdata);
        }

        return new StanzaBuilder()
                .description("meta")
                .attribute("origin", origin)
                .attribute("destination_id", destinationId)
                .attribute("sender_intent", senderIntent)
                .attribute("polltype", polltype)
                .attribute("event_type", eventType)
                .attribute("thread_msg_id", threadMsgId)
                .attribute("thread_msg_sender_jid", threadMsgSenderJid)
                .attribute("appdata", appdata)
                .attribute("view_once", viewOnce)
                .attribute("conversation_thread_id", hashedAiThreadId)
                .attribute("tag_reason", tagReason)
                .attribute("status_setting", statusSetting)
                .build();
    }

    /**
     * Builds the {@code <meta>} child without a pre-computed AI thread id hash.
     * <p>
     * Delegates to {@link #buildChat(Jid, LinkedMessageContainer, String, String)} with {@code null} for the AI thread id, for
     * the non-AI-thread send path.
     *
     * @param chatJid       the recipient chat {@link Jid}
     * @param container     the outgoing {@link LinkedMessageContainer}
     * @param statusSetting the status-privacy label, or {@code null}
     * @return the {@code <meta>} {@link Stanza}, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgMetaNode", exports = "genMetaNode",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza buildChat(Jid chatJid, LinkedMessageContainer container, String statusSetting) {
        return buildChat(chatJid, container, statusSetting, null);
    }

    /**
     * Resolves the {@code polltype} attribute from the unwrapped message.
     * <p>
     * Returns one of {@code "creation"}, {@code "vote"}, {@code "result_snapshot"}, or {@code null} for non-poll
     * content. Poll-update messages are tagged only when they carry a {@link PollUpdateMessage#vote()}.
     *
     * @param message the unwrapped {@link Message}, possibly {@code null}
     * @return the poll-type label, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgMetaNode", exports = "genMetaNode",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static String resolvePollType(Message message) {
        return switch (message) {
            case PollCreationMessage _ -> "creation";
            case PollUpdateMessage p when p.vote().isPresent() -> "vote";
            case PollResultSnapshotMessage _ -> "result_snapshot";
            default -> null;
        };
    }

    /**
     * Resolves the {@code event_type} attribute from the unwrapped message.
     * <p>
     * Returns one of {@code "creation"}, {@code "response"}, {@code "edit"}, or {@code null} for non-event content. The
     * edit value applies only to {@link SecretEncMessage} payloads of subtype
     * {@link SecretEncMessage.SecretEncType#EVENT_EDIT}.
     *
     * @param message the unwrapped {@link Message}, possibly {@code null}
     * @return the event-type label, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgMetaNode", exports = "genMetaNode",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static String resolveEventType(Message message) {
        return switch (message) {
            case EventMessage _ -> "creation";
            case EncEventResponseMessage _ -> "response";
            case SecretEncMessage s
                    when s.secretEncType().orElse(null) == SecretEncMessage.SecretEncType.EVENT_EDIT -> "edit";
            default -> null;
        };
    }

    /**
     * Resolves the {@code appdata} attribute from the unwrapped message.
     * <p>
     * Returns {@code "member_tag"} for the group member-label change protocol message, {@code "default"} for the
     * ephemeral-sync response protocol message, {@code "group_history"} for the {@link MessageHistoryNotice}, and
     * {@code null} otherwise. The {@code "default"} value for peer-routed protocol messages is handled by the peer
     * message sender, which builds its own {@code <meta>}.
     *
     * @param message the unwrapped {@link Message}, possibly {@code null}
     * @return the appdata label, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgMetaNode", exports = "genMetaNode",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static String resolveAppdata(Message message) {
        if (message instanceof ProtocolMessage pm
                && pm.type().orElse(null) == ProtocolMessage.Type.GROUP_MEMBER_LABEL_CHANGE) {
            return "member_tag";
        }
        if (message instanceof ProtocolMessage pm
                && pm.type().orElse(null) == ProtocolMessage.Type.EPHEMERAL_SYNC_RESPONSE) {
            return "default";
        }
        if (message instanceof MessageHistoryNotice) {
            return "group_history";
        }
        return null;
    }

    /**
     * Resolves the {@code tag_reason} attribute for group-member label change protocol messages.
     * <p>
     * The label-change message carries a label payload whose presence distinguishes an addition or update from a
     * deletion: an empty or absent label maps to {@code "user_delete"}, a non-empty label maps to {@code "user_update"}.
     * Returns {@code null} for every other message type.
     *
     * @param message the unwrapped {@link Message}, possibly {@code null}
     * @return the tag-reason label, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgMetaNode", exports = "genMetaNode",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static String resolveTagReason(Message message) {
        if (!(message instanceof ProtocolMessage pm)
                || pm.type().orElse(null) != ProtocolMessage.Type.GROUP_MEMBER_LABEL_CHANGE) {
            return null;
        }
        var label = pm.memberLabel()
                .flatMap(ml -> ml.label())
                .orElse(null);
        return (label == null || label.isEmpty()) ? "user_delete" : "user_update";
    }

    /**
     * Resolves the {@code origin} attribute for LID chats whose chat record carries a LID-origin tag.
     * <p>
     * Only the {@code "ctwa"} LID-origin survives onto the {@code <meta>}; other LID origins (e.g. {@code "pn_share"})
     * are dropped.
     *
     * @param chatJid the recipient chat {@link Jid}
     * @return the origin label, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgMetaNode", exports = "getOriginAttribute",
            adaptation = WhatsAppAdaptation.DIRECT)
    private String resolveOrigin(Jid chatJid) {
        if (!chatJid.hasLidServer()) {
            return null;
        }

        return store.chatStore().findChatByJid(chatJid)
                .flatMap(Chat::lidOriginType)
                .filter("ctwa"::equals)
                .orElse(null);
    }

    /**
     * Returns whether the given {@link Jid} identifies the Meta AI bot account.
     * <p>
     * Matches both the FBID Meta AI bot ({@link Jid#metaAiBotAccount()}) and the
     * {@link Jid#metaAiBotPhoneAccount() legacy phone-number Meta AI bot}.
     *
     * @param jid the {@link Jid} to test
     * @return {@code true} when the JID is a Meta AI bot
     */
    @WhatsAppWebExport(moduleName = "WAWebBotUtils", exports = "isMetaAiBot",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean isMetaAiBot(Jid jid) {
        return jid.equals(Jid.metaAiBotAccount())
                || Jid.metaAiBotPhoneAccount().hasUser(jid.user());
    }

    /**
     * Maps a {@link BotMetricsEntryPoint} to its {@code origin} attribute label.
     * <p>
     * Used only when the message targets the Meta AI bot. Entry points with no canonical label (e.g. unmapped surface
     * codes) return {@code null} so the {@link #resolveOrigin(Jid)} LID fallback can apply.
     *
     * @implNote This implementation enumerates the subset of entry points that map to a non-null label and falls through
     * to {@code null} for unrecognised codes (e.g. {@code WEB_INTRO_PANEL}, {@code WEB_NAVIGATION_BAR}), matching the
     * surfaces Cobalt does not expose.
     *
     * @param entryPoint the {@link BotMetricsEntryPoint}
     * @return the origin label, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBotLoggingUtils", exports = "getBotOriginFromBotMetricsEntryPoint",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static String resolveBotOrigin(BotMetricsEntryPoint entryPoint) {
        return switch (entryPoint) {
            case FAVICON -> "favicon";
            case CHATLIST -> "chat_list";
            case AISEARCH_NULL_STATE_SUGGESTION -> "nullstate_suggestion";
            case AISEARCH_TYPE_AHEAD_SUGGESTION -> "typeahead_suggestion";
            case DEEPLINK -> "deeplink";
            case NOTIFICATION -> "notification";
            case AI_TAB -> "ai_tab";
            case ASK_META_AI_CONTEXT_MENU -> "ask_meta_ai_context_menu";
            case ASK_META_AI_CONTEXT_MENU_1ON1 -> "ask_meta_ai_context_menu_1on1";
            case ASK_META_AI_CONTEXT_MENU_GROUP -> "ask_meta_ai_context_menu_group";
            case META_AI_FORWARD -> "meta_ai_forward";
            default -> null;
        };
    }

    /**
     * Returns whether the recipient is a hosted business account.
     * <p>
     * Drives the {@code sender_intent="hosted"} attribute. A hosted business has a verified-business-name record whose
     * {@link com.github.auties00.cobalt.wire.linked.business.BusinessVerifiedName#hostStorage()} is present.
     *
     * @param chatJid the recipient chat {@link Jid}
     * @return {@code true} when the recipient has a hosted-storage record
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgMetaNode", exports = "genMetaNode",
            adaptation = WhatsAppAdaptation.DIRECT)
    private boolean isHostedRecipient(Jid chatJid) {
        return store.contactStore().findVerifiedBusinessName(chatJid)
                .map(vbn -> vbn.hostStorage().isPresent())
                .orElse(false);
    }

    /**
     * Builds the SMAX {@code <meta questiontype="question">} child for a newsletter question publish.
     * <p>
     * Used by the newsletter publish pipeline alongside {@link NewsletterStanza#buildPlaintext(byte[])}.
     *
     * @return the {@code <meta>} {@link Stanza}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishQuestionTypeQuestionMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static Stanza buildNewsletterQuestion() {
        return new StanzaBuilder()
                .description("meta")
                .attribute("questiontype", "question")
                .build();
    }

    /**
     * Builds the SMAX {@code <meta questiontype="reply">} child for a newsletter question reply publish.
     * <p>
     * Used by the newsletter publish pipeline for the reply-to-question subflow.
     *
     * @return the {@code <meta>} {@link Stanza}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishQuestionTypeReplyMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static Stanza buildNewsletterQuestionReply() {
        return new StanzaBuilder()
                .description("meta")
                .attribute("questiontype", "reply")
                .build();
    }

    /**
     * Builds the SMAX {@code <meta questiontype="response">} child for a newsletter question response publish.
     * <p>
     * Used by the newsletter publish pipeline for the response-to-question subflow.
     *
     * @return the {@code <meta>} {@link Stanza}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutMessagePublishQuestionTypeResponseMixin", exports = "applyMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static Stanza buildNewsletterQuestionResponse() {
        return new StanzaBuilder()
                .description("meta")
                .attribute("questiontype", "response")
                .build();
    }
}
