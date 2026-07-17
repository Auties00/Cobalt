package com.github.auties00.cobalt.wire.linked.bot;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.bot.ai.AIRegenerateMetadata;
import com.github.auties00.cobalt.wire.linked.bot.ai.AIThreadInfo;
import com.github.auties00.cobalt.wire.linked.bot.feedback.*;
import com.github.auties00.cobalt.wire.linked.bot.metrics.BotInfrastructureDiagnostics;
import com.github.auties00.cobalt.wire.linked.bot.metrics.BotMetricsMetadata;
import com.github.auties00.cobalt.wire.linked.bot.plugin.*;
import com.github.auties00.cobalt.wire.linked.bot.rendering.*;
import com.github.auties00.cobalt.wire.linked.bot.session.*;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Top-level metadata envelope carried by every message exchanged with a WhatsApp
 * AI bot (such as Meta AI).
 *
 * <p>When a user sends a prompt to or receives a reply from an AI bot, the
 * end-to-end encrypted message wraps a {@code BotMetadata} instance that
 * aggregates every aspect of the bot interaction. The server populates the
 * response-side fields (model info, disclaimer text, rendering instructions,
 * sources, progress indicators) while the client populates the request-side
 * fields (capabilities, invoker identity, timezone, mode selection, thread
 * context) before encryption.
 *
 * <p>The metadata is organized into sub-messages covering these concerns:
 * <ul>
 *   <li>{@linkplain #sessionMetadata() Session}: model selection, session origin,
 *       and session identifier for conversation continuity
 *   <li>{@linkplain #pluginMetadata() Plugins}: which server-side plugin produced
 *       the response (web search, code generation, image generation, etc.)
 *   <li>{@linkplain #capabilityMetadata() Capabilities}: rich-response features
 *       the client advertises so the server can tailor its output
 *   <li>{@linkplain #renderingMetadata() Rendering}: keywords and layout hints
 *       that control how the response is displayed
 *   <li>{@linkplain #progressIndicatorMetadata() Progress}: step-by-step planning
 *       indicators shown while the bot is "thinking"
 *   <li>{@linkplain #memoryMetadata() Memory}: personalization facts added or
 *       removed by the AI during the conversation
 *   <li>{@linkplain #reminderMetadata() Reminders}: scheduled notifications the
 *       bot creates on behalf of the user
 *   <li>{@linkplain #verificationMetadata() Verification}: cryptographic proofs
 *       that authenticate the bot's response
 *   <li>{@linkplain #botThreadInfo() Thread info}: AI thread context that links
 *       related turns of a multi-message conversation
 *   <li>{@linkplain #imagineMetadata() Imagine}: metadata specific to AI
 *       image-generation ("Imagine") responses
 *   <li>{@linkplain #modelMetadata() Model}: which AI model produced the response
 *       and whether premium features are active
 *   <li>{@linkplain #botQuotaMetadata() Quota}: remaining AI usage allowance for
 *       the user
 * </ul>
 *
 * <p>All fields are optional; a given message typically populates only the
 * sub-messages relevant to that particular interaction turn.
 */
@ProtobufMessage(name = "BotMetadata")
public final class BotMetadata {
    /**
     * Metadata controlling the animated avatar rendered alongside this bot response,
     * including the avatar's sentiment expression and animation behaviour.
     * Present only on bot responses that display an animated persona.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    BotAvatarMetadata avatarMetadata;

    /**
     * Identifier of the bot persona that generated or should process this message,
     * for example {@code "meta_ai"}. The persona ID determines the bot's display
     * name, profile picture, and behavioural configuration.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String personaId;

    /**
     * Metadata about the server-side plugin that produced this bot response. A
     * plugin represents a specific capability such as web search, code generation,
     * or image recognition. This field carries the plugin type, search provider,
     * reference index, search query, and any thumbnail or favicon URLs associated
     * with plugin output.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    BotPluginMetadata pluginMetadata;

    /**
     * Follow-up prompt suggestions that the server attaches to a bot response.
     * The client displays these as tappable chips below the message so the user
     * can continue the conversation without typing.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    BotSuggestedPromptMetadata suggestedPromptMetadata;

    /**
     * JID of the user who invoked the bot. In one-to-one chats this is typically
     * the sender; in group chats it identifies which participant triggered the AI
     * interaction, since multiple group members may address the bot independently.
     * On outgoing messages, the client converts this to the user's LID before
     * encryption.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    Jid invokerJid;

    /**
     * Session-level metadata that ties this message to a bot conversation session.
     * Contains the session identifier and the session source (how the user entered
     * the conversation, e.g. deep link, chat list, or suggestion).
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    BotSessionMetadata sessionMetadata;

    /**
     * Metadata for the Meta AI "memu" (menu/onboarding) experience. Contains face
     * images and media assets used by the client to render the bot's visual
     * onboarding UI.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    BotMemuMetadata memuMetadata;

    /**
     * IANA timezone identifier of the user interacting with the bot, for example
     * {@code "America/New_York"} or {@code "Europe/Rome"}. The server uses this
     * to provide time-aware responses such as reminders and localized greetings.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String timezone;

    /**
     * Metadata for a reminder that the AI bot creates, modifies, or triggers on
     * behalf of the user. Includes the reminder name, the next trigger timestamp,
     * the action being performed (create, delete, or trigger), and the recurrence
     * frequency.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    BotReminderMetadata reminderMetadata;

    /**
     * Metadata identifying which AI model produced this response. Includes the
     * model type (e.g. default, premium), whether premium model access is enabled
     * for the user, and an optional display-name override for UI branding.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    BotModelMetadata modelMetadata;

    /**
     * Human-readable disclaimer text the client displays below the bot response,
     * for example {@code "AI-generated content may be inaccurate or misleading"}.
     * The server controls the wording; the client renders it verbatim.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    String messageDisclaimerText;

    /**
     * Step-by-step progress indicator metadata shown while the bot is generating
     * its response. Includes a top-level progress description, individual planning
     * steps with status titles and source attributions, and an estimated completion
     * time. The client uses this to display a "thinking" or "searching" animation
     * with incremental status updates.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.MESSAGE)
    BotProgressIndicatorMetadata progressIndicatorMetadata;

    /**
     * Client capability advertisement sent with outgoing bot messages. Lists the
     * rich-response feature types the client supports (structured responses,
     * headings, tables, LaTeX, code blocks, inline reels, unified responses, etc.)
     * so the server can tailor its output format. Only present on outgoing
     * messages.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.MESSAGE)
    BotCapabilityMetadata capabilityMetadata;

    /**
     * Metadata specific to AI image-generation ("Imagine") responses. Contains
     * the imagine type (e.g. create, edit) and a short version of the user's
     * prompt used to label the generated images.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.MESSAGE)
    BotImagineMetadata imagineMetadata;

    /**
     * Metadata describing changes the AI made to its personalization memory
     * during this interaction. Includes lists of added and removed memory facts
     * and a disclaimer shown to the user about memory usage.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
    BotMemoryMetadata memoryMetadata;

    /**
     * Rendering hints that control how the bot response is displayed. Contains
     * keywords extracted from the response with associated follow-up prompts
     * that the user can tap to explore related topics.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.MESSAGE)
    BotRenderingMetadata renderingMetadata;

    /**
     * Telemetry and analytics metadata for this bot interaction. Carries the
     * destination identifier, the entry point that led the user to the bot, and
     * the thread origin for funnel tracking.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    BotMetricsMetadata botMetricsMetadata;

    /**
     * Metadata about external third-party accounts (e.g. Spotify, Google) linked
     * to the bot for plugin access. Includes the linked account types and
     * authentication tokens or error codes for account-connected operations.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.MESSAGE)
    BotLinkedAccountsMetadata botLinkedAccountsMetadata;

    /**
     * Source attribution metadata for a rich response. Lists the external sources
     * (web pages, articles) that the bot cited in its answer, including provider
     * names, URLs, thumbnails, favicons, titles, and citation numbers.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.MESSAGE)
    BotSourcesMetadata richResponseSourcesMetadata;

    /**
     * Opaque binary context blob maintained by the server to preserve
     * conversation state across turns. The client must echo this value back
     * unchanged in subsequent messages within the same session. The contents
     * are not interpreted client-side.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.BYTES)
    byte[] aiConversationContext;

    /**
     * Metadata for a promotional message the server injects into the bot
     * conversation, such as an upsell for premium AI features. Includes the
     * promotion type and a call-to-action button title.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.MESSAGE)
    BotPromotionMessageMetadata botPromotionMessageMetadata;

    /**
     * Metadata describing the AI response mode(s) the user has selected for this
     * message. Modes control the bot's behaviour (e.g. creative, precise, balanced)
     * and are sent as repeated enum values along with optional override modes.
     * Only populated when the AI mode selector feature is enabled.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.MESSAGE)
    BotModeSelectionMetadata botModeSelectionMetadata;

    /**
     * Metadata about the user's remaining AI usage quota. Contains per-feature
     * quota entries (e.g. image generation, premium model) with remaining
     * allowances and expiration timestamps.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.MESSAGE)
    BotQuotaMetadata botQuotaMetadata;

    /**
     * Metadata for age-verification gating that the server requires before
     * allowing certain AI interactions. Indicates whether the user is eligible
     * for age collection, whether the client should trigger the collection flow,
     * and the collection type.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.MESSAGE)
    BotAgeCollectionMetadata botAgeCollectionMetadata;

    /**
     * Identifier of the conversation-starter prompt that initiated this bot
     * session, for example {@code "starter_travel_planning"}. Conversation
     * starters are pre-defined prompts displayed on the bot's home screen to
     * help users begin a new topic.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.STRING)
    String conversationStarterPromptId;

    /**
     * Server-assigned unique identifier for this particular bot response. Used
     * to correlate feedback, regeneration requests, and analytics events back
     * to a specific AI output.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.STRING)
    String botResponseId;

    /**
     * Cryptographic signature verification metadata that proves this bot message
     * was produced by a trusted Meta AI backend. Contains one or more use-case
     * proofs, each with a version, use-case identifier, signature bytes, and
     * certificate chain.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.MESSAGE)
    BotSignatureVerificationMetadata verificationMetadata;

    /**
     * Incremental mutation data for unified rich responses. When the server
     * streams a response in multiple updates, this field carries side-by-side
     * comparison metadata and media detail updates (high-resolution and preview
     * media) that the client merges into the existing response.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.MESSAGE)
    BotUnifiedResponseMutation unifiedResponseMutation;

    /**
     * Metadata describing the origin context of this bot message. Contains a
     * list of origin type indicators that classify how the message was triggered
     * (e.g. user-initiated, system-generated, forwarded).
     */
    @ProtobufProperty(index = 29, type = ProtobufType.MESSAGE)
    BotMessageOriginMetadata botMessageOriginMetadata;

    /**
     * Metadata for an in-thread user satisfaction survey injected by the server.
     * Contains the survey questions, answer options, privacy disclosures,
     * invitation text, and session identifiers used for telemetry correlation.
     * Present only when the server decides to collect feedback inline.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.MESSAGE)
    InThreadSurveyMetadata inThreadSurveyMetadata;

    /**
     * Thread context that links this message to an AI conversation thread.
     * Includes client-side info (thread type and the source chat JID that
     * originated the thread) and server-side info (thread title). Used to
     * maintain multi-turn conversation identity across messages.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.MESSAGE)
    AIThreadInfo botThreadInfo;

    /**
     * Metadata for a response-regeneration request. When the user asks the bot
     * to regenerate a previous answer, this field references the original
     * message key and the timestamp of the response being regenerated.
     */
    @ProtobufProperty(index = 32, type = ProtobufType.MESSAGE)
    AIRegenerateMetadata regenerateMetadata;

    /**
     * Session transparency disclosure metadata displayed as a system message
     * within the bot conversation. Contains the disclaimer text, a help-center
     * article identifier, and the transparency type (e.g. initial disclosure,
     * continuous reminder).
     */
    @ProtobufProperty(index = 33, type = ProtobufType.MESSAGE)
    SessionTransparencyMetadata sessionTransparencyMetadata;

    /**
     * Metadata describing document processing for this bot message. When the
     * user sends a document to the bot, this field indicates the plugin type
     * used to extract content (e.g. text extraction or OCR with image
     * conversion).
     */
    @ProtobufProperty(index = 34, type = ProtobufType.MESSAGE)
    BotDocumentMessageMetadata botDocumentMessageMetadata;

    /**
     * Group-specific metadata attached when the bot is invoked within a group
     * chat. Contains participant metadata identifying which bot participants
     * are present in the group, by their Facebook ID.
     */
    @ProtobufProperty(index = 35, type = ProtobufType.MESSAGE)
    BotGroupMetadata botGroupMetadata;

    /**
     * Rendering configuration metadata that specifies the Bloks versioning
     * identifier and the client's pixel density. Used by the server to deliver
     * resolution-appropriate rich content.
     */
    @ProtobufProperty(index = 36, type = ProtobufType.MESSAGE)
    BotRenderingConfigMetadata botRenderingConfigMetadata;

    /**
     * Infrastructure diagnostics data attached to a bot response for debugging.
     * Identifies which backend system generated the response, which tools were
     * invoked during generation, and whether the bot is currently in a
     * "thinking" state.
     */
    @ProtobufProperty(index = 37, type = ProtobufType.MESSAGE)
    BotInfrastructureDiagnostics botInfrastructureDiagnostics;

    /**
     * Opaque binary blob reserved for server-internal use. The client must
     * preserve and retransmit this data without interpretation. Carried at
     * protobuf index 999 to avoid collision with future field additions.
     */
    @ProtobufProperty(index = 999, type = ProtobufType.BYTES)
    byte[] internalMetadata;


    /**
     * Constructs a new {@code BotMetadata} with the specified sub-metadata values.
     * All parameters are optional and may be {@code null} when the corresponding
     * metadata is not relevant for the message being constructed.
     *
     * @param avatarMetadata                the animated avatar metadata, or {@code null}
     * @param personaId                     the bot persona identifier (e.g. {@code "meta_ai"}), or {@code null}
     * @param pluginMetadata                the server-side plugin metadata, or {@code null}
     * @param suggestedPromptMetadata       the follow-up prompt suggestions, or {@code null}
     * @param invokerJid                    the JID of the user who invoked the bot, or {@code null}
     * @param sessionMetadata               the session identifier and source, or {@code null}
     * @param memuMetadata                  the Meta AI menu/onboarding metadata, or {@code null}
     * @param timezone                      the user's IANA timezone (e.g. {@code "America/New_York"}), or {@code null}
     * @param reminderMetadata              the reminder action metadata, or {@code null}
     * @param modelMetadata                 the AI model identification metadata, or {@code null}
     * @param messageDisclaimerText         the disclaimer text displayed below the response, or {@code null}
     * @param progressIndicatorMetadata     the step-by-step progress indicator, or {@code null}
     * @param capabilityMetadata            the client capability advertisement, or {@code null}
     * @param imagineMetadata               the image-generation metadata, or {@code null}
     * @param memoryMetadata                the personalization memory changes, or {@code null}
     * @param renderingMetadata             the response rendering hints, or {@code null}
     * @param botMetricsMetadata            the telemetry and analytics metadata, or {@code null}
     * @param botLinkedAccountsMetadata     the third-party linked accounts, or {@code null}
     * @param richResponseSourcesMetadata   the source attribution for rich responses, or {@code null}
     * @param aiConversationContext          the opaque server-side conversation context, or {@code null}
     * @param botPromotionMessageMetadata   the promotional message metadata, or {@code null}
     * @param botModeSelectionMetadata      the AI mode selection (creative, precise, etc.), or {@code null}
     * @param botQuotaMetadata              the user's remaining AI usage quota, or {@code null}
     * @param botAgeCollectionMetadata      the age-verification gating metadata, or {@code null}
     * @param conversationStarterPromptId   the pre-defined starter prompt ID, or {@code null}
     * @param botResponseId                 the server-assigned response identifier, or {@code null}
     * @param verificationMetadata          the cryptographic signature proofs, or {@code null}
     * @param unifiedResponseMutation       the incremental unified response updates, or {@code null}
     * @param botMessageOriginMetadata      the message origin classification, or {@code null}
     * @param inThreadSurveyMetadata        the in-thread satisfaction survey, or {@code null}
     * @param botThreadInfo                 the AI conversation thread context, or {@code null}
     * @param regenerateMetadata            the response regeneration request, or {@code null}
     * @param sessionTransparencyMetadata   the session transparency disclosure, or {@code null}
     * @param botDocumentMessageMetadata    the document processing metadata, or {@code null}
     * @param botGroupMetadata              the group-specific bot metadata, or {@code null}
     * @param botRenderingConfigMetadata    the rendering configuration (Bloks version, pixel density), or {@code null}
     * @param botInfrastructureDiagnostics  the backend infrastructure diagnostics, or {@code null}
     * @param internalMetadata              the opaque server-internal bytes, or {@code null}
     */
    BotMetadata(BotAvatarMetadata avatarMetadata, String personaId, BotPluginMetadata pluginMetadata, BotSuggestedPromptMetadata suggestedPromptMetadata, Jid invokerJid, BotSessionMetadata sessionMetadata, BotMemuMetadata memuMetadata, String timezone, BotReminderMetadata reminderMetadata, BotModelMetadata modelMetadata, String messageDisclaimerText, BotProgressIndicatorMetadata progressIndicatorMetadata, BotCapabilityMetadata capabilityMetadata, BotImagineMetadata imagineMetadata, BotMemoryMetadata memoryMetadata, BotRenderingMetadata renderingMetadata, BotMetricsMetadata botMetricsMetadata, BotLinkedAccountsMetadata botLinkedAccountsMetadata, BotSourcesMetadata richResponseSourcesMetadata, byte[] aiConversationContext, BotPromotionMessageMetadata botPromotionMessageMetadata, BotModeSelectionMetadata botModeSelectionMetadata, BotQuotaMetadata botQuotaMetadata, BotAgeCollectionMetadata botAgeCollectionMetadata, String conversationStarterPromptId, String botResponseId, BotSignatureVerificationMetadata verificationMetadata, BotUnifiedResponseMutation unifiedResponseMutation, BotMessageOriginMetadata botMessageOriginMetadata, InThreadSurveyMetadata inThreadSurveyMetadata, AIThreadInfo botThreadInfo, AIRegenerateMetadata regenerateMetadata, SessionTransparencyMetadata sessionTransparencyMetadata, BotDocumentMessageMetadata botDocumentMessageMetadata, BotGroupMetadata botGroupMetadata, BotRenderingConfigMetadata botRenderingConfigMetadata, BotInfrastructureDiagnostics botInfrastructureDiagnostics, byte[] internalMetadata) {
        this.avatarMetadata = avatarMetadata;
        this.personaId = personaId;
        this.pluginMetadata = pluginMetadata;
        this.suggestedPromptMetadata = suggestedPromptMetadata;
        this.invokerJid = invokerJid;
        this.sessionMetadata = sessionMetadata;
        this.memuMetadata = memuMetadata;
        this.timezone = timezone;
        this.reminderMetadata = reminderMetadata;
        this.modelMetadata = modelMetadata;
        this.messageDisclaimerText = messageDisclaimerText;
        this.progressIndicatorMetadata = progressIndicatorMetadata;
        this.capabilityMetadata = capabilityMetadata;
        this.imagineMetadata = imagineMetadata;
        this.memoryMetadata = memoryMetadata;
        this.renderingMetadata = renderingMetadata;
        this.botMetricsMetadata = botMetricsMetadata;
        this.botLinkedAccountsMetadata = botLinkedAccountsMetadata;
        this.richResponseSourcesMetadata = richResponseSourcesMetadata;
        this.aiConversationContext = aiConversationContext;
        this.botPromotionMessageMetadata = botPromotionMessageMetadata;
        this.botModeSelectionMetadata = botModeSelectionMetadata;
        this.botQuotaMetadata = botQuotaMetadata;
        this.botAgeCollectionMetadata = botAgeCollectionMetadata;
        this.conversationStarterPromptId = conversationStarterPromptId;
        this.botResponseId = botResponseId;
        this.verificationMetadata = verificationMetadata;
        this.unifiedResponseMutation = unifiedResponseMutation;
        this.botMessageOriginMetadata = botMessageOriginMetadata;
        this.inThreadSurveyMetadata = inThreadSurveyMetadata;
        this.botThreadInfo = botThreadInfo;
        this.regenerateMetadata = regenerateMetadata;
        this.sessionTransparencyMetadata = sessionTransparencyMetadata;
        this.botDocumentMessageMetadata = botDocumentMessageMetadata;
        this.botGroupMetadata = botGroupMetadata;
        this.botRenderingConfigMetadata = botRenderingConfigMetadata;
        this.botInfrastructureDiagnostics = botInfrastructureDiagnostics;
        this.internalMetadata = internalMetadata;
    }

    /**
     * Returns the animated avatar metadata controlling the visual persona rendered
     * alongside this bot response, including sentiment expression and animation style.
     *
     * @return an {@code Optional} containing the avatar metadata, or empty if this
     *         message does not include an animated persona
     */
    public Optional<BotAvatarMetadata> avatarMetadata() {
        return Optional.ofNullable(avatarMetadata);
    }

    /**
     * Returns the identifier of the bot persona that generated or should process this
     * message (for example {@code "meta_ai"}), which determines the bot's display name,
     * profile picture, and behavioral configuration.
     *
     * @return an {@code Optional} containing the persona identifier string, or empty
     *         if not set
     */
    public Optional<String> personaId() {
        return Optional.ofNullable(personaId);
    }

    /**
     * Returns metadata about the server-side plugin that produced this bot response,
     * such as web search, code generation, or image recognition, including the plugin
     * type, search provider, reference index, query, and any associated thumbnail URLs.
     *
     * @return an {@code Optional} containing the plugin metadata, or empty if the
     *         response was not generated by a specific plugin
     */
    public Optional<BotPluginMetadata> pluginMetadata() {
        return Optional.ofNullable(pluginMetadata);
    }

    /**
     * Returns the follow-up prompt suggestions attached to this bot response. These
     * suggestions are displayed as tappable chips below the message so the user can
     * continue the conversation without typing.
     *
     * @return an {@code Optional} containing the suggested prompt metadata, or empty
     *         if no follow-up suggestions are included
     */
    public Optional<BotSuggestedPromptMetadata> suggestedPromptMetadata() {
        return Optional.ofNullable(suggestedPromptMetadata);
    }

    /**
     * Returns the JID of the user who invoked the bot. In one-to-one chats this is
     * typically the sender; in group chats it identifies which participant triggered
     * the AI interaction. On outgoing messages the client converts this to the
     * user's LID before encryption.
     *
     * @return an {@code Optional} containing the invoker's {@link Jid}, or empty
     *         if not set
     */
    public Optional<Jid> invokerJid() {
        return Optional.ofNullable(invokerJid);
    }

    /**
     * Returns session-level metadata tying this message to a bot conversation session,
     * including the session identifier and the session source (how the user entered
     * the conversation, such as a deep link, chat list, or suggestion).
     *
     * @return an {@code Optional} containing the session metadata, or empty if not set
     */
    public Optional<BotSessionMetadata> sessionMetadata() {
        return Optional.ofNullable(sessionMetadata);
    }

    /**
     * Returns the Meta AI "memu" (menu/onboarding) metadata, containing face images
     * and media assets used by the client to render the bot's visual onboarding UI.
     *
     * @return an {@code Optional} containing the memu metadata, or empty if this
     *         message does not include onboarding content
     */
    public Optional<BotMemuMetadata> memuMetadata() {
        return Optional.ofNullable(memuMetadata);
    }

    /**
     * Returns the IANA timezone identifier of the user interacting with the bot
     * (for example {@code "America/New_York"} or {@code "Europe/Rome"}), used by
     * the server for time-aware responses such as reminders and localized greetings.
     *
     * @return an {@code Optional} containing the IANA timezone string, or empty
     *         if not set
     */
    public Optional<String> timezone() {
        return Optional.ofNullable(timezone);
    }

    /**
     * Returns metadata for a reminder that the AI bot creates, modifies, or triggers
     * on behalf of the user, including the reminder name, next trigger timestamp,
     * action being performed, and recurrence frequency.
     *
     * @return an {@code Optional} containing the reminder metadata, or empty if this
     *         message does not involve a reminder action
     */
    public Optional<BotReminderMetadata> reminderMetadata() {
        return Optional.ofNullable(reminderMetadata);
    }

    /**
     * Returns metadata identifying which AI model produced this response, including the
     * model type (such as default or premium), whether premium model access is enabled
     * for the user, and an optional display-name override for UI branding.
     *
     * @return an {@code Optional} containing the model metadata, or empty if not set
     */
    public Optional<BotModelMetadata> modelMetadata() {
        return Optional.ofNullable(modelMetadata);
    }

    /**
     * Returns the human-readable disclaimer text displayed below the bot response
     * (for example, "AI-generated content may be inaccurate or misleading"). The
     * server controls the wording and the client renders it verbatim.
     *
     * @return an {@code Optional} containing the disclaimer text, or empty if no
     *         disclaimer is attached to this message
     */
    public Optional<String> messageDisclaimerText() {
        return Optional.ofNullable(messageDisclaimerText);
    }

    /**
     * Returns the step-by-step progress indicator metadata shown while the bot is
     * generating its response, including planning steps with status titles, source
     * attributions, and estimated completion time. The client uses this to display
     * a "thinking" or "searching" animation.
     *
     * @return an {@code Optional} containing the progress indicator metadata, or
     *         empty if no progress indicator is present
     */
    public Optional<BotProgressIndicatorMetadata> progressIndicatorMetadata() {
        return Optional.ofNullable(progressIndicatorMetadata);
    }

    /**
     * Returns the client capability advertisement sent with outgoing bot messages,
     * listing the rich-response feature types the client supports (structured responses,
     * headings, tables, LaTeX, code blocks, inline reels, etc.) so the server can
     * tailor its output format. Only present on outgoing messages.
     *
     * @return an {@code Optional} containing the capability metadata, or empty if
     *         this is an incoming message or capabilities were not advertised
     */
    public Optional<BotCapabilityMetadata> capabilityMetadata() {
        return Optional.ofNullable(capabilityMetadata);
    }

    /**
     * Returns metadata specific to AI image-generation ("Imagine") responses,
     * including the imagine type (such as create or edit) and a short version of
     * the user's prompt used to label the generated images.
     *
     * @return an {@code Optional} containing the imagine metadata, or empty if
     *         this message is not related to image generation
     */
    public Optional<BotImagineMetadata> imagineMetadata() {
        return Optional.ofNullable(imagineMetadata);
    }

    /**
     * Returns metadata describing changes the AI made to its personalization memory
     * during this interaction, including lists of added and removed memory facts
     * and a disclaimer shown to the user about memory usage.
     *
     * @return an {@code Optional} containing the memory metadata, or empty if no
     *         memory changes occurred during this interaction
     */
    public Optional<BotMemoryMetadata> memoryMetadata() {
        return Optional.ofNullable(memoryMetadata);
    }

    /**
     * Returns rendering hints that control how the bot response is displayed,
     * including keywords extracted from the response with associated follow-up
     * prompts the user can tap to explore related topics.
     *
     * @return an {@code Optional} containing the rendering metadata, or empty if
     *         no rendering hints are attached
     */
    public Optional<BotRenderingMetadata> renderingMetadata() {
        return Optional.ofNullable(renderingMetadata);
    }

    /**
     * Returns telemetry and analytics metadata for this bot interaction, carrying
     * the destination identifier, the entry point that led the user to the bot,
     * and the thread origin for funnel tracking.
     *
     * @return an {@code Optional} containing the metrics metadata, or empty if no
     *         telemetry data is attached
     */
    public Optional<BotMetricsMetadata> botMetricsMetadata() {
        return Optional.ofNullable(botMetricsMetadata);
    }

    /**
     * Returns metadata about external third-party accounts (such as Spotify or Google)
     * linked to the bot for plugin access, including the linked account types and
     * authentication tokens or error codes for account-connected operations.
     *
     * @return an {@code Optional} containing the linked accounts metadata, or empty
     *         if no linked accounts are involved
     */
    public Optional<BotLinkedAccountsMetadata> botLinkedAccountsMetadata() {
        return Optional.ofNullable(botLinkedAccountsMetadata);
    }

    /**
     * Returns source attribution metadata listing the external sources (web pages,
     * articles) the bot cited in its answer, including provider names, URLs,
     * thumbnails, favicons, titles, and citation numbers.
     *
     * @return an {@code Optional} containing the sources metadata, or empty if the
     *         response does not include source citations
     */
    public Optional<BotSourcesMetadata> richResponseSourcesMetadata() {
        return Optional.ofNullable(richResponseSourcesMetadata);
    }

    /**
     * Returns the opaque binary context blob maintained by the server to preserve
     * conversation state across turns. The client must echo this value back unchanged
     * in subsequent messages within the same session; the contents are not interpreted
     * client-side.
     *
     * @return an {@code Optional} containing the context bytes, or empty if not set
     */
    public Optional<byte[]> aiConversationContext() {
        return Optional.ofNullable(aiConversationContext);
    }

    /**
     * Returns metadata for a promotional message the server injects into the bot
     * conversation, such as an upsell for premium AI features, including the
     * promotion type and a call-to-action button title.
     *
     * @return an {@code Optional} containing the promotion metadata, or empty if
     *         no promotional content is attached
     */
    public Optional<BotPromotionMessageMetadata> botPromotionMessageMetadata() {
        return Optional.ofNullable(botPromotionMessageMetadata);
    }

    /**
     * Returns metadata describing the AI response mode(s) the user has selected for
     * this message. Modes control the bot's behavior (such as creative, precise, or
     * balanced) and are sent as repeated enum values along with optional override modes.
     *
     * @return an {@code Optional} containing the mode selection metadata, or empty if
     *         no mode selection is active
     */
    public Optional<BotModeSelectionMetadata> botModeSelectionMetadata() {
        return Optional.ofNullable(botModeSelectionMetadata);
    }

    /**
     * Returns metadata about the user's remaining AI usage quota, containing
     * per-feature quota entries (such as image generation or premium model access)
     * with remaining allowances and expiration timestamps.
     *
     * @return an {@code Optional} containing the quota metadata, or empty if no
     *         quota information is included
     */
    public Optional<BotQuotaMetadata> botQuotaMetadata() {
        return Optional.ofNullable(botQuotaMetadata);
    }

    /**
     * Returns metadata for age-verification gating that the server requires before
     * allowing certain AI interactions, indicating whether the user is eligible for
     * age collection, whether the client should trigger the collection flow, and
     * the collection type.
     *
     * @return an {@code Optional} containing the age collection metadata, or empty
     *         if no age verification is required
     */
    public Optional<BotAgeCollectionMetadata> botAgeCollectionMetadata() {
        return Optional.ofNullable(botAgeCollectionMetadata);
    }

    /**
     * Returns the identifier of the conversation-starter prompt that initiated this
     * bot session (for example {@code "starter_travel_planning"}). Conversation
     * starters are pre-defined prompts displayed on the bot's home screen to help
     * users begin a new topic.
     *
     * @return an {@code Optional} containing the starter prompt identifier, or empty
     *         if the session was not started from a pre-defined prompt
     */
    public Optional<String> conversationStarterPromptId() {
        return Optional.ofNullable(conversationStarterPromptId);
    }

    /**
     * Returns the server-assigned unique identifier for this particular bot response,
     * used to correlate feedback, regeneration requests, and analytics events back
     * to a specific AI output.
     *
     * @return an {@code Optional} containing the response identifier, or empty if
     *         not set
     */
    public Optional<String> botResponseId() {
        return Optional.ofNullable(botResponseId);
    }

    /**
     * Returns the cryptographic signature verification metadata that proves this bot
     * message was produced by a trusted Meta AI backend, containing one or more
     * use-case proofs with version, use-case identifier, signature bytes, and
     * certificate chain.
     *
     * @return an {@code Optional} containing the verification metadata, or empty if
     *         no cryptographic proof is attached
     */
    public Optional<BotSignatureVerificationMetadata> verificationMetadata() {
        return Optional.ofNullable(verificationMetadata);
    }

    /**
     * Returns incremental mutation data for unified rich responses. When the server
     * streams a response in multiple updates, this carries side-by-side comparison
     * metadata and media detail updates (high-resolution and preview media) that
     * the client merges into the existing response.
     *
     * @return an {@code Optional} containing the unified response mutation, or empty
     *         if no incremental update is present
     */
    public Optional<BotUnifiedResponseMutation> unifiedResponseMutation() {
        return Optional.ofNullable(unifiedResponseMutation);
    }

    /**
     * Returns metadata describing how this bot message was triggered, containing a
     * list of origin type indicators that classify the message as user-initiated,
     * system-generated, forwarded, or other origin types.
     *
     * @return an {@code Optional} containing the message origin metadata, or empty
     *         if no origin classification is present
     */
    public Optional<BotMessageOriginMetadata> botMessageOriginMetadata() {
        return Optional.ofNullable(botMessageOriginMetadata);
    }

    /**
     * Returns metadata for an in-thread user satisfaction survey injected by the
     * server, containing survey questions, answer options, privacy disclosures,
     * invitation text, and session identifiers for telemetry correlation.
     *
     * @return an {@code Optional} containing the survey metadata, or empty if no
     *         survey is attached to this message
     */
    public Optional<InThreadSurveyMetadata> inThreadSurveyMetadata() {
        return Optional.ofNullable(inThreadSurveyMetadata);
    }

    /**
     * Returns thread context that links this message to an AI conversation thread,
     * including client-side info (thread type and the source chat JID that originated
     * the thread) and server-side info (thread title). Used to maintain multi-turn
     * conversation identity across messages.
     *
     * @return an {@code Optional} containing the thread info, or empty if this
     *         message is not part of a threaded conversation
     */
    public Optional<AIThreadInfo> botThreadInfo() {
        return Optional.ofNullable(botThreadInfo);
    }

    /**
     * Returns metadata for a response-regeneration request. When the user asks the
     * bot to regenerate a previous answer, this references the original message key
     * and the timestamp of the response being regenerated.
     *
     * @return an {@code Optional} containing the regeneration metadata, or empty if
     *         this message is not a regeneration request
     */
    public Optional<AIRegenerateMetadata> regenerateMetadata() {
        return Optional.ofNullable(regenerateMetadata);
    }

    /**
     * Returns session transparency disclosure metadata displayed as a system message
     * within the bot conversation, containing the disclaimer text, a help-center
     * article identifier, and the transparency type (such as initial disclosure or
     * continuous reminder).
     *
     * @return an {@code Optional} containing the transparency metadata, or empty if
     *         no transparency notice is present
     */
    public Optional<SessionTransparencyMetadata> sessionTransparencyMetadata() {
        return Optional.ofNullable(sessionTransparencyMetadata);
    }

    /**
     * Returns metadata describing document processing for this bot message. When the
     * user sends a document to the bot, this indicates the plugin type used to extract
     * content (such as text extraction or OCR with image conversion).
     *
     * @return an {@code Optional} containing the document metadata, or empty if this
     *         message does not involve document processing
     */
    public Optional<BotDocumentMessageMetadata> botDocumentMessageMetadata() {
        return Optional.ofNullable(botDocumentMessageMetadata);
    }

    /**
     * Returns group-specific metadata attached when the bot is invoked within a group
     * chat, containing participant metadata identifying which bot participants are
     * present in the group by their Facebook ID.
     *
     * @return an {@code Optional} containing the group metadata, or empty if this
     *         message is not from a group chat context
     */
    public Optional<BotGroupMetadata> botGroupMetadata() {
        return Optional.ofNullable(botGroupMetadata);
    }

    /**
     * Returns rendering configuration metadata specifying the Bloks versioning
     * identifier and the client's pixel density, used by the server to deliver
     * resolution-appropriate rich content.
     *
     * @return an {@code Optional} containing the rendering configuration, or empty
     *         if no rendering configuration is attached
     */
    public Optional<BotRenderingConfigMetadata> botRenderingConfigMetadata() {
        return Optional.ofNullable(botRenderingConfigMetadata);
    }

    /**
     * Returns infrastructure diagnostics data attached to this bot response for
     * debugging, identifying which backend system generated the response, which
     * tools were invoked during generation, and whether the bot is currently in
     * a "thinking" state.
     *
     * @return an {@code Optional} containing the diagnostics data, or empty if no
     *         diagnostics are attached
     */
    public Optional<BotInfrastructureDiagnostics> botInfrastructureDiagnostics() {
        return Optional.ofNullable(botInfrastructureDiagnostics);
    }

    /**
     * Returns the opaque binary blob reserved for server-internal use. The client
     * must preserve and retransmit this data without interpretation. This field
     * is carried at a high protobuf index (999) to avoid collision with future
     * field additions.
     *
     * @return an {@code Optional} containing the internal metadata bytes, or empty
     *         if not set
     */
    public Optional<byte[]> internalMetadata() {
        return Optional.ofNullable(internalMetadata);
    }

    /**
     * Sets the animated avatar metadata controlling the visual persona rendered
     * alongside this bot response.
     *
     * @param avatarMetadata the avatar metadata to set, or {@code null} to clear
     */
    public void setAvatarMetadata(BotAvatarMetadata avatarMetadata) {
        this.avatarMetadata = avatarMetadata;
    }

    /**
     * Sets the identifier of the bot persona that generated or should process this
     * message (for example {@code "meta_ai"}).
     *
     * @param personaId the persona identifier to set, or {@code null} to clear
     */
    public void setPersonaId(String personaId) {
        this.personaId = personaId;
    }

    /**
     * Sets metadata about the server-side plugin that produced this bot response.
     *
     * @param pluginMetadata the plugin metadata to set, or {@code null} to clear
     */
    public void setPluginMetadata(BotPluginMetadata pluginMetadata) {
        this.pluginMetadata = pluginMetadata;
    }

    /**
     * Sets the follow-up prompt suggestions attached to this bot response.
     *
     * @param suggestedPromptMetadata the suggested prompt metadata to set, or {@code null} to clear
     */
    public void setSuggestedPromptMetadata(BotSuggestedPromptMetadata suggestedPromptMetadata) {
        this.suggestedPromptMetadata = suggestedPromptMetadata;
    }

    /**
     * Sets the JID of the user who invoked the bot. In group chats this identifies
     * which participant triggered the AI interaction.
     *
     * @param invokerJid the invoker JID to set, or {@code null} to clear
     */
    public void setInvokerJid(Jid invokerJid) {
        this.invokerJid = invokerJid;
    }

    /**
     * Sets session-level metadata tying this message to a bot conversation session.
     *
     * @param sessionMetadata the session metadata to set, or {@code null} to clear
     */
    public void setSessionMetadata(BotSessionMetadata sessionMetadata) {
        this.sessionMetadata = sessionMetadata;
    }

    /**
     * Sets the Meta AI "memu" (menu/onboarding) metadata containing face images
     * and media assets for the bot's visual onboarding UI.
     *
     * @param memuMetadata the memu metadata to set, or {@code null} to clear
     */
    public void setMemuMetadata(BotMemuMetadata memuMetadata) {
        this.memuMetadata = memuMetadata;
    }

    /**
     * Sets the IANA timezone identifier of the user interacting with the bot,
     * used by the server for time-aware responses.
     *
     * @param timezone the IANA timezone string to set (for example
     *                 {@code "America/New_York"}), or {@code null} to clear
     */
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    /**
     * Sets metadata for a reminder that the AI bot creates, modifies, or triggers
     * on behalf of the user.
     *
     * @param reminderMetadata the reminder metadata to set, or {@code null} to clear
     */
    public void setReminderMetadata(BotReminderMetadata reminderMetadata) {
        this.reminderMetadata = reminderMetadata;
    }

    /**
     * Sets metadata identifying which AI model produced this response, including
     * model type and premium model status.
     *
     * @param modelMetadata the model metadata to set, or {@code null} to clear
     */
    public void setModelMetadata(BotModelMetadata modelMetadata) {
        this.modelMetadata = modelMetadata;
    }

    /**
     * Sets the human-readable disclaimer text displayed below the bot response.
     * The client renders this text verbatim.
     *
     * @param messageDisclaimerText the disclaimer text to set, or {@code null} to clear
     */
    public void setMessageDisclaimerText(String messageDisclaimerText) {
        this.messageDisclaimerText = messageDisclaimerText;
    }

    /**
     * Sets the step-by-step progress indicator metadata shown while the bot is
     * generating its response.
     *
     * @param progressIndicatorMetadata the progress metadata to set, or {@code null} to clear
     */
    public void setProgressIndicatorMetadata(BotProgressIndicatorMetadata progressIndicatorMetadata) {
        this.progressIndicatorMetadata = progressIndicatorMetadata;
    }

    /**
     * Sets the client capability advertisement listing the rich-response feature
     * types the client supports. Only populated on outgoing messages.
     *
     * @param capabilityMetadata the capability metadata to set, or {@code null} to clear
     */
    public void setCapabilityMetadata(BotCapabilityMetadata capabilityMetadata) {
        this.capabilityMetadata = capabilityMetadata;
    }

    /**
     * Sets metadata specific to AI image-generation ("Imagine") responses.
     *
     * @param imagineMetadata the imagine metadata to set, or {@code null} to clear
     */
    public void setImagineMetadata(BotImagineMetadata imagineMetadata) {
        this.imagineMetadata = imagineMetadata;
    }

    /**
     * Sets metadata describing changes the AI made to its personalization memory
     * during this interaction.
     *
     * @param memoryMetadata the memory metadata to set, or {@code null} to clear
     */
    public void setMemoryMetadata(BotMemoryMetadata memoryMetadata) {
        this.memoryMetadata = memoryMetadata;
    }

    /**
     * Sets the rendering hints controlling how the bot response is displayed,
     * including keywords with associated follow-up prompts.
     *
     * @param renderingMetadata the rendering metadata to set, or {@code null} to clear
     */
    public void setRenderingMetadata(BotRenderingMetadata renderingMetadata) {
        this.renderingMetadata = renderingMetadata;
    }

    /**
     * Sets the telemetry and analytics metadata for this bot interaction.
     *
     * @param botMetricsMetadata the metrics metadata to set, or {@code null} to clear
     */
    public void setBotMetricsMetadata(BotMetricsMetadata botMetricsMetadata) {
        this.botMetricsMetadata = botMetricsMetadata;
    }

    /**
     * Sets metadata about external third-party accounts linked to the bot for
     * plugin access.
     *
     * @param botLinkedAccountsMetadata the linked accounts metadata to set, or {@code null} to clear
     */
    public void setBotLinkedAccountsMetadata(BotLinkedAccountsMetadata botLinkedAccountsMetadata) {
        this.botLinkedAccountsMetadata = botLinkedAccountsMetadata;
    }

    /**
     * Sets the source attribution metadata listing external sources the bot
     * cited in its answer.
     *
     * @param richResponseSourcesMetadata the sources metadata to set, or {@code null} to clear
     */
    public void setRichResponseSourcesMetadata(BotSourcesMetadata richResponseSourcesMetadata) {
        this.richResponseSourcesMetadata = richResponseSourcesMetadata;
    }

    /**
     * Sets the opaque binary context blob maintained by the server to preserve
     * conversation state across turns. The client must echo this value back unchanged
     * in subsequent messages.
     *
     * @param aiConversationContext the context bytes to set, or {@code null} to clear
     */
    public void setAiConversationContext(byte[] aiConversationContext) {
        this.aiConversationContext = aiConversationContext;
    }

    /**
     * Sets metadata for a promotional message injected into the bot conversation,
     * such as an upsell for premium AI features.
     *
     * @param botPromotionMessageMetadata the promotion metadata to set, or {@code null} to clear
     */
    public void setBotPromotionMessageMetadata(BotPromotionMessageMetadata botPromotionMessageMetadata) {
        this.botPromotionMessageMetadata = botPromotionMessageMetadata;
    }

    /**
     * Sets metadata describing the AI response mode(s) the user has selected,
     * controlling the bot's behavior (such as creative, precise, or balanced).
     *
     * @param botModeSelectionMetadata the mode selection metadata to set, or {@code null} to clear
     */
    public void setBotModeSelectionMetadata(BotModeSelectionMetadata botModeSelectionMetadata) {
        this.botModeSelectionMetadata = botModeSelectionMetadata;
    }

    /**
     * Sets metadata about the user's remaining AI usage quota with per-feature
     * allowances and expiration timestamps.
     *
     * @param botQuotaMetadata the quota metadata to set, or {@code null} to clear
     */
    public void setBotQuotaMetadata(BotQuotaMetadata botQuotaMetadata) {
        this.botQuotaMetadata = botQuotaMetadata;
    }

    /**
     * Sets metadata for age-verification gating that the server requires before
     * allowing certain AI interactions.
     *
     * @param botAgeCollectionMetadata the age collection metadata to set, or {@code null} to clear
     */
    public void setBotAgeCollectionMetadata(BotAgeCollectionMetadata botAgeCollectionMetadata) {
        this.botAgeCollectionMetadata = botAgeCollectionMetadata;
    }

    /**
     * Sets the identifier of the conversation-starter prompt that initiated this
     * bot session.
     *
     * @param conversationStarterPromptId the starter prompt identifier to set, or {@code null} to clear
     */
    public void setConversationStarterPromptId(String conversationStarterPromptId) {
        this.conversationStarterPromptId = conversationStarterPromptId;
    }

    /**
     * Sets the server-assigned unique identifier for this particular bot response,
     * used to correlate feedback and analytics.
     *
     * @param botResponseId the response identifier to set, or {@code null} to clear
     */
    public void setBotResponseId(String botResponseId) {
        this.botResponseId = botResponseId;
    }

    /**
     * Sets the cryptographic signature verification metadata proving this bot
     * message was produced by a trusted Meta AI backend.
     *
     * @param verificationMetadata the verification metadata to set, or {@code null} to clear
     */
    public void setVerificationMetadata(BotSignatureVerificationMetadata verificationMetadata) {
        this.verificationMetadata = verificationMetadata;
    }

    /**
     * Sets the incremental mutation data for unified rich responses, used when the
     * server streams a response in multiple updates.
     *
     * @param unifiedResponseMutation the mutation data to set, or {@code null} to clear
     */
    public void setUnifiedResponseMutation(BotUnifiedResponseMutation unifiedResponseMutation) {
        this.unifiedResponseMutation = unifiedResponseMutation;
    }

    /**
     * Sets metadata describing how this bot message was triggered, classifying
     * the origin as user-initiated, system-generated, forwarded, or other types.
     *
     * @param botMessageOriginMetadata the origin metadata to set, or {@code null} to clear
     */
    public void setBotMessageOriginMetadata(BotMessageOriginMetadata botMessageOriginMetadata) {
        this.botMessageOriginMetadata = botMessageOriginMetadata;
    }

    /**
     * Sets metadata for an in-thread user satisfaction survey injected by the server.
     *
     * @param inThreadSurveyMetadata the survey metadata to set, or {@code null} to clear
     */
    public void setInThreadSurveyMetadata(InThreadSurveyMetadata inThreadSurveyMetadata) {
        this.inThreadSurveyMetadata = inThreadSurveyMetadata;
    }

    /**
     * Sets the thread context linking this message to an AI conversation thread,
     * used for multi-turn conversation identity.
     *
     * @param botThreadInfo the thread info to set, or {@code null} to clear
     */
    public void setBotThreadInfo(AIThreadInfo botThreadInfo) {
        this.botThreadInfo = botThreadInfo;
    }

    /**
     * Sets metadata for a response-regeneration request referencing the original
     * message key and timestamp of the response being regenerated.
     *
     * @param regenerateMetadata the regeneration metadata to set, or {@code null} to clear
     */
    public void setRegenerateMetadata(AIRegenerateMetadata regenerateMetadata) {
        this.regenerateMetadata = regenerateMetadata;
    }

    /**
     * Sets session transparency disclosure metadata displayed as a system message
     * within the bot conversation.
     *
     * @param sessionTransparencyMetadata the transparency metadata to set, or {@code null} to clear
     */
    public void setSessionTransparencyMetadata(SessionTransparencyMetadata sessionTransparencyMetadata) {
        this.sessionTransparencyMetadata = sessionTransparencyMetadata;
    }

    /**
     * Sets metadata describing document processing for this bot message, indicating
     * the plugin type used to extract content from user-sent documents.
     *
     * @param botDocumentMessageMetadata the document metadata to set, or {@code null} to clear
     */
    public void setBotDocumentMessageMetadata(BotDocumentMessageMetadata botDocumentMessageMetadata) {
        this.botDocumentMessageMetadata = botDocumentMessageMetadata;
    }

    /**
     * Sets group-specific metadata attached when the bot is invoked within a group
     * chat, identifying bot participants by their Facebook ID.
     *
     * @param botGroupMetadata the group metadata to set, or {@code null} to clear
     */
    public void setBotGroupMetadata(BotGroupMetadata botGroupMetadata) {
        this.botGroupMetadata = botGroupMetadata;
    }

    /**
     * Sets the rendering configuration metadata specifying the Bloks versioning
     * identifier and client pixel density.
     *
     * @param botRenderingConfigMetadata the rendering configuration to set, or {@code null} to clear
     */
    public void setBotRenderingConfigMetadata(BotRenderingConfigMetadata botRenderingConfigMetadata) {
        this.botRenderingConfigMetadata = botRenderingConfigMetadata;
    }

    /**
     * Sets infrastructure diagnostics data for debugging, identifying the backend
     * system, tools invoked, and thinking state.
     *
     * @param botInfrastructureDiagnostics the diagnostics data to set, or {@code null} to clear
     */
    public void setBotInfrastructureDiagnostics(BotInfrastructureDiagnostics botInfrastructureDiagnostics) {
        this.botInfrastructureDiagnostics = botInfrastructureDiagnostics;
    }

    /**
     * Sets the opaque binary blob reserved for server-internal use. The client
     * must preserve and retransmit this data without interpretation.
     *
     * @param internalMetadata the internal metadata bytes to set, or {@code null} to clear
     */
    public void setInternalMetadata(byte[] internalMetadata) {
        this.internalMetadata = internalMetadata;
    }
}
