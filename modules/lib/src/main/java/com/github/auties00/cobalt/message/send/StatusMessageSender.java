package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.ack.AckParser;
import com.github.auties00.cobalt.ack.AckResult;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryptedPayload;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.message.send.senderkey.SenderKeyDistribution;
import com.github.auties00.cobalt.message.send.stanza.ChatFanoutStanza;
import com.github.auties00.cobalt.message.send.stanza.MetaStanza;
import com.github.auties00.cobalt.message.send.stanza.ParticipantsStanza;
import com.github.auties00.cobalt.message.send.stanza.ReportingStanza;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.contact.Contact;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.linked.message.media.AudioMessage;
import com.github.auties00.cobalt.wire.linked.message.media.DocumentMessage;
import com.github.auties00.cobalt.wire.linked.message.media.ImageMessage;
import com.github.auties00.cobalt.wire.linked.message.media.StickerMessage;
import com.github.auties00.cobalt.wire.linked.message.media.VideoMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.privacy.StatusPrivacySetting;
import com.github.auties00.cobalt.wire.linked.privacy.StatusPrivacyMode;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.PrekeysDepletionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.StatusPostEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.PrekeysFetchContext;
import com.github.auties00.cobalt.wire.wam.type.PrivacySettingsValueType;
import com.github.auties00.cobalt.wire.wam.type.SizeBucket;
import com.github.auties00.cobalt.wire.wam.type.StatusCategory;
import com.github.auties00.cobalt.wire.wam.type.StatusPostOrigin;
import com.github.auties00.cobalt.wire.wam.type.StatusPostResult;

import java.lang.System.Logger.Level;
import java.util.*;

/**
 * Publishes a status update to the {@code status@broadcast} JID.
 *
 * <p>Status broadcasts share the sender-key (SKMSG) pipeline used for group
 * sends but resolve the audience from the user's status privacy preferences
 * rather than from a group's participant list. The outgoing stanza carries a
 * {@code <meta status_setting="...">} child that mirrors the configured privacy
 * mode (suppressed for revokes). Revokes whose original audience is no longer
 * covered by the current privacy preference take a dedicated per-device direct
 * path so the revoke reaches the original recipients even if they would not
 * normally see new status updates.
 */
@WhatsAppWebModule(moduleName = "WAWebEncryptAndSendStatusMsg")
final class StatusMessageSender extends MessageSender<ChatMessageInfo> {
    /**
     * The logger for {@link StatusMessageSender}.
     */
    private static final System.Logger LOGGER = Log.get(StatusMessageSender.class);

    /**
     * Performs both the SKMSG group encryption and the per-device path taken by
     * direct revokes.
     */
    private final MessageEncryption encryption;

    /**
     * Resolves the audience fanout and manages Signal sessions to audience
     * devices.
     */
    private final DeviceService deviceService;

    /**
     * Encrypts the per-device sender-key distribution payloads.
     */
    private final SenderKeyDistribution senderKeyDistribution;

    /**
     * Builds the {@code status_setting} and other meta attributes on the stanza.
     */
    private final MetaStanza metaStanza;

    /**
     * Builds the {@code <reporting>} child payload.
     */
    private final ReportingStanza reportingStanza;

    /**
     * Constructs a {@link StatusMessageSender} bound to the supplied
     * dependencies.
     *
     * <p>Constructed once by {@link MessageSendingService}; embedders should not
     * instantiate directly.
     *
     * @param client                the {@link LinkedWhatsAppClient} used to dispatch
     *                              stanzas
     * @param encryption            the {@link MessageEncryption} service
     * @param deviceService         the {@link DeviceService} used for audience
     *                              fanout
     * @param abPropsService        the {@link ABPropsService} consulted by the
     *                              base sender
     * @param senderKeyDistribution the {@link SenderKeyDistribution} service
     * @param metaStanza            the {@link MetaStanza} builder
     * @param reportingStanza       the {@link ReportingStanza} builder
     * @param wamService            the {@link WamService} shared with the base
     *                              sender
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebEncryptAndSendStatusMsg", exports = "encryptAndSendStatusMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    StatusMessageSender(
            LinkedWhatsAppClient client,
            MessageEncryption encryption,
            DeviceService deviceService,
            ABPropsService abPropsService,
            SenderKeyDistribution senderKeyDistribution,
            MetaStanza metaStanza,
            ReportingStanza reportingStanza,
            WamService wamService
    ) {
        super(client, abPropsService, wamService);
        this.encryption = Objects.requireNonNull(encryption, "encryption");
        this.deviceService = Objects.requireNonNull(deviceService, "deviceService");
        this.senderKeyDistribution = Objects.requireNonNull(senderKeyDistribution, "senderKeyDistribution");
        this.metaStanza = Objects.requireNonNull(metaStanza, "metaStanza");
        this.reportingStanza = Objects.requireNonNull(reportingStanza, "reportingStanza");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Encrypts the payload with the status sender key, distributes the sender
     * key to audience devices that do not yet hold it, and dispatches the SKMSG
     * stanza. Revokes that cover an audience subset which is no longer reachable
     * by SKMSG fall back to the per-device direct path via
     * {@link #sendDirectRevoke(Jid, ChatMessageInfo, Collection)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebEncryptAndSendStatusMsg", exports = "encryptAndSendStatusMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public AckResult doSend(Jid statusJid, ChatMessageInfo messageInfo) {
        var container = messageInfo.message();
        var selfJid = requireSelfJid();

        var statusAudience = resolveStatusAudience();
        var audienceDevices = deviceService.getStatusFanout(statusAudience);

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "status send audience resolved: {0} users, {1} devices",
                    statusAudience.size(), audienceDevices.size());
        }

        var revokeResult = resolveRevokeDevices(container, audienceDevices);
        if (revokeResult.useDirect()) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "status revoke requires direct path for {0}", messageInfo.key().id().orElse(null));
            }
            return sendDirectRevoke(statusJid, messageInfo, revokeResult.devices());
        }

        var allDevices = revokeResult.devices();

        var messageId = messageInfo.key().id().orElseThrow();
        store.chatStore().createOrMergeReceiptRecords(messageId, allDevices);

        var skDistribDevices = new ArrayList<Jid>();
        var skExistingDevices = new ArrayList<Jid>();
        for (var device : allDevices) {
            if (store.signalStore().hasSenderKeyDistributed(statusJid, device)) {
                skExistingDevices.add(device);
            } else {
                skDistribDevices.add(device);
            }
        }

        var rotateKey = store.signalStore().clearKeyRotation(statusJid);
        if (rotateKey) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "rotating status sender key for {0}", statusJid);
            }
            encryption.rotateSenderKey(statusJid, selfJid);
            skDistribDevices.addAll(skExistingDevices);
            skExistingDevices.clear();
        }

        var plaintext = LinkedMessageContainerSpec.encode(container);
        var skmsgPayload = encryption.encryptForGroup(statusJid, selfJid, plaintext);
        var senderKeyBytes = encryption.getSenderKeyBytes(statusJid, selfJid);

        List<MessageEncryptedPayload> skDistPayloads;
        if (skDistribDevices.isEmpty()) {
            skDistPayloads = List.of();
        } else {
            var depletedPrekeyCount = deviceService.ensureSessions(skDistribDevices);
            emitPrekeysDepletionEvents(depletedPrekeyCount, MessageType.STATUS, allDevices.size());
            skDistPayloads = senderKeyDistribution.encrypt(statusJid, senderKeyBytes, skDistribDevices);
        }

        var participantsChildren = new ArrayList<Stanza>();
        for (var payload : skDistPayloads) {
            if (payload.recipientJid() == null) {
                continue;
            }
            var encNode = new StanzaBuilder()
                    .description("enc")
                    .attribute("v", String.valueOf(MessageEncryption.CIPHERTEXT_VERSION))
                    .attribute("type", payload.type().protocolValue())
                    .content(payload.ciphertext())
                    .build();
            participantsChildren.add(new StanzaBuilder()
                    .description("to")
                    .attribute("jid", payload.recipientJid())
                    .content(encNode)
                    .build());
        }
        for (var device : skExistingDevices) {
            participantsChildren.add(new StanzaBuilder()
                    .description("to")
                    .attribute("jid", device)
                    .build());
        }

        var participantsNode = participantsChildren.isEmpty() ? null : new StanzaBuilder()
                .description("participants")
                .content(participantsChildren)
                .build();

        store.signalStore().updateIdentityRange(allDevices);

        var skmsgEncNode = new StanzaBuilder()
                .description("enc")
                .attribute("v", String.valueOf(MessageEncryption.CIPHERTEXT_VERSION))
                .attribute("type", MessageEncryptionType.SKMSG.protocolValue())
                .attribute("mediatype", resolveMediaType(container))
                .content(skmsgPayload.ciphertext())
                .build();

        var identityNode = ParticipantsStanza.requiresIdentityNode(skDistPayloads)
                ? buildIdentityNode() : null;

        var isRevoke = container.content() instanceof ProtocolMessage pm
                && pm.type().orElse(null) == ProtocolMessage.Type.REVOKE;
        var statusSetting = isRevoke ? null : resolveStatusSetting();
        var metaNode = metaStanza.buildChat(statusJid, container, statusSetting);

        var reportingNode = reportingStanza.build(messageInfo, selfJid, statusJid);

        var stanza = new StanzaBuilder()
                .description("message")
                .attribute("id", messageId)
                .attribute("to", statusJid)
                .attribute("type", resolveStanzaType(container))
                .attribute("edit", resolveEditAttribute(container))
                .content(
                        participantsNode,
                        skmsgEncNode,
                        identityNode,
                        metaNode,
                        reportingNode
                );

        flushStore();
        var ackNode = client.sendNode(stanza);
        var ack = AckParser.parse(ackNode);

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "status send to {0} finished, success={1}", statusJid, ack.isSuccess());
        }

        if (ack.isSuccess()) {
            for (var device : skDistribDevices) {
                store.signalStore().markSenderKeyDistributed(statusJid, device);
            }
        }

        if (!isRevoke) {
            emitStatusPostEvent(messageInfo, statusAudience.size(), ack);
        }

        return ack;
    }

    /**
     * Resolves the status audience user JIDs from the user's status privacy preference.
     *
     * <p>{@link StatusPrivacyMode#CONTACTS} returns every stored contact;
     * {@link StatusPrivacyMode#WHITELIST} returns the configured allowlist;
     * {@link StatusPrivacyMode#CONTACTS_EXCEPT} returns every stored contact minus the configured
     * blocklist. An absent preference is treated as {@link StatusPrivacyMode#CONTACTS}. Mirrors WA
     * Web's {@code getStatusList}, whose audience drives the status fanout instead of any group
     * roster.
     *
     * @return the resolved audience user JIDs
     */
    @WhatsAppWebExport(moduleName = "WAWebUserPrefsStatus", exports = "getStatusList",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private List<Jid> resolveStatusAudience() {
        var privacy = store.settingsStore().statusPrivacy().orElse(null);
        var mode = privacy == null
                ? StatusPrivacyMode.CONTACTS
                : privacy.mode().orElse(StatusPrivacyMode.CONTACTS);
        return switch (mode) {
            case WHITELIST -> List.copyOf(privacy.jids());
            case CONTACTS_EXCEPT -> {
                var excluded = Set.copyOf(privacy.jids());
                yield store.contactStore().contacts().stream()
                        .map(Contact::jid)
                        .filter(jid -> !excluded.contains(jid))
                        .distinct()
                        .toList();
            }
            case CONTACTS -> store.contactStore().contacts().stream()
                    .map(Contact::jid)
                    .distinct()
                    .toList();
        };
    }

    /**
     * Returns the {@code status_setting} meta attribute value derived from the
     * user's status privacy preference.
     *
     * <p>The mapping mirrors WA Web's enum-to-string table: {@code Contact} to
     * {@code "contacts"}, {@code AllowList} to {@code "allowlist"},
     * {@code DenyList} to {@code "denylist"}.
     *
     * @implNote
     * This implementation returns {@code null} (rather than throwing) on unknown
     * preference values, departing from WA Web on the exhaustive-match branch;
     * the caller treats a {@code null} the same as an absent preference and
     * drops the attribute.
     *
     * @return the {@code status_setting} value, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebEncryptAndSendStatusMsg", exports = "encryptAndSendStatusMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private String resolveStatusSetting() {
        var mode = store.settingsStore().statusPrivacy()
                .flatMap(StatusPrivacySetting::mode)
                .orElse(null);
        if (mode == null) {
            return null;
        }
        return switch (mode) {
            case CONTACTS -> "contacts";
            case WHITELIST -> "allowlist";
            case CONTACTS_EXCEPT -> "denylist";
            default -> null;
        };
    }

    /**
     * Resolves the target device list and dispatch path for the given outgoing
     * container.
     *
     * <p>Non-revokes return the current audience unchanged on the SKMSG path.
     * Revokes attempt to narrow the audience to the original recipients recorded
     * against the revoked message; if at least one original recipient is no
     * longer part of the current audience, the union is returned with the
     * direct-path flag set so the caller dispatches via
     * {@link #sendDirectRevoke(Jid, ChatMessageInfo, Collection)}.
     *
     * @param container       the outbound {@link LinkedMessageContainer}
     * @param currentAudience the resolved status audience fanout
     * @return the {@link RevokeResolution} describing the dispatch decision
     */
    @WhatsAppWebExport(moduleName = "WAWebEncryptAndSendStatusMsg", exports = "encryptAndSendStatusMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    private RevokeResolution resolveRevokeDevices(
            LinkedMessageContainer container,
            Collection<Jid> currentAudience
    ) {
        if (!(container.content() instanceof ProtocolMessage pm)
            || pm.type().orElse(null) != ProtocolMessage.Type.REVOKE) {
            return new RevokeResolution(false, currentAudience);
        }

        var originalKey = pm.key().orElse(null);
        if (originalKey == null) {
            return new RevokeResolution(false, currentAudience);
        }

        var originalRecipients = originalKey.id()
                .map(store.chatStore()::findReceiptRecords)
                .orElse(Set.of());
        if (originalRecipients.isEmpty()) {
            return new RevokeResolution(false, currentAudience);
        }

        var currentUserJids = new HashSet<String>(currentAudience.size());
        for (var device : currentAudience) {
            currentUserJids.add(device.toUserJid().toString());
        }

        var selfUserJid = store.accountStore().jid().map(j -> j.toUserJid().toString()).orElse(null);
        var hasOutOfAudience = false;

        for (var recipient : originalRecipients) {
            var recipientStr = recipient.toUserJid().toString();
            if (!recipientStr.equals(selfUserJid) && !currentUserJids.contains(recipientStr)) {
                hasOutOfAudience = true;
                break;
            }
        }

        if (hasOutOfAudience) {
            var merged = new LinkedHashSet<>(currentAudience);
            merged.addAll(originalRecipients);
            return new RevokeResolution(true, merged);
        }

        var originalUserJids = new HashSet<String>(originalRecipients.size());
        for (var recipient : originalRecipients) {
            originalUserJids.add(recipient.toUserJid().toString());
        }
        var narrowed = currentAudience.stream()
                .filter(d -> originalUserJids.contains(d.toUserJid().toString()))
                .toList();
        return new RevokeResolution(false, narrowed.isEmpty() ? currentAudience : narrowed);
    }

    /**
     * Carries the outcome of resolving the device list for a status revoke.
     *
     * <p>Internal carrier for {@link #resolveRevokeDevices}; the {@code useDirect}
     * flag selects the GROUP_DIRECT dispatch path on the caller side.
     *
     * @param useDirect {@code true} when the revoke must use the per-device
     *                  direct path
     * @param devices   the target device list
     */
    private record RevokeResolution(boolean useDirect, Collection<Jid> devices) {

    }

    /**
     * Dispatches a status revoke via the per-device fanout (GROUP_DIRECT)
     * branch.
     *
     * <p>Taken when {@link #resolveRevokeDevices} detects that at least one
     * original recipient is no longer covered by the current status audience;
     * the stanza is built via {@link ChatFanoutStanza#build} with a per-device
     * payload list rather than a sender-key payload.
     *
     * @param statusJid   the status broadcast {@link Jid}
     * @param messageInfo the outgoing revoke {@link ChatMessageInfo}
     * @param allDevices  the union of original recipients and current audience
     * @return the parsed server {@link AckResult}
     * @throws WhatsAppMessageException.Send.Unknown if encryption fails for every
     *                                               device
     */
    @WhatsAppWebExport(moduleName = "WAWebEncryptAndSendStatusMsg", exports = "encryptAndSendStatusDirectMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    private AckResult sendDirectRevoke(
            Jid statusJid,
            ChatMessageInfo messageInfo,
            Collection<Jid> allDevices
    ) {
        var container = messageInfo.message();

        var depletedPrekeyCount = deviceService.ensureSessions(allDevices);
        emitPrekeysDepletionEvents(depletedPrekeyCount, MessageType.GROUP, allDevices.size());
        var senderIcdc = deviceService.computeIcdc(requireSelfJid()).orElse(null);
        var payloads = encryptForDevices(encryption, allDevices, container, statusJid, senderIcdc, null);
        if (payloads.isEmpty()) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "status direct revoke encryption failed for all devices targeting {0}", statusJid);
            }
            throw new WhatsAppMessageException.Send.Unknown(
                    "Encryption failed for all devices in direct status revoke to " + statusJid, null);
        }

        var identityNode = ParticipantsStanza.requiresIdentityNode(payloads)
                ? buildIdentityNode() : null;

        var stanza = ChatFanoutStanza.build(
                messageInfo.key().id().orElseThrow(),
                statusJid,
                resolveStanzaType(container),
                payloads,
                resolveEditAttribute(container),
                null,
                null,
                resolveMediaType(container),
                resolveDecryptFail(container),
                resolveNativeFlowName(container),
                null,
                false,
                null,
                null,
                null,
                null,
                identityNode,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        flushStore();
        var ackNode = client.sendNode(stanza);
        var ack = AckParser.parse(ackNode);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "status direct revoke to {0} finished, success={1}", statusJid, ack.isSuccess());
        }
        return ack;
    }

    /**
     * Commits one {@link com.github.auties00.cobalt.wire.wam.event.StatusPostEvent}
     * recording the outcome and composition of a completed status post.
     *
     * <p>Invoked once the SKMSG status stanza has been dispatched and acked, for
     * genuine status posts only; status revokes are excluded because they carry
     * their own telemetry. The {@code statusPostResult} is folded from the server
     * {@link AckResult} ({@link StatusPostResult#OK} on acceptance, otherwise
     * {@link StatusPostResult#ERROR_UNKNOWN}); {@code statusCategory} is fixed to
     * {@link StatusCategory#REGULAR_STATUS} because this sender only publishes to
     * {@code status@broadcast}; {@code mediaType}, {@code hasCaption},
     * {@code defaultStatusPrivacySetting}, {@code statusAudienceSize}, and
     * {@code statusId} are read from the outbound container, the store privacy
     * preference, and the message key. The media-editing flags
     * ({@code hasFilters}, {@code isCropped}, {@code isRotated}, the video-trim
     * flags), the audience-selector flags, {@code statusContainsMusic}, and
     * {@code isReshare} are reported {@code false} because this transport performs
     * no client-side media editing or selector interaction, and {@code retryCount}
     * is {@code 0} because {@link #doSend(Jid, ChatMessageInfo)} makes a single
     * dispatch attempt.
     *
     * @param messageInfo  the dispatched status {@link ChatMessageInfo}
     * @param audienceSize the number of audience user JIDs resolved from the
     *                     status privacy preference
     * @param ack          the parsed server {@link AckResult} for the send
     */
    @WhatsAppWebExport(moduleName = "WAWebLogStatusPost", exports = "logStatusPost",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitStatusPostEvent(ChatMessageInfo messageInfo, int audienceSize, AckResult ack) {
        var container = messageInfo.message();
        wamService.commit(new StatusPostEventBuilder()
                .statusPostResult(ack.isSuccess() ? StatusPostResult.OK : StatusPostResult.ERROR_UNKNOWN)
                .statusPostOrigin(StatusPostOrigin.STATUS_TAB)
                .statusCategory(StatusCategory.REGULAR_STATUS)
                .mediaType(resolveWamMediaType(container))
                .defaultStatusPrivacySetting(resolveDefaultStatusPrivacySetting())
                .statusAudienceSize(audienceSize)
                .statusId(messageInfo.key().id().orElse(null))
                .hasCaption(resolveHasCaption(container))
                .hasFilters(false)
                .isCropped(false)
                .isReshare(false)
                .isRotated(false)
                .isVideoManuallyTrimmed(false)
                .isVideoMuted(false)
                .isVideoTrimmed(false)
                .statusContainsMusic(false)
                .statusAudienceSelectorClicked(false)
                .statusAudienceSelectorUpdated(false)
                .retryCount(0)
                .build());
    }

    /**
     * Maps the outbound status container to the WAM {@link MediaType} slot.
     *
     * <p>Classifies by the top-level content type: images map to
     * {@link MediaType#PHOTO}, videos to {@link MediaType#VIDEO}, audio to
     * {@link MediaType#AUDIO}, documents to {@link MediaType#DOCUMENT}, stickers
     * to {@link MediaType#STICKER}, and every other content (text status
     * included) to {@link MediaType#NONE}.
     *
     * @param container the outbound status {@link LinkedMessageContainer}
     * @return the matching {@link MediaType}; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebLogStatusPost", exports = "getStatusMediaType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static MediaType resolveWamMediaType(LinkedMessageContainer container) {
        return switch (container.content()) {
            case ImageMessage _ -> MediaType.PHOTO;
            case VideoMessage _ -> MediaType.VIDEO;
            case AudioMessage _ -> MediaType.AUDIO;
            case DocumentMessage _ -> MediaType.DOCUMENT;
            case StickerMessage _ -> MediaType.STICKER;
            default -> MediaType.NONE;
        };
    }

    /**
     * Returns the {@code defaultStatusPrivacySetting} value derived from the
     * user's stored status privacy preference.
     *
     * <p>{@link StatusPrivacyMode#CONTACTS} maps to
     * {@link PrivacySettingsValueType#MY_CONTACTS},
     * {@link StatusPrivacyMode#WHITELIST} to
     * {@link PrivacySettingsValueType#ONLY_SHARE_WITH}, and
     * {@link StatusPrivacyMode#CONTACTS_EXCEPT} to
     * {@link PrivacySettingsValueType#MY_CONTACTS_EXCEPT}. An absent preference is
     * treated as {@link StatusPrivacyMode#CONTACTS}, matching
     * {@link #resolveStatusAudience()}.
     *
     * @return the mapped {@link PrivacySettingsValueType}; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebLogStatusPost", exports = "logStatusPost",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private PrivacySettingsValueType resolveDefaultStatusPrivacySetting() {
        var mode = store.settingsStore().statusPrivacy()
                .flatMap(StatusPrivacySetting::mode)
                .orElse(StatusPrivacyMode.CONTACTS);
        return switch (mode) {
            case CONTACTS -> PrivacySettingsValueType.MY_CONTACTS;
            case WHITELIST -> PrivacySettingsValueType.ONLY_SHARE_WITH;
            case CONTACTS_EXCEPT -> PrivacySettingsValueType.MY_CONTACTS_EXCEPT;
        };
    }

    /**
     * Returns whether the outbound status media carries a caption.
     *
     * <p>Only captioned media content ({@link ImageMessage},
     * {@link VideoMessage}, {@link DocumentMessage}) can supply a caption; every
     * other content type reports {@code false}.
     *
     * @param container the outbound status {@link LinkedMessageContainer}
     * @return {@code true} when the content carries a non-empty caption
     */
    private static boolean resolveHasCaption(LinkedMessageContainer container) {
        return switch (container.content()) {
            case ImageMessage image -> image.caption().isPresent();
            case VideoMessage video -> video.caption().isPresent();
            case DocumentMessage document -> document.caption().isPresent();
            default -> false;
        };
    }

    /**
     * Commits one
     * {@link com.github.auties00.cobalt.wire.wam.event.PrekeysDepletionEvent} per
     * depleted one-time pre-key reported by the last
     * {@link DeviceService#ensureSessions(Collection)} call.
     *
     * <p>No-op when {@code depletedPrekeyCount} is not positive. The audience
     * size drives the {@code deviceSizeBucket} slot.
     *
     * @param depletedPrekeyCount the number of depleted one-time pre-keys
     * @param messageType         the WAM {@link MessageType} for this send
     * @param deviceCount         the device count used for the
     *                            {@code deviceSizeBucket} classification, or
     *                            {@code null} to omit the bucket
     */
    @WhatsAppWebExport(moduleName = "WAWebPostPrekeysDepletionMetric",
            exports = "maybePostPrekeysDepletionMetric",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitPrekeysDepletionEvents(int depletedPrekeyCount, MessageType messageType, Integer deviceCount) {
        if (depletedPrekeyCount <= 0) {
            return;
        }
        var bucket = deviceCount == null ? null : numberToSizeBucket(deviceCount);
        for (var i = 0; i < depletedPrekeyCount; i++) {
            wamService.commit(new PrekeysDepletionEventBuilder()
                    .prekeysFetchReason(PrekeysFetchContext.SEND_MESSAGE)
                    .messageType(messageType)
                    .deviceSizeBucket(bucket)
                    .build());
        }
    }

    /**
     * Maps a fanout device count to the matching {@link SizeBucket} carried by
     * the {@code deviceSizeBucket} WAM property.
     *
     * <p>Buckets are exclusive upper bounds: {@code count=31} returns
     * {@link SizeBucket#LT32}, {@code count=1024} returns
     * {@link SizeBucket#LT1500}, and any {@code count >= 5000} returns
     * {@link SizeBucket#LARGEST_BUCKET}.
     *
     * @param count the device count to classify
     * @return the matching {@link SizeBucket}; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamNumberToSizeBucket",
            exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static SizeBucket numberToSizeBucket(int count) {
        if (count < 32) return SizeBucket.LT32;
        if (count < 64) return SizeBucket.LT64;
        if (count < 128) return SizeBucket.LT128;
        if (count < 256) return SizeBucket.LT256;
        if (count < 512) return SizeBucket.LT512;
        if (count < 1024) return SizeBucket.LT1024;
        if (count < 1500) return SizeBucket.LT1500;
        if (count < 2000) return SizeBucket.LT2000;
        if (count < 2500) return SizeBucket.LT2500;
        if (count < 3000) return SizeBucket.LT3000;
        if (count < 3500) return SizeBucket.LT3500;
        if (count < 4000) return SizeBucket.LT4000;
        if (count < 4500) return SizeBucket.LT4500;
        if (count < 5000) return SizeBucket.LT5000;
        return SizeBucket.LARGEST_BUCKET;
    }
}
