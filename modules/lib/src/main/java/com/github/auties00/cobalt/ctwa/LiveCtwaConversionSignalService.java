package com.github.auties00.cobalt.ctwa;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaDataSharingPreference;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaDataSharingSetting;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaOrderStatus;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.preference.Label;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.Ctwa3pdConversionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.CtwaLabelSignalEventBuilder;
import com.github.auties00.cobalt.wam.threadlogging.LiveThreadLoggingService;
import com.github.auties00.cobalt.wire.wam.type.CtwaLabelTarget;
import com.github.auties00.cobalt.wire.wam.type.CtwaLabelType;
import com.github.auties00.cobalt.wire.wam.type.CustomerAdsSharingSettingEnabled;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production {@link CtwaConversionSignalService} backed by a {@link LinkedWhatsAppClient}.
 *
 * <p>This resolves a chat's Click-To-WhatsApp eligibility by scanning its messages for an inbound
 * message that carries an {@code FB_Ads} conversion, computes the per-chat conversation depth and thread
 * id, and commits the {@code Ctwa3pdConversion} and {@code CtwaLabelSignal} WAM events through the bound
 * {@link WamService} subject to the business account's data-sharing gates.
 *
 * @implNote
 * This implementation resolves WhatsApp Web's business-gating feature flags through the bound
 * {@link ABPropsService}: {@code isSMBLabelsDataSharingEnabledForChats} from
 * {@link ABProp#SMB_LABELS_CTWA_DATA_SHARING}, {@code smbDataSharingConsentEnabled} from
 * {@link ABProp#CTWA_SMB_DATA_SHARING_CONSENT}, {@code isPerCustomerDataSharingControlsEnabled} from
 * {@link ABProp#PER_CUSTOMER_DATA_SHARING_CONTROLS_ELIGIBLE}, and
 * {@code is3pdImportantLabelSignalsEnabled} from {@link ABProp#CTWA_IMPORTANT_LABEL_SENDS_SIGNALS}. The
 * {@code ctwaConversationRepeat} and {@code ctwaDirectionFrom} third-party conversion fields are left
 * unset because WhatsApp Web's own emitter does not populate them either.
 */
@WhatsAppWebModule(moduleName = "WAWebSmb3pdConversionSignalAction")
@WhatsAppWebModule(moduleName = "WAWebSmbMarkAsXLabelAction")
@WhatsAppWebModule(moduleName = "WAWebCommonCTWADataSharing")
@WhatsAppWebModule(moduleName = "WAWebCtwaConversationDepthUtils")
@WhatsAppWebModule(moduleName = "WAWebGetCTWAEligibilityFromConversion")
public final class LiveCtwaConversionSignalService implements CtwaConversionSignalService {
    /**
     * The logger for {@link LiveCtwaConversionSignalService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveCtwaConversionSignalService.class);

    /**
     * The only conversion source that makes a chat CTWA-eligible.
     */
    private static final String SOURCE_FB_ADS = "FB_Ads";

    /**
     * The surface reported for a conversion driven by a label association.
     */
    private static final String SURFACE_LABEL_CHAT = "label_chat";

    /**
     * The surface reported for a conversion driven by an order change.
     */
    private static final String SURFACE_ORDER = "order";

    /**
     * The paid-data metadata for a label or order that is not in a paid state.
     */
    private static final String PAID_DATA_FALSE = "{\"paid\":false}";

    /**
     * The paid-data metadata for a label or order that is in a paid state.
     */
    private static final String PAID_DATA_TRUE = "{\"paid\":true}";

    /**
     * The empty paid-data metadata reported for the follow-up and lead label types.
     */
    private static final String PAID_DATA_EMPTY = "{}";

    /**
     * The schema version stamped on every {@code Ctwa3pdConversion} event.
     */
    private static final int SCHEMA_VERSION = 2;

    /**
     * The signal version stamped on every {@code CtwaLabelSignal} event.
     */
    private static final int LABEL_SIGNAL_VERSION = 1;

    /**
     * The bound client, used to read the message store, the chat store, the business data-sharing
     * state, and the thread-logging secret.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The telemetry sink for the {@code Ctwa3pdConversion} and {@code CtwaLabelSignal} events.
     */
    private final WamService wamService;

    /**
     * The AB-prop source consulted for the CTWA business-gating flags.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs a service bound to the given collaborators.
     *
     * @param client         the bound client, must not be {@code null}
     * @param wamService     the telemetry sink, must not be {@code null}
     * @param abPropsService the AB-prop source for the business-gating flags, must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public LiveCtwaConversionSignalService(LinkedWhatsAppClient client, WamService wamService, ABPropsService abPropsService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSmbMarkAsXLabelAction", exports = "logLabelSignalForModels", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSmb3pdConversionSignalAction", exports = "log3pdConversionSignalForChats", adaptation = WhatsAppAdaptation.ADAPTED)
    public void emitLabelConversion(JidProvider chat, Label label) {
        Objects.requireNonNull(chat, "chat cannot be null");
        Objects.requireNonNull(label, "label cannot be null");
        if (!abPropsService.getBool(ABProp.SMB_LABELS_CTWA_DATA_SHARING)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ctwa label conversion skipped, data sharing disabled for chat {0}", chat.toJid());
            return;
        }

        var predefinedId = label.predefinedId();
        if (predefinedId.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ctwa label conversion skipped, label has no predefined id");
            return;
        }

        var conversion = labelConversionFor(predefinedId.getAsInt());
        if (conversion == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ctwa label conversion skipped, predefined id {0} not a ctwa identifier", predefinedId.getAsInt());
            return;
        }
        if (conversion.labelType() == CtwaLabelType.IMPORTANT
                && !abPropsService.getBool(ABProp.CTWA_IMPORTANT_LABEL_SENDS_SIGNALS)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ctwa label conversion skipped, important label signals disabled");
            return;
        }

        var resolved = client.store().chatStore().findChatByJid(chat.toJid());
        if (resolved.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ctwa label conversion skipped, chat {0} not found", chat.toJid());
            return;
        }

        var resolvedChat = resolved.get();
        var paidData = labelPaidData(conversion.labelType());
        emit3pdConversion(resolvedChat, SURFACE_LABEL_CHAT, conversion.conversionType(), conversion.subtype(), paidData);
        emitLabelSignals(resolvedChat, List.of(conversion.labelType()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSmb3pdConversionSignalAction", exports = "log3pdConversionSignalForOrders", adaptation = WhatsAppAdaptation.ADAPTED)
    public void emitOrderConversion(JidProvider chat, CtwaOrderStatus status, boolean paid) {
        Objects.requireNonNull(chat, "chat cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        if (!abPropsService.getBool(ABProp.CTWA_SMB_DATA_SHARING_CONSENT)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ctwa order conversion skipped, data sharing consent disabled for chat {0}", chat.toJid());
            return;
        }

        var resolved = client.store().chatStore().findChatByJid(chat.toJid());
        if (resolved.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ctwa order conversion skipped, chat {0} not found", chat.toJid());
            return;
        }

        var paidData = paid ? PAID_DATA_TRUE : PAID_DATA_FALSE;
        emit3pdConversion(resolved.get(), SURFACE_ORDER, status.conversionType(), status.subtype(), paidData);
    }

    /**
     * Commits a {@code Ctwa3pdConversion} event for the chat when the third-party conversion gates pass.
     *
     * <p>The event is committed only when the chat is CTWA-eligible and the global data-sharing setting
     * is {@link CtwaDataSharingSetting#ENABLED}; it is then suppressed when the chat's ads-account LID has
     * its per-customer sharing disabled.
     *
     * @param chat        the resolved chat
     * @param surface     the conversion surface
     * @param type        the conversion type
     * @param subtype     the conversion subtype
     * @param paidData    the paid-data metadata JSON
     */
    @WhatsAppWebExport(moduleName = "WAWebSmb3pdConversionSignalAction", exports = "log3pdConversionSignalForChats", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emit3pdConversion(Chat chat, String surface, String type, String subtype, String paidData) {
        var eligibility = resolveEligibility(chat);
        if (eligibility.isEmpty() || globalSetting() != CtwaDataSharingSetting.ENABLED) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "ctwa 3pd conversion skipped, not eligible or sharing disabled for chat {0}", chat.toJid());
            }
            return;
        }
        if (abPropsService.getBool(ABProp.PER_CUSTOMER_DATA_SHARING_CONTROLS_ELIGIBLE) && !perCustomerEnabled(chat)) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "ctwa 3pd conversion skipped, per-customer sharing disabled for chat {0}", chat.toJid());
            }
            return;
        }

        var event = new Ctwa3pdConversionEventBuilder()
                .ctwa3pdSchemaVersion(SCHEMA_VERSION)
                .ctwa3pdSurfaceType(surface)
                .ctwa3pdConversionType(type)
                .ctwa3pdConversionSubtype(subtype)
                .ctwa3pdConversionMetadata(paidData)
                .ctwaConversationDepth((int) conversationDepth(chat))
                .ctwaSignals(eligibility.get().ctwaSignals())
                .ctwaTrackingPayload(eligibility.get().trackingPayload())
                .build();
        wamService.commit(event);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ctwa 3pd conversion committed, surface={0} type={1} subtype={2}", surface, type, subtype);
    }

    /**
     * Commits one {@code CtwaLabelSignal} event per label type when the label-signal gates pass.
     *
     * <p>The events are committed only when the chat is CTWA-eligible and the global data-sharing setting
     * is not {@link CtwaDataSharingSetting#NOT_SET}. Each event reports the global setting as
     * {@code globalSharingSettingEnabled}, the per-customer consent as {@code customerAdsSharingSettingEnabled},
     * the chat-thread HMAC, and the per-label paid-data metadata.
     *
     * @param chat       the resolved chat
     * @param labelTypes the label types to emit a signal for
     */
    @WhatsAppWebExport(moduleName = "WAWebSmbMarkAsXLabelAction", exports = "logLabelAddedToChatAction", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitLabelSignals(Chat chat, List<CtwaLabelType> labelTypes) {
        var eligibility = resolveEligibility(chat);
        var global = globalSetting();
        if (eligibility.isEmpty() || global == CtwaDataSharingSetting.NOT_SET) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "ctwa label signal skipped, not eligible or sharing not set for chat {0}", chat.toJid());
            }
            return;
        }

        var depth = (int) conversationDepth(chat);
        var threadHmac = threadIdHmac(chat);
        var customerState = customerAdsSharingState(chat);
        var enabled = global == CtwaDataSharingSetting.ENABLED;
        var perCustomerControls = abPropsService.getBool(ABProp.PER_CUSTOMER_DATA_SHARING_CONTROLS_ELIGIBLE);
        for (var labelType : labelTypes) {
            var builder = new CtwaLabelSignalEventBuilder()
                    .ctwaConversationDepth(depth)
                    .ctwaLabelSignalVersion(LABEL_SIGNAL_VERSION)
                    .ctwaLabelTarget(CtwaLabelTarget.CHAT)
                    .ctwaLabelType(labelType)
                    .ctwaSignalMetadata(labelPaidData(labelType))
                    .deepLinkConversionSource(eligibility.get().source())
                    .globalSharingSettingEnabled(enabled)
                    .customerAdsSharingSettingEnabled(customerState);
            if (enabled && !perCustomerControls) {
                builder.eventSharingSettingEnabled(true);
            }
            if (!threadHmac.isEmpty()) {
                builder.threadIdHmac(threadHmac);
            }
            wamService.commit(builder.build());
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ctwa label signal committed, chat {0} count={1}", chat.toJid(), labelTypes.size());
    }

    /**
     * Resolves the CTWA eligibility of a chat from its inbound ad-originated message.
     *
     * <p>This scans the chat's messages for the first inbound message whose context info reports the
     * {@code FB_Ads} conversion source and carries conversion data, decoding that data as the UTF-8
     * tracking payload and reading the CTWA signals from the same context.
     *
     * @implNote
     * This implementation reconstructs WhatsApp Web's {@code ConversionTupleCollection} lookup from the
     * message store: the chat's origin conversion is carried on the inbound ad-click message's context
     * info, so the not-from-me filter selects that message rather than a later outbound reply.
     *
     * @param chat the chat to inspect
     * @return the eligibility, or empty when no inbound {@code FB_Ads} conversion is present
     */
    @WhatsAppWebExport(moduleName = "WAWebCommonCTWADataSharing", exports = "getCTWAEligibilityFromChat", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebGetCTWAEligibilityFromConversion", exports = "getCTWAEligibilityFromConversion", adaptation = WhatsAppAdaptation.ADAPTED)
    private Optional<CtwaEligibility> resolveEligibility(Chat chat) {
        try (var messages = chat.messages()) {
            return messages
                    .filter(message -> !message.key().fromMe())
                    .map(LiveCtwaConversionSignalService::ctwaContextInfo)
                    .flatMap(Optional::stream)
                    .filter(context -> context.conversionSource().filter(SOURCE_FB_ADS::equals).isPresent()
                            && context.conversionData().isPresent())
                    .findFirst()
                    .map(context -> new CtwaEligibility(
                            SOURCE_FB_ADS,
                            new String(context.conversionData().orElseThrow(), StandardCharsets.UTF_8),
                            context.ctwaSignals().orElse(null)));
        }
    }

    /**
     * Counts the conversation depth of a chat as the number of inbound-to-outbound transitions.
     *
     * <p>This scans the chat's plain-text and interactive messages in order and increments the depth
     * each time an outbound message immediately follows an inbound one, matching WhatsApp Web's
     * biz-reply count.
     *
     * @implNote
     * This implementation restricts the scan to the messages whose innermost content is an
     * {@link ExtendedTextMessage} or an {@link InteractiveMessage}, the Cobalt counterparts of WhatsApp
     * Web's {@code MSG_TYPE.CHAT} and {@code MSG_TYPE.INTERACTIVE}; the leading message never increments
     * because there is no preceding inbound message to transition from.
     *
     * @param chat the chat to inspect
     * @return the conversation depth
     */
    @WhatsAppWebExport(moduleName = "WAWebCtwaConversationDepthUtils", exports = "getCtwaConversationDepth", adaptation = WhatsAppAdaptation.ADAPTED)
    private long conversationDepth(Chat chat) {
        long depth = 0;
        Boolean previousFromMe = null;
        try (var messages = chat.messages()) {
            for (var message : (Iterable<ChatMessageInfo>) messages::iterator) {
                if (!isDepthCounted(message)) {
                    continue;
                }
                var fromMe = message.key().fromMe();
                if (fromMe && Boolean.FALSE.equals(previousFromMe)) {
                    depth++;
                }
                previousFromMe = fromMe;
            }
        }
        return depth;
    }

    /**
     * Returns the business account's global CTWA data-sharing setting, fetching it once when absent.
     *
     * <p>The runtime-only business store starts each session without the setting, so the first call on a
     * CTWA-eligible conversion fetches it from the relay through
     * {@link LinkedWhatsAppClient#refreshBusinessDataSharingSetting()} and caches it for the remainder of
     * the session; the fetched value (including {@link CtwaDataSharingSetting#NOT_SET}) is then present on
     * the store, so later calls in the same session do not re-fetch.
     *
     * @return the stored or freshly fetched tri-state setting
     */
    @WhatsAppWebExport(moduleName = "WAWebCTWADataSharingModel", exports = "getValue", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebCommonCTWADataSharing", exports = "fetchDataSharingSettingAndUpdateModel", adaptation = WhatsAppAdaptation.ADAPTED)
    private CtwaDataSharingSetting globalSetting() {
        return client.store().businessStore().ctwaDataSharingSetting()
                .orElseGet(this::fetchGlobalSetting);
    }

    /**
     * Fetches the business account's global CTWA data-sharing setting from the relay.
     *
     * @return the freshly fetched tri-state setting
     */
    private CtwaDataSharingSetting fetchGlobalSetting() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ctwa global data sharing setting not cached, fetching from server");
        return client.refreshBusinessDataSharingSetting();
    }

    /**
     * Returns whether the chat's ads-account LID has per-customer CTWA data sharing enabled.
     *
     * @param chat the chat to inspect
     * @return {@code true} when no per-customer preference disables sharing for the chat's account LID
     */
    @WhatsAppWebExport(moduleName = "WAWebDataSharing3pdLidCollection", exports = "isDataSharingEnabled", adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean perCustomerEnabled(Chat chat) {
        return chat.accountLid()
                .flatMap(lid -> client.store().businessStore().findCtwaDataSharing(lid.toString()))
                .map(CtwaDataSharingPreference::enabled)
                .orElse(true);
    }

    /**
     * Maps the chat's per-customer CTWA consent to the label-signal {@code customerAdsSharingSettingEnabled}
     * value.
     *
     * <p>When per-customer data-sharing controls are not eligible the value is forced to
     * {@link CustomerAdsSharingSettingEnabled#UNSET}; otherwise it reflects the stored per-customer
     * preference for the chat's ads-account LID.
     *
     * @param chat the chat to inspect
     * @return {@link CustomerAdsSharingSettingEnabled#UNSET} when per-customer controls are not eligible or
     *         no preference is stored, otherwise {@link CustomerAdsSharingSettingEnabled#TRUE} or
     *         {@link CustomerAdsSharingSettingEnabled#FALSE} from the stored preference
     */
    @WhatsAppWebExport(moduleName = "WAWebPerCustomerDataSharingUtils", exports = "getCustomerAdsDataSharingState", adaptation = WhatsAppAdaptation.ADAPTED)
    private CustomerAdsSharingSettingEnabled customerAdsSharingState(Chat chat) {
        if (!abPropsService.getBool(ABProp.PER_CUSTOMER_DATA_SHARING_CONTROLS_ELIGIBLE)) {
            return CustomerAdsSharingSettingEnabled.UNSET;
        }
        return chat.accountLid()
                .flatMap(lid -> client.store().businessStore().findCtwaDataSharing(lid.toString()))
                .map(preference -> preference.enabled()
                        ? CustomerAdsSharingSettingEnabled.TRUE
                        : CustomerAdsSharingSettingEnabled.FALSE)
                .orElse(CustomerAdsSharingSettingEnabled.UNSET);
    }

    /**
     * Returns the chat-thread HMAC stamped on the label signal.
     *
     * @param chat the chat to inspect
     * @return the Base64 chat-thread HMAC, or the empty string when no thread-logging secret is provisioned
     */
    @WhatsAppWebExport(moduleName = "WAWebChatThreadLogging", exports = "getChatThreadIDHMAC", adaptation = WhatsAppAdaptation.ADAPTED)
    private String threadIdHmac(Chat chat) {
        return LiveThreadLoggingService.chatThreadIdHmac(client, chat.toJid().toString());
    }

    /**
     * Returns the innermost context info of an inbound message, if its content carries one.
     *
     * @param message the message to inspect
     * @return the context info, or empty when the content is not contextual or carries no context
     */
    private static Optional<ContextInfo> ctwaContextInfo(ChatMessageInfo message) {
        return message.message().contextualContent().flatMap(ContextualMessage::contextInfo);
    }

    /**
     * Returns whether a message participates in the conversation-depth scan.
     *
     * @param message the message to classify
     * @return {@code true} when the innermost content is plain text or an interactive message
     */
    private static boolean isDepthCounted(ChatMessageInfo message) {
        var content = message.message().content();
        return content instanceof ExtendedTextMessage || content instanceof InteractiveMessage;
    }

    /**
     * Resolves a predefined label identifier to its CTWA label type, conversion type and conversion
     * subtype.
     *
     * @implNote
     * This implementation accepts the {@code IMPORTANT} identifier (6) unconditionally; WhatsApp Web
     * gates it behind {@code is3pdImportantLabelSignalsEnabled()}, a business-gating flag unreachable from
     * this service. The conversion type follows WhatsApp Web's reducer: follow-up, lead and important map
     * to {@code lead_created}, new order and new customer map to {@code order_created}, and the remaining
     * order identifiers map to {@code order_updated}.
     *
     * @param predefinedId the label's predefined identifier
     * @return the matching conversion, or {@code null} when the identifier is not an allowed CTWA identifier
     */
    @WhatsAppWebExport(moduleName = "WAWebSmb3pdConversionSignalAction", exports = "log3pdConversionSignalForChats", adaptation = WhatsAppAdaptation.ADAPTED)
    private static LabelConversion labelConversionFor(int predefinedId) {
        return switch (predefinedId) {
            case 1 -> new LabelConversion(CtwaLabelType.NEW_CUSTOMER, "order_created", "new_customer");
            case 2 -> new LabelConversion(CtwaLabelType.NEW_ORDER, "order_created", "new_order");
            case 3 -> new LabelConversion(CtwaLabelType.PENDING_PAYMENT, "order_updated", "pending_payment");
            case 4 -> new LabelConversion(CtwaLabelType.PAID, "order_updated", "paid");
            case 5 -> new LabelConversion(CtwaLabelType.ORDER_COMPLETE, "order_updated", "order_complete");
            case 6 -> new LabelConversion(CtwaLabelType.IMPORTANT, "lead_created", "important");
            case 7 -> new LabelConversion(CtwaLabelType.FOLLOW_UP, "lead_created", "follow_up");
            case 8 -> new LabelConversion(CtwaLabelType.LEAD, "lead_created", "lead");
            default -> null;
        };
    }

    /**
     * Returns the paid-data metadata JSON for a CTWA label type.
     *
     * @implNote
     * This implementation mirrors WhatsApp Web's reducer: follow-up and lead report the empty object,
     * paid reports {@code {"paid":true}}, and every other label type reports {@code {"paid":false}}.
     *
     * @param labelType the label type
     * @return the paid-data metadata JSON
     */
    private static String labelPaidData(CtwaLabelType labelType) {
        return switch (labelType) {
            case FOLLOW_UP, LEAD -> PAID_DATA_EMPTY;
            case PAID -> PAID_DATA_TRUE;
            default -> PAID_DATA_FALSE;
        };
    }

    /**
     * The resolved CTWA eligibility of a chat.
     *
     * @param source          the conversion source, always {@code FB_Ads}
     * @param trackingPayload the UTF-8 decoded conversion data
     * @param ctwaSignals     the CTWA signals, or {@code null} when none are attached
     */
    private record CtwaEligibility(String source, String trackingPayload, String ctwaSignals) {
    }

    /**
     * The CTWA conversion mapping for a predefined label identifier.
     *
     * @param labelType      the CTWA label type
     * @param conversionType the third-party conversion type
     * @param subtype        the third-party conversion subtype
     */
    private record LabelConversion(CtwaLabelType labelType, String conversionType, String subtype) {
    }
}
