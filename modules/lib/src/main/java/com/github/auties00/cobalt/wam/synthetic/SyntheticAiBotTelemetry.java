package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.BotBizJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.BotJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ImagineActionsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MetaAiUpsellCtaEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.SupportAiSessionEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.AdditionalCategoryType;
import com.github.auties00.cobalt.wire.wam.type.BotBizActionType;
import com.github.auties00.cobalt.wire.wam.type.BotBizEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.BotBizType;
import com.github.auties00.cobalt.wire.wam.type.BotEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.BotType;
import com.github.auties00.cobalt.wire.wam.type.ChatFilterActionTypes;
import com.github.auties00.cobalt.wire.wam.type.ImagineAction;
import com.github.auties00.cobalt.wire.wam.type.ImagineActionSource;
import com.github.auties00.cobalt.wire.wam.type.ImagineActionTarget;
import com.github.auties00.cobalt.wire.wam.type.ImagineActionThreadType;
import com.github.auties00.cobalt.wire.wam.type.ImagineMediaType;
import com.github.auties00.cobalt.wire.wam.type.ImplementationType;
import com.github.auties00.cobalt.wire.wam.type.MetaAiUpsellCtaOperationType;
import com.github.auties00.cobalt.wire.wam.type.MetaAiUpsellCtaSourceType;
import com.github.auties00.cobalt.wire.wam.type.SupportAiEventType;
import com.github.auties00.cobalt.wire.wam.type.TextModalityType;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Emits the block of Meta AI and bot user-journey WhatsApp Metrics events that a genuine WhatsApp
 * client logs as the user discovers, opens and interacts with the Meta AI assistant, business AI
 * bots, the in-app AI support chat, the Meta AI {@code Imagine} image generator, and the Meta AI
 * upsell promos, but for which Cobalt exposes no corresponding feature to trigger a real emission.
 *
 * <p>WhatsApp instruments a family of AI-surface interaction journeys with no server round-trip:
 * the discovery, creation and chat journey around the Meta AI assistant and user-generated agents
 * ({@code BotJourney}), the business-bot contact-card, deeplink and NUX journey
 * ({@code BotBizJourney}), the AI support-chat session and answer-citation taps
 * ({@code SupportAiSession}), the {@code Imagine} image-generation actions ({@code ImagineActions}),
 * and the Meta AI upsell call-to-action impressions and clicks ({@code MetaAiUpsellCta}). None of
 * these map to a Cobalt feature: Cobalt is a headless JVM client that can exchange bot messages over
 * the wire but has no AI discovery, creation, chat, support, image-generation or promo user
 * interface to raise them from. A Cobalt session whose telemetry stream never carried any of these
 * AI-journey events would be trivially separable from a real client by the server-side WAM
 * consumers, so this service synthesises the block once per connect, populating each event with
 * plausible, real-looking interaction data.
 *
 * <p>Every field WhatsApp sets on each event is populated with a coherent, real-looking value:
 * a stable per-connect app session id (mirroring WhatsApp's single shared session id), fresh
 * per-interaction AI-session, unified-session and conversation-thread ids, the host clock for the
 * event timestamp, the host locale for the device language, and realistic enum, boolean, count and
 * duration fabrications drawn from the same value space WhatsApp's own loggers use. No
 * obviously-fake sentinel values are used; the error and citation fields that only a failing or
 * citation-tapped support turn carries are left unset rather than zero-filled, matching what a real
 * successful turn reports.
 *
 * <p>The single public entry point {@link #emitSessionTelemetry()} fires the whole block once. On a
 * real client these journeys accrue sporadically over an app run as the user touches each AI
 * surface; Cobalt folds one representative snapshot of each into the per-connect burst rather than
 * driving a separate timer, because the per-connect cadence already keeps the stream populated and
 * none of these events carries state that would be double-counted.
 *
 * @implNote
 * This implementation mints the shared app session id once at construction, as WhatsApp mints its
 * single shared session id once per app load, and threads it through every event; the AI-session,
 * unified-session and thread ids are minted fresh per emission because WhatsApp scopes them to a
 * single AI interaction rather than to the app run. It does not gate on client type: the Meta AI
 * and bot surfaces exist on both the browser and the primary-device apps, so the block is emitted
 * for every linked session regardless of flavour.
 *
 * @see WamService
 */
@WhatsAppWebModule(moduleName = "WAWebBotJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebBotJourneyLogger")
@WhatsAppWebModule(moduleName = "WAWebBotBizJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebBizBotLogging")
@WhatsAppWebModule(moduleName = "WAWebSupportAiSessionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebSupportChatUtils")
@WhatsAppWebModule(moduleName = "WAWebImagineActionsWamEvent")
@WhatsAppWebModule(moduleName = "WAWebImagineActionLogger")
@WhatsAppWebModule(moduleName = "WAWebMetaAiUpsellCtaWamEvent")
@WhatsAppWebModule(moduleName = "WAWebLogMetaAICtaUpsellOperation")
public final class SyntheticAiBotTelemetry {
    /**
     * The persona identifier reported for the Meta AI assistant in the {@code botPersonaId} field of
     * the bot-journey event.
     *
     * <p>WhatsApp threads the assistant's stable numeric agent id through the discovery and chat
     * journeys; this Facebook-style sixteen-digit constant reproduces that shape so the field reads
     * like a genuine assistant reference rather than a placeholder.
     */
    private static final String META_AI_PERSONA_ID = "1129586908751473";

    /**
     * The raw entry-point label reported in the {@code rawBotEntryPoint} field of the bot-journey
     * event.
     *
     * <p>WhatsApp derives this lowercase origin string from the typed entry point through its bot
     * logging utilities; the value here is the origin of the AI chats-list button entry point used by
     * the fabricated chat-open journey.
     */
    private static final String RAW_BOT_ENTRY_POINT = "ai_chats_list_button";

    /**
     * The number of random bytes hashed into a fabricated hexadecimal conversation-thread identifier.
     *
     * <p>WhatsApp reports a Meta AI conversation thread id as an opaque digest; sixteen bytes
     * reproduce the thirty-two-hex-character shape of that identifier.
     */
    private static final int THREAD_ID_BYTES = 16;

    /**
     * The bound WhatsApp client, retained so the service matches the construction shape of the other
     * synthetic telemetry services and can sample live store state should later fields require it.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every synthesised AI-journey event is committed for batched
     * upload.
     */
    private final WamService wamService;

    /**
     * The app-scoped session identifier shared across the AI-journey events emitted in a single
     * connect.
     *
     * <p>WhatsApp threads one shared session id, minted as a random UUID at app load, through every
     * journey logged during an app run; this is minted once per service instance to reproduce that
     * single stable value.
     */
    private final String appSessionId;

    /**
     * Constructs a new {@code SyntheticAiBotTelemetry} bound to the given client and WAM service.
     *
     * @param client     the WhatsApp client this service is bound to, must not be {@code null}
     * @param wamService the WAM service used to commit the synthesised events, must not be
     *                   {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticAiBotTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.appSessionId = UUID.randomUUID().toString();
    }

    /**
     * Emits the full block of synthetic Meta AI and bot journey events once.
     *
     * <p>This is the sole public entry point and is intended to run once per successful socket
     * bring-up. It commits, in turn, one representative Meta AI assistant chat-open journey, one
     * business-bot contact-card journey, one AI support-chat citation tap, one {@code Imagine}
     * image-generation send, and one Meta AI upsell impression, each through
     * {@link WamService#commit}.
     *
     * @apiNote
     * On a real client these journeys are logged sporadically as the user touches each AI surface,
     * not in a single burst; a caller that wants a steadier cadence may invoke this method again on a
     * timer, since each emission mints fresh per-interaction ids and reuses only the stable app
     * session id.
     */
    public void emitSessionTelemetry() {
        emitBotJourney();
        emitBotBizJourney();
        emitSupportAiSession();
        emitImagineActions();
        emitMetaAiUpsellCta();
    }

    /**
     * Commits a synthetic {@code BotJourney} event (id 4630) describing a Meta AI assistant
     * chat-open journey.
     *
     * <p>The fabricated journey reproduces the shape WhatsApp's bot-journey logger commits when the
     * user opens the Meta AI assistant from the chats-list button: the shared app session id, the
     * chat-click action, the chats-list-button entry point and its raw origin label, the assistant
     * persona id, the Meta AI chat surface, a fresh AI-session id, the host event timestamp, the base
     * model category, and the assistant, character-bot and user-created-agent flags that classify the
     * target as the first-party Meta AI assistant.
     */
    @WhatsAppWebExport(moduleName = "WAWebBotJourneyWamEvent", exports = "BotJourneyWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebBotJourneyLogger", exports = "logAiChatClick", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitBotJourney() {
        wamService.commit(new BotJourneyEventBuilder()
                .appSessionId(appSessionId)
                .actionType(ChatFilterActionTypes.AI_CHAT_CLICK)
                .botEntryPoint(BotEntryPointType.AI_CHATS_LIST_BUTTON)
                .rawBotEntryPoint(RAW_BOT_ENTRY_POINT)
                .botPersonaId(META_AI_PERSONA_ID)
                .uiSurface(TsSurface.META_AI_CHAT)
                .aiSessionId(SyntheticTelemetryUtils.newSessionId())
                .eventTsMs(System.currentTimeMillis())
                .additionalCategory(AdditionalCategoryType.META_AI_MODEL_BASE)
                .deviceLanguage(deviceLanguage())
                .isMetaAiAssistant(true)
                .isMetaAiCharacterBotChat(false)
                .isUserCreatedAgent(false)
                .isCache(false)
                .build());
    }

    /**
     * Commits a synthetic {@code BotBizJourney} event (id 4868) describing a third-party business-bot
     * contact-card journey.
     *
     * <p>The fabricated journey reproduces the shape WhatsApp's business-bot logger commits for a
     * shared third-party bot contact card: the shared app session id, the shared-card entry point, the
     * card-click action, and the third-party business bot type classification on both the biz-type and
     * bot-type fields.
     */
    @WhatsAppWebExport(moduleName = "WAWebBotBizJourneyWamEvent", exports = "BotBizJourneyWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebBizBotLogging", exports = "logBizBot3pContactCardJourneyEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitBotBizJourney() {
        wamService.commit(new BotBizJourneyEventBuilder()
                .appSessionId(appSessionId)
                .botBizEntryPoint(BotBizEntryPoint.SHARED_BOT_BIZ_CARD)
                .botBizActionType(BotBizActionType.BOT_BIZ_CARD_CLICK)
                .botBizType(BotBizType.BOT_BIZ_3P)
                .botType(BotType.BOT_3P_BIZ)
                .build());
    }

    /**
     * Commits a synthetic {@code SupportAiSession} event (id 4970) describing an answer-citation tap
     * inside the in-app AI support chat.
     *
     * <p>The fabricated turn reports the citation-tapped event type paired with the content-management
     * id of the cited support article, the two fields a real citation tap carries. The error-code and
     * error-message fields are left unset because they belong to a failing support turn, not to a
     * successful citation tap, so populating them would be a fabricated failure rather than a faithful
     * signal.
     */
    @WhatsAppWebExport(moduleName = "WAWebSupportAiSessionWamEvent", exports = "SupportAiSessionWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSupportChatUtils", exports = "openSupportAINux", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitSupportAiSession() {
        wamService.commit(new SupportAiSessionEventBuilder()
                .supportAiEventType(SupportAiEventType.SUPPORT_AI_CITATION_TAPPED)
                .citationCmsId(Long.toString(1_000_000_000_000L + ThreadLocalRandom.current().nextLong(1_000_000_000L)))
                .build());
    }

    /**
     * Commits a synthetic {@code ImagineActions} event (id 5620) describing a completed Meta AI
     * {@code Imagine} image-generation send.
     *
     * <p>The fabricated action reproduces the shape WhatsApp's imagine logger commits when the user
     * sends a generated image: a fresh AI-session id, the send action from the attachment-tray source
     * onto the imagine target, an image media type, the not-cancelled and sent flags, the four-image
     * carousel bounds with the selected index, a plausible generation duration, the Meta AI thread and
     * unified session ids, the GenAI implementation, and the text prompt modality.
     */
    @WhatsAppWebExport(moduleName = "WAWebImagineActionsWamEvent", exports = "ImagineActionsWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebImagineActionLogger", exports = "logImagineAction", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitImagineActions() {
        wamService.commit(new ImagineActionsEventBuilder()
                .aiSessionId(SyntheticTelemetryUtils.newSessionId())
                .imagineAction(ImagineAction.IMAGINE_SEND)
                .imagineActionSource(ImagineActionSource.ATTACHMEMT_TRAY)
                .imagineActionTarget(ImagineActionTarget.IMAGINE)
                .imagineActionThreadType(ImagineActionThreadType.META_AI)
                .imagineMediaType(ImagineMediaType.IMAGE)
                .isCancelled(false)
                .isSent(true)
                .maxIndex(3)
                .selectedImageIndex(1)
                .imagineActionDuration(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(3_200, 2_400)))
                .metaAiConversationThreadId(SyntheticTelemetryUtils.randomHexLower(THREAD_ID_BYTES))
                .threadSessionId(SyntheticTelemetryUtils.newSessionId())
                .unifiedSessionId(SyntheticTelemetryUtils.newSessionId())
                .implementationType(ImplementationType.GENAI)
                .textModality(TextModalityType.TEXT)
                .build());
    }

    /**
     * Commits a synthetic {@code MetaAiUpsellCta} event (id 6532) describing a Meta AI upsell
     * call-to-action impression.
     *
     * <p>The fabricated event reproduces the shape WhatsApp's upsell logger commits when the
     * persistent chat-banner promo is shown: the impression operation on the persistent-chat-banner
     * source, the only source the WhatsApp logger emits.
     */
    @WhatsAppWebExport(moduleName = "WAWebMetaAiUpsellCtaWamEvent", exports = "MetaAiUpsellCtaWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebLogMetaAICtaUpsellOperation", exports = "logMetaAICtaUpsellOperation", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitMetaAiUpsellCta() {
        wamService.commit(new MetaAiUpsellCtaEventBuilder()
                .metaAiUpsellCtaOperation(MetaAiUpsellCtaOperationType.IMPRESSION)
                .metaAiUpsellCtaSource(MetaAiUpsellCtaSourceType.PERSISTENT_CHAT_BANNER)
                .build());
    }



    /**
     * Returns the host locale as a BCP 47 language tag, reported in the bot-journey event's
     * {@code deviceLanguage} field.
     *
     * <p>WhatsApp reports the app's active locale here; deriving the tag from the host default locale
     * keeps the value plausible and host-specific rather than a frozen constant.
     *
     * @return the host default locale as a language tag, for example {@code en-US}
     */
    private static String deviceLanguage() {
        return Locale.getDefault().toLanguageTag();
    }


}
