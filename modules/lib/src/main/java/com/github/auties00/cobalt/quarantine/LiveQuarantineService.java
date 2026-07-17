package com.github.auties00.cobalt.quarantine;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.NewMessageListener;
import com.github.auties00.cobalt.listener.linked.LinkedMessageQuarantinedListener;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.contact.Contact;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.QuarantinedMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.call.CallLogMessage;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactMessage;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactsArrayMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.TemplateButton;
import com.github.auties00.cobalt.wire.linked.message.interactive.TemplateMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LiveLocationMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LocationMessage;
import com.github.auties00.cobalt.wire.linked.message.media.AlbumMessage;
import com.github.auties00.cobalt.wire.linked.message.media.AudioMessage;
import com.github.auties00.cobalt.wire.linked.message.media.DocumentMessage;
import com.github.auties00.cobalt.wire.linked.message.media.ImageMessage;
import com.github.auties00.cobalt.wire.linked.message.media.StickerMessage;
import com.github.auties00.cobalt.wire.linked.message.media.VideoMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollUpdateMessage;
import com.github.auties00.cobalt.wire.linked.message.security.EncReactionMessage;
import com.github.auties00.cobalt.wire.linked.message.system.KeepInChatMessage;
import com.github.auties00.cobalt.wire.linked.message.system.PinInChatMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.message.text.HighlyStructuredMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessage;
import com.github.auties00.cobalt.wire.linked.privacy.DefenseModePrivacyValue;
import com.github.auties00.cobalt.wire.linked.privacy.PrivacySettingType;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.DefenseModeQuarantineEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.DefenseModeQuarantineAction;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Production {@link QuarantineService} backed by a {@link LinkedWhatsAppClient}.
 *
 * @implNote
 * This implementation classifies {@link LinkedMessageContainer#content()} (the envelope-unwrapped
 * message) rather than re-deriving WA Web's separate {@code maybeGetFutureproofMessage} pass:
 * because {@code content()} already unwraps every view-once, ephemeral and futureproof layer to
 * the innermost displayable message, classifying it yields the same result as WA Web's
 * stronger-of-outer-and-inner rule (the outer futureproof envelope itself has no displayable
 * content and so never raises the verdict).
 */
@WhatsAppWebModule(moduleName = "WAWebQuarantineActionUtils")
public final class LiveQuarantineService implements QuarantineService {
    /**
     * The logger for {@link LiveQuarantineService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveQuarantineService.class);

    /**
     * The bound client, used to read Defense Mode state, the account identity and the contact
     * roster.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The AB-props service consulted for the Defense Mode feature flags.
     */
    private final ABPropsService abPropsService;

    /**
     * The telemetry sink for the {@code DefenseModeQuarantine} metrics.
     */
    private final WamService wamService;

    /**
     * The keys of the messages quarantined during this session, used to drive {@link #restoreAll()}
     * without scanning the message store.
     */
    private final Set<MessageKey> quarantinedKeys;

    /**
     * Constructs a service bound to the given collaborators.
     *
     * @param client         the bound client, must not be {@code null}
     * @param abPropsService the AB-props service, must not be {@code null}
     * @param wamService     the telemetry sink, must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public LiveQuarantineService(LinkedWhatsAppClient client, ABPropsService abPropsService, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.quarantinedKeys = ConcurrentHashMap.newKeySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebQuarantineActionUtils", exports = "getQuarantineAction", adaptation = WhatsAppAdaptation.ADAPTED)
    public QuarantineAction getQuarantineAction(LinkedMessageContainer message, Jid sender) {
        if (message == null
                || !abPropsService.getBool(ABProp.DEFENSE_MODE_QUARANTINE)
                || !isDefenseModeActive()
                || !isQuarantinableSender(sender)) {
            return QuarantineAction.NO_QUARANTINE;
        }
        return classify(message.content());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebQuarantineDataStore", exports = "bulkCreateOrReplaceQuarantineData", adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean quarantine(ChatMessageInfo info) {
        var action = getQuarantineAction(info.message(), info.senderJid().orElse(null));
        if (!action.shouldQuarantine()) {
            return false;
        }
        info.setQuarantinedMessage(new QuarantinedMessageBuilder()
                .originalData(LinkedMessageContainerSpec.encode(info.message()))
                .extractedText(action.extractedText().orElse(null))
                .build());
        quarantinedKeys.add(info.key());
        wamService.commit(new DefenseModeQuarantineEventBuilder()
                .quarantineAction(DefenseModeQuarantineAction.QUARANTINED_MSG)
                .defenseModeQuarantineEventCount(1)
                .build());
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "quarantined message id={0} sender={1}",
                    info.key().id().orElse(null), info.senderJid().orElse(null));
        }
        for (var listener : client.store().listeners()) {
            if (listener instanceof LinkedMessageQuarantinedListener typed) {
                Thread.startVirtualThread(() -> typed.onMessageQuarantined(client, info));
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUnquarantineMessageJob", exports = "unquarantineMessageJob", adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean restore(ChatMessageInfo info) {
        boolean restored;
        try {
            restored = reinject(info);
        } catch (RuntimeException exception) {
            restored = false;
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "quarantine restore failed for id=" + info.key().id().orElse(null), exception);
            }
        }
        wamService.commit(new DefenseModeQuarantineEventBuilder()
                .quarantineAction(restored
                        ? DefenseModeQuarantineAction.QUARANTINE_RESTORE_SUCCESS
                        : DefenseModeQuarantineAction.QUARANTINE_RESTORE_FAILED)
                .build());
        return restored;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation drives the restore from the in-memory {@link #quarantinedKeys} index
     * rather than scanning the message store. Messages quarantined in an earlier session are not in
     * the index and so are not auto-restored; releasing them would require a persisted index or a
     * full store scan.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBulkUnquarantineMessagesJob", exports = "bulkUnquarantineMessagesJob", adaptation = WhatsAppAdaptation.ADAPTED)
    public void restoreAll() {
        var restored = 0;
        for (var key : Set.copyOf(quarantinedKeys)) {
            if (!(client.store().chatStore().findMessageByKey(key).orElse(null) instanceof ChatMessageInfo info)) {
                quarantinedKeys.remove(key);
                continue;
            }
            try {
                if (reinject(info)) {
                    restored++;
                }
            } catch (RuntimeException exception) {
                // a single failed restore does not abort the bulk pass
                if (Log.WARNING) LOGGER.log(Level.WARNING, "quarantine restore failed during bulk restore", exception);
            }
        }
        if (restored > 0) {
            wamService.commit(new DefenseModeQuarantineEventBuilder()
                    .quarantineAction(DefenseModeQuarantineAction.QUARANTINE_RESTORE_AUTO)
                    .defenseModeQuarantineEventCount(restored)
                    .build());
            if (Log.INFO) LOGGER.log(Level.INFO, "restored {0} quarantined messages", restored);
        }
    }

    /**
     * Clears the quarantine flag on a message and re-delivers it to the {@code onNewMessage}
     * listeners.
     *
     * @param info the quarantined chat message
     * @return {@code true} when the message was quarantined and is now restored, {@code false} when
     *         it was not quarantined
     */
    private boolean reinject(ChatMessageInfo info) {
        if (info.quarantinedMessage().isEmpty()) {
            return false;
        }
        info.setQuarantinedMessage(null);
        quarantinedKeys.remove(info.key());
        for (var listener : client.store().listeners()) {
            if (listener instanceof NewMessageListener typed) {
                Thread.startVirtualThread(() -> typed.onNewMessage(client, info));
            }
        }
        return true;
    }

    /**
     * Returns whether Defense Mode is currently active for the account.
     *
     * @return {@code true} when the feature is available and the Defense Mode setting is at the
     *         standard tier, {@code false} otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebQuarantineActionUtils", exports = "isDefenseModeOn", adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isDefenseModeActive() {
        return abPropsService.getInt(ABProp.DEFENSE_MODE_AVAILABLE) >= 1
                && client.store().settingsStore().findPrivacySetting(PrivacySettingType.DEFENSE_MODE)
                .orElse(null) instanceof DefenseModePrivacyValue.OnStandard;
    }

    /**
     * Returns whether the given sender's messages are subject to quarantine.
     *
     * @implNote
     * This implementation mirrors WA Web's {@code shouldQuarantineSender}: the sender must be a
     * {@linkplain #isUser(Jid) user, LID or bot JID} that is neither the current account nor one of
     * the exempt system senders ({@linkplain #isPsa(Jid) PSA}, {@linkplain #isIas(Jid) in-app
     * support}, {@linkplain #isOfficialBusinessAccount(Jid) official business},
     * {@linkplain #isSupportAccount(Jid) support}, {@linkplain #isCapiSupportAccount(Jid) CAPI
     * support}, {@linkplain #isAiHub(Jid) AI hub} or {@linkplain #isMetaAiBot(Jid) Meta AI bot}),
     * and must not be a saved address-book contact. WA Web keys the address-book test off a stored
     * {@code isAddressBookContact} flag; the {@link Contact} model has no such flag, so the presence
     * of a {@linkplain Contact#fullName() saved name} stands in for it.
     *
     * @param sender the message sender, or {@code null}
     * @return {@code true} when the sender is quarantinable, {@code false} otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebQuarantineActionUtils", exports = "shouldQuarantineSender", adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isQuarantinableSender(Jid sender) {
        if (sender == null || !isUser(sender)) {
            return false;
        }
        var account = client.store().accountStore();
        if (account.jid().filter(sender::equals).isPresent() || account.lid().filter(sender::equals).isPresent()) {
            return false;
        }
        if (isPsa(sender) || isIas(sender) || isOfficialBusinessAccount(sender)
                || isSupportAccount(sender) || isCapiSupportAccount(sender)
                || isAiHub(sender) || isMetaAiBot(sender)) {
            return false;
        }
        return client.store().contactStore().findContactByJid(sender)
                .flatMap(Contact::fullName)
                .isEmpty();
    }

    /**
     * Returns whether the given JID addresses a user, LID or bot account.
     *
     * @param sender the sender JID
     * @return {@code true} when the JID lives on the user, LID or bot server
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "isUser", adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean isUser(Jid sender) {
        return sender.hasUserServer() || sender.hasLidServer() || sender.hasBotServer();
    }

    /**
     * Returns whether the given JID addresses the public service announcements (PSA) account.
     *
     * <p>This is the {@link Jid#announcementsAccount() announcements account}, whose user component
     * is {@code 0}.
     *
     * @param sender the sender JID
     * @return {@code true} when the JID is the PSA account
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "isPSA", adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isPsa(Jid sender) {
        return sender.hasUserServer() && Jid.announcementsAccount().hasUser(sender.user());
    }

    /**
     * Returns whether the given JID addresses WhatsApp's in-app support (IAS) account.
     *
     * <p>This is the {@link Jid#inAppSupportAccount() in-app support account}, whose user component
     * is {@code 16508638904}.
     *
     * @param sender the sender JID
     * @return {@code true} when the JID is the in-app support account
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "isIAS", adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isIas(Jid sender) {
        return sender.hasUserServer() && Jid.inAppSupportAccount().hasUser(sender.user());
    }

    /**
     * Returns whether the given JID addresses the official WhatsApp business account.
     *
     * <p>This is the {@link Jid#officialBusinessAccount() official business account}, whose user
     * component is {@code 16505361212}.
     *
     * @param sender the sender JID
     * @return {@code true} when the JID is the official business account
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "isOfficialBizAccount", adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isOfficialBusinessAccount(Jid sender) {
        return sender.hasUserServer() && Jid.officialBusinessAccount().hasUser(sender.user());
    }

    /**
     * Returns whether the given JID addresses a WhatsApp support account.
     *
     * @implNote
     * This implementation mirrors WA Web: a {@linkplain Jid#hasLidServer() LID} sender is a support
     * account when its user is listed in the {@link ABProp#SUPPORT_LIDS} or
     * {@link ABProp#PAYMENT_SUPPORT_LIDS} AB prop; any other sender is a support account when its
     * user begins with one of the comma-separated prefixes in the
     * {@link ABProp#IN_APP_SUPPORT_V2_NUMBER_PREFIXES} AB prop.
     *
     * @param sender the sender JID
     * @return {@code true} when the JID is a support account
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "isSupportAccount", adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isSupportAccount(Jid sender) {
        var user = sender.user();
        if (user == null) {
            return false;
        }
        if (sender.hasLidServer()) {
            return isListedUser(ABProp.SUPPORT_LIDS, user) || isListedUser(ABProp.PAYMENT_SUPPORT_LIDS, user);
        }
        return hasListedPrefix(ABProp.IN_APP_SUPPORT_V2_NUMBER_PREFIXES, user);
    }

    /**
     * Returns whether the given JID addresses a WhatsApp CAPI support account.
     *
     * @implNote
     * This implementation mirrors WA Web: a {@linkplain Jid#hasLidServer() LID} sender is a CAPI
     * support account when its user is listed in the {@link ABProp#SUPPORT_LIDS} AB prop; any other
     * sender is a CAPI support account when its user begins with one of the comma-separated prefixes
     * in the {@link ABProp#IN_APP_SUPPORT_CAPI_NUMBER_PREFIXES} AB prop.
     *
     * @param sender the sender JID
     * @return {@code true} when the JID is a CAPI support account
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "isCAPISupportAccount", adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isCapiSupportAccount(Jid sender) {
        var user = sender.user();
        if (user == null) {
            return false;
        }
        if (sender.hasLidServer()) {
            return isListedUser(ABProp.SUPPORT_LIDS, user);
        }
        return hasListedPrefix(ABProp.IN_APP_SUPPORT_CAPI_NUMBER_PREFIXES, user);
    }

    /**
     * Returns whether the given JID addresses the Meta AI hub account.
     *
     * <p>Matches the {@link Jid#aiHubLidAccount() AI hub LID} and the
     * {@link Jid#aiHubBotAccount() AI hub bot} account.
     *
     * @param sender the sender JID
     * @return {@code true} when the JID is the AI hub LID or bot account
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "isAiHub", adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean isAiHub(Jid sender) {
        return (sender.hasLidServer() && Jid.aiHubLidAccount().hasUser(sender.user()))
                || (sender.hasBotServer() && Jid.aiHubBotAccount().hasUser(sender.user()));
    }

    /**
     * Returns whether the given JID addresses the Meta AI bot account.
     *
     * <p>Matches both the FBID Meta AI bot ({@link Jid#metaAiBotAccount()}) and the
     * {@link Jid#metaAiBotPhoneAccount() legacy phone-number Meta AI bot}.
     *
     * @param sender the sender JID
     * @return {@code true} when the JID is a Meta AI bot
     */
    @WhatsAppWebExport(moduleName = "WAWebBotUtils", exports = "isMetaAiBot", adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean isMetaAiBot(Jid sender) {
        return sender.equals(Jid.metaAiBotAccount())
                || Jid.metaAiBotPhoneAccount().hasUser(sender.user());
    }

    /**
     * Returns whether the given user is listed in the comma-separated value of the AB prop.
     *
     * @param prop the AB prop holding a comma-separated list of user identifiers
     * @param user the user component to look up
     * @return {@code true} when the user exactly matches one of the listed entries
     */
    private boolean isListedUser(ABProp prop, String user) {
        var value = abPropsService.getString(prop);
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (var entry : value.split(",")) {
            if (entry.equals(user)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the given user begins with one of the comma-separated prefixes in the value
     * of the AB prop.
     *
     * @param prop the AB prop holding a comma-separated list of number prefixes
     * @param user the user component to test
     * @return {@code true} when the user starts with one of the listed prefixes
     */
    private boolean hasListedPrefix(ABProp prop, String user) {
        var value = abPropsService.getString(prop);
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (var prefix : value.split(",")) {
            if (user.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Classifies the displayable content of a message into a quarantine action.
     *
     * <p>Media messages quarantine with their caption; an extended-text message quarantines with
     * its text only when it carries link-preview media; structured messages quarantine without
     * text unless they are free of embedded media; control and no-content messages are never
     * quarantined.
     *
     * @param content the envelope-unwrapped message content, or {@code null}
     * @return the quarantine action for the content, never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebQuarantineActionUtils", exports = "getQuarantineActionForMsg", adaptation = WhatsAppAdaptation.ADAPTED)
    private QuarantineAction classify(Message content) {
        return switch (content) {
            case ExtendedTextMessage text -> hasLinkPreviewMedia(text)
                    ? QuarantineAction.of(text.text().orElse(null))
                    : QuarantineAction.NO_QUARANTINE;
            case ImageMessage image -> QuarantineAction.of(image.caption().orElse(null));
            case VideoMessage video -> QuarantineAction.of(video.caption().orElse(null));
            case DocumentMessage document -> QuarantineAction.of(document.caption().orElse(null));
            case HighlyStructuredMessage structured -> isStructuredSafe(structured)
                    ? QuarantineAction.NO_QUARANTINE : QuarantineAction.WITHOUT_TEXT;
            case TemplateMessage template -> isStructuredSafe(template)
                    ? QuarantineAction.NO_QUARANTINE : QuarantineAction.WITHOUT_TEXT;
            case InteractiveMessage interactive -> isStructuredSafe(interactive)
                    ? QuarantineAction.NO_QUARANTINE : QuarantineAction.WITHOUT_TEXT;
            case CallLogMessage _ -> QuarantineAction.NO_QUARANTINE;
            case ProtocolMessage _ -> QuarantineAction.NO_QUARANTINE;
            case ReactionMessage _ -> QuarantineAction.NO_QUARANTINE;
            case EncReactionMessage _ -> QuarantineAction.NO_QUARANTINE;
            case PollUpdateMessage _ -> QuarantineAction.NO_QUARANTINE;
            case KeepInChatMessage _ -> QuarantineAction.NO_QUARANTINE;
            case PinInChatMessage _ -> QuarantineAction.NO_QUARANTINE;
            case AlbumMessage _ -> QuarantineAction.NO_QUARANTINE;
            case AudioMessage _ -> QuarantineAction.WITHOUT_TEXT;
            case StickerMessage _ -> QuarantineAction.WITHOUT_TEXT;
            case ContactMessage _ -> QuarantineAction.WITHOUT_TEXT;
            case ContactsArrayMessage _ -> QuarantineAction.WITHOUT_TEXT;
            case LocationMessage _ -> QuarantineAction.WITHOUT_TEXT;
            case LiveLocationMessage _ -> QuarantineAction.WITHOUT_TEXT;
            case null, default -> QuarantineAction.NO_QUARANTINE;
        };
    }

    /**
     * Returns whether an extended-text message carries link-preview media.
     *
     * @param text the extended-text message
     * @return {@code true} when any thumbnail, direct path or media key is present
     */
    private boolean hasLinkPreviewMedia(ExtendedTextMessage text) {
        return text.jpegThumbnail().isPresent()
                || text.thumbnailDirectPath().isPresent()
                || text.mediaKey().isPresent()
                || text.thumbnailSha256().isPresent();
    }

    /**
     * Returns whether a highly-structured message is free of embedded media.
     *
     * @param structured the highly-structured message
     * @return {@code true} when its hydrated template is absent or itself media-free
     */
    private boolean isStructuredSafe(HighlyStructuredMessage structured) {
        return structured.hydratedHsm()
                .map(this::isStructuredSafe)
                .orElse(true);
    }

    /**
     * Returns whether a template message is free of embedded media across its format variant and
     * hydrated template.
     *
     * @param template the template message
     * @return {@code true} when no variant carries media, {@code false} otherwise
     */
    private boolean isStructuredSafe(TemplateMessage template) {
        var formatSafe = switch (template.format().orElse(null)) {
            case TemplateMessage.FourRowTemplate fourRow -> isStructuredSafe(fourRow);
            case TemplateMessage.HydratedFourRowTemplate hydrated -> isStructuredSafe(hydrated);
            case InteractiveMessage interactive -> isStructuredSafe(interactive);
            case null, default -> true;
        };
        return formatSafe && template.hydratedTemplate()
                .map(this::isStructuredSafe)
                .orElse(true);
    }

    /**
     * Returns whether a four-row template is free of embedded media in its title, content, footer
     * and buttons.
     *
     * @param fourRow the four-row template
     * @return {@code true} when the template carries no media, {@code false} otherwise
     */
    private boolean isStructuredSafe(TemplateMessage.FourRowTemplate fourRow) {
        var title = fourRow.title().orElse(null);
        if (title != null && !(title instanceof HighlyStructuredMessage)) {
            return false;
        }
        if (!fourRow.content().map(this::isStructuredSafe).orElse(true)) {
            return false;
        }
        if (!fourRow.footer().map(this::isStructuredSafe).orElse(true)) {
            return false;
        }
        for (var button : fourRow.buttons()) {
            if (!isStructuredSafe(button)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether a hydrated four-row template is free of embedded media in its title.
     *
     * @param hydrated the hydrated four-row template
     * @return {@code true} when the title is absent or plain text, {@code false} when it is media
     */
    private boolean isStructuredSafe(TemplateMessage.HydratedFourRowTemplate hydrated) {
        var title = hydrated.title().orElse(null);
        return title == null || title instanceof TemplateMessage.TitleSpec.HydratedTitleText;
    }

    /**
     * Returns whether an interactive message is free of embedded media in its header, content and
     * carousel cards.
     *
     * @param interactive the interactive message
     * @return {@code true} when no media, shop or collection content is present, {@code false}
     *         otherwise
     */
    private boolean isStructuredSafe(InteractiveMessage interactive) {
        if (interactive.header().map(header -> header.media().isPresent()).orElse(false)) {
            return false;
        }
        var content = interactive.content().orElse(null);
        if (content instanceof InteractiveMessage.ShopMessage || content instanceof InteractiveMessage.CollectionMessage) {
            return false;
        }
        if (content instanceof InteractiveMessage.CarouselMessage carousel) {
            for (var card : carousel.cards()) {
                if (!isStructuredSafe(card)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns whether a template button is free of embedded media in its display text and
     * payload.
     *
     * @param button the template button
     * @return {@code true} when every structured field is media-free, {@code false} otherwise
     */
    private boolean isStructuredSafe(TemplateButton button) {
        return switch (button.button().orElse(null)) {
            case TemplateButton.QuickReplyButton quick ->
                    quick.displayText().map(this::isStructuredSafe).orElse(true);
            case TemplateButton.URLButton url ->
                    url.displayText().map(this::isStructuredSafe).orElse(true)
                            && url.url().map(this::isStructuredSafe).orElse(true);
            case TemplateButton.CallButton call ->
                    call.displayText().map(this::isStructuredSafe).orElse(true)
                            && call.phoneNumber().map(this::isStructuredSafe).orElse(true);
            case null, default -> true;
        };
    }
}
