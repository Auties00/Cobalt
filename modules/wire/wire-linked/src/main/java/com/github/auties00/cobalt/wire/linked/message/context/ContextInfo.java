package com.github.auties00.cobalt.wire.linked.message.context;

import com.github.auties00.cobalt.wire.linked.bot.session.BotMessageSharingInfo;
import com.github.auties00.cobalt.wire.linked.bot.session.ForwardedAIBotMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatDisappearingMode;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupParticipantLabel;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMention;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveAction;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveActionLink;
import com.github.auties00.cobalt.wire.linked.message.interactive.UrlTrackingMap;
import com.github.auties00.cobalt.wire.linked.message.status.StatusAttribution;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.*;

/**
 * Carries the contextual metadata attached to a message that supports it.
 *
 * <p>A {@code ContextInfo} aggregates every piece of information that is not
 * part of the message content itself but still needs to travel with it. This
 * includes, among others, the reference to a quoted message (for replies),
 * the list of mentioned users, forwarding counters, ad attribution data
 * (Click-To-WhatsApp and external ad replies), disappearing message settings,
 * status reshare metadata, newsletter and business forwarding details,
 * trust banners, UTM parameters and AI bot sharing data.
 *
 * <p>All fields are optional: a {@code ContextInfo} instance may carry only a
 * small subset of them depending on the message that produced it. Accessors
 * returning {@code Optional} or {@code OptionalInt} signal nullable single
 * values, while collection accessors always return an unmodifiable view that
 * is never {@code null}.
 *
 * <p>Nested types group logically related attributes: {@link AdReplyInfo} and
 * {@link ExternalAdReplyInfo} model replies coming from ads,
 * {@link ForwardedNewsletterMessageInfo} and {@link BusinessMessageForwardInfo}
 * carry forwarding provenance, {@link FeatureEligibilities} declares which
 * interactive features are available on the message, and
 * {@link DataSharingContext}, {@link UTMInfo}, {@link QuestionReplyQuotedMessage}
 * and {@link StatusAudienceMetadata} expose specialised contextual data.
 */
@ProtobufMessage(name = "ContextInfo")
public final class ContextInfo {
    /**
     * The identifier of the quoted message, when this message is a reply.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String quotedMessageId;

    /**
     * The JID of the user that originally sent the quoted message.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    Jid quotedMessageSenderJid;

    /**
     * The content of the quoted message, typically a trimmed or preview
     * version used to render the reply bubble.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    LinkedMessageContainer quotedMessageContent;

    /**
     * The JID of the parent group when the quoted message was sent inside a
     * community sub-group.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    Jid quotedMessageParentJid;

    /**
     * The JIDs of the individual users mentioned in this message using the
     * {@code @user} syntax.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.STRING)
    List<Jid> mentionedJids;

    /**
     * The source that triggered a Click-To-WhatsApp (CTWA) conversion leading
     * to this message.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.STRING)
    String conversionSource;

    /**
     * Opaque data attached to a Click-To-WhatsApp conversion for attribution.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.BYTES)
    byte[] conversionData;

    /**
     * Number of seconds elapsed between the conversion event and this
     * message being sent.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.UINT32)
    Integer conversionDelaySeconds;

    /**
     * Number of times this message has been forwarded. A value of zero or
     * {@code null} indicates that the message has not been forwarded yet.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.UINT32)
    Integer forwardingScore;

    /**
     * Whether this message has been forwarded at least once.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.BOOL)
    Boolean isForwarded;

    /**
     * Metadata describing a native ad that the user replied to in order to
     * produce this message.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.MESSAGE)
    AdReplyInfo quotedAdReply;

    /**
     * Placeholder key used when the real quoted message is not yet available
     * to the client.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.MESSAGE)
    MessageKey placeholderKey;

    /**
     * Duration, in seconds, after which a disappearing message is deleted
     * from the chat.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.UINT32)
    Integer ephemeralDuration;

    /**
     * Timestamp of the moment the disappearing setting that applies to this
     * message was last changed.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant ephemeralSettingTimestamp;

    /**
     * Shared secret used to coordinate the ephemeral settings between
     * participants of the conversation.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.BYTES)
    byte[] ephemeralSharedSecret;

    /**
     * Metadata describing an external (off-WhatsApp) ad that led to this
     * message.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.MESSAGE)
    ExternalAdReplyInfo externalAdReply;

    /**
     * Entry point identifier for the specific conversion source that
     * originated this message.
     */
    @ProtobufProperty(index = 29, type = ProtobufType.STRING)
    String entryPointConversionSource;

    /**
     * Identifier of the application that generated the conversion entry
     * point.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.STRING)
    String entryPointConversionApp;

    /**
     * Number of seconds between the entry point conversion event and the
     * message being sent.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.UINT32)
    Integer entryPointConversionDelaySeconds;

    /**
     * Disappearing messages configuration in effect when this message was
     * delivered.
     */
    @ProtobufProperty(index = 32, type = ProtobufType.MESSAGE)
    ChatDisappearingMode disappearingMode;

    /**
     * Deep link that can be used to open a related interactive action from
     * this message.
     */
    @ProtobufProperty(index = 33, type = ProtobufType.MESSAGE)
    InteractiveActionLink actionLink;

    /**
     * Subject (display name) of the group owning the quoted message.
     */
    @ProtobufProperty(index = 34, type = ProtobufType.STRING)
    String quotedGroupSubject;

    /**
     * JID of the parent community when the quoted message lives inside a
     * sub-group of a community.
     */
    @ProtobufProperty(index = 35, type = ProtobufType.STRING)
    Jid quotedParentGroupJid;

    /**
     * Identifier of the trust banner shown to the recipient (for example the
     * safety notice displayed for messages originated from ads).
     */
    @ProtobufProperty(index = 37, type = ProtobufType.STRING)
    String trustBannerType;

    /**
     * Numeric identifier of the action bound to the trust banner.
     */
    @ProtobufProperty(index = 38, type = ProtobufType.UINT32)
    Integer trustBannerAction;

    /**
     * Whether this message has been selected for telemetry sampling.
     */
    @ProtobufProperty(index = 39, type = ProtobufType.BOOL)
    Boolean isSampled;

    /**
     * Mentions targeting whole groups (for example the {@code @everyone}
     * mention) rather than individual users.
     */
    @ProtobufProperty(index = 40, type = ProtobufType.MESSAGE)
    List<GroupMention> groupMentions;

    /**
     * UTM attribution parameters propagated from a marketing campaign.
     */
    @ProtobufProperty(index = 41, type = ProtobufType.MESSAGE)
    UTMInfo utm;

    /**
     * Provenance information when this message is a forward of a newsletter
     * post.
     */
    @ProtobufProperty(index = 43, type = ProtobufType.MESSAGE)
    ForwardedNewsletterMessageInfo forwardedNewsletterMessageInfo;

    /**
     * Provenance information when this message is a forward originated from
     * a business conversation.
     */
    @ProtobufProperty(index = 44, type = ProtobufType.MESSAGE)
    BusinessMessageForwardInfo businessMessageForwardInfo;

    /**
     * Campaign identifier assigned by the small-business client that
     * originated the message.
     */
    @ProtobufProperty(index = 45, type = ProtobufType.STRING)
    String smbClientCampaignId;

    /**
     * Campaign identifier assigned by the server to a small-business
     * conversation.
     */
    @ProtobufProperty(index = 46, type = ProtobufType.STRING)
    String smbServerCampaignId;

    /**
     * Data sharing consent and parameters carried with this message when it
     * is part of a business data collection flow.
     */
    @ProtobufProperty(index = 47, type = ProtobufType.MESSAGE)
    DataSharingContext dataSharingContext;

    /**
     * Whether the ad attribution banner must always be shown regardless of
     * other heuristics.
     */
    @ProtobufProperty(index = 48, type = ProtobufType.BOOL)
    Boolean alwaysShowAdAttribution;

    /**
     * Flags describing which features the message is eligible for (for
     * example reactions or resharing).
     */
    @ProtobufProperty(index = 49, type = ProtobufType.MESSAGE)
    FeatureEligibilities featureEligibilities;

    /**
     * External source identifier propagated from the entry point conversion
     * flow.
     */
    @ProtobufProperty(index = 50, type = ProtobufType.STRING)
    String entryPointConversionExternalSource;

    /**
     * External medium identifier propagated from the entry point conversion
     * flow.
     */
    @ProtobufProperty(index = 51, type = ProtobufType.STRING)
    String entryPointConversionExternalMedium;

    /**
     * Signals carried from Click-To-WhatsApp ads for attribution purposes.
     */
    @ProtobufProperty(index = 54, type = ProtobufType.STRING)
    String ctwaSignals;

    /**
     * Binary payload attached to a Click-To-WhatsApp ad conversion.
     */
    @ProtobufProperty(index = 55, type = ProtobufType.BYTES)
    byte[] ctwaPayload;

    /**
     * Provenance information when this message is a forward of an AI bot
     * response.
     */
    @ProtobufProperty(index = 56, type = ProtobufType.MESSAGE)
    ForwardedAIBotMessageInfo forwardedAiBotMessageInfo;

    /**
     * Reason why a status update is attributed to another user (for example
     * a reshare from a mention or from a previous post).
     */
    @ProtobufProperty(index = 57, type = ProtobufType.ENUM)
    StatusAttributionType statusAttributionType;

    /**
     * Map of trackable URLs present in the message, used to expand link
     * previews and propagate click analytics.
     */
    @ProtobufProperty(index = 58, type = ProtobufType.MESSAGE)
    UrlTrackingMap urlTrackingMap;

    /**
     * Paired media relationship, used to link low-resolution and
     * high-resolution copies of the same media asset.
     */
    @ProtobufProperty(index = 59, type = ProtobufType.ENUM)
    PairedMediaType pairedMediaType;

    /**
     * Version of the server-side ranking algorithm used to score this
     * message.
     */
    @ProtobufProperty(index = 60, type = ProtobufType.UINT32)
    Integer rankingVersion;

    /**
     * Label attached to the sender when the message is visible in a group
     * context (for example an admin badge).
     */
    @ProtobufProperty(index = 62, type = ProtobufType.MESSAGE)
    GroupParticipantLabel memberLabel;

    /**
     * Whether this message is a question that expects a reply, for use in
     * question-and-answer flows.
     */
    @ProtobufProperty(index = 63, type = ProtobufType.BOOL)
    Boolean isQuestion;

    /**
     * Type of the source media when this message is attached to a status
     * update (image, video, audio, text and so on).
     */
    @ProtobufProperty(index = 64, type = ProtobufType.ENUM)
    StatusSourceType statusSourceType;

    /**
     * Attributions declared for this status update, listing the users that
     * the status is shared with or attributed to.
     */
    @ProtobufProperty(index = 65, type = ProtobufType.MESSAGE)
    List<StatusAttribution> statusAttributions;

    /**
     * Whether this status update targets a group audience rather than the
     * sender's contacts.
     */
    @ProtobufProperty(index = 66, type = ProtobufType.BOOL)
    Boolean isGroupStatus;

    /**
     * Origin from which this message was forwarded (regular chat, status,
     * channel, Meta AI and so on).
     */
    @ProtobufProperty(index = 67, type = ProtobufType.ENUM)
    ForwardOrigin forwardOrigin;

    /**
     * When this message is a reply to a question, bundles both the original
     * question and its response for inline rendering.
     */
    @ProtobufProperty(index = 68, type = ProtobufType.MESSAGE)
    QuestionReplyQuotedMessage questionReplyQuotedMessage;

    /**
     * Audience configuration describing who is allowed to see this status
     * update (for example close friends).
     */
    @ProtobufProperty(index = 69, type = ProtobufType.MESSAGE)
    StatusAudienceMetadata statusAudienceMetadata;

    /**
     * Count of mentions that do not correspond to a known JID (for example
     * phone numbers typed as plain text).
     */
    @ProtobufProperty(index = 70, type = ProtobufType.UINT32)
    Integer nonJidMentions;

    /**
     * Whether the quoted message was selected explicitly by the user or
     * attached automatically by the client.
     */
    @ProtobufProperty(index = 71, type = ProtobufType.ENUM)
    QuotedType quotedType;

    /**
     * Context attached to messages shared from an AI bot session.
     */
    @ProtobufProperty(index = 72, type = ProtobufType.MESSAGE)
    BotMessageSharingInfo botMessageSharingInfo;


    /**
     * Constructs a fully populated {@code ContextInfo} instance.
     *
     * <p>This constructor is package-private and is intended to be invoked by
     * the generated protobuf deserializer or the matching
     * {@code ContextInfoBuilder}. Callers should obtain a builder instead of
     * invoking this constructor directly.
     *
     * @param quotedMessageId                      see {@link #quotedMessageId()}
     * @param quotedMessageSenderJid               see {@link #quotedMessageSenderJid()}
     * @param quotedMessageContent                 see {@link #quotedMessageContent()}
     * @param quotedMessageParentJid               see {@link #quotedMessageParentJid()}
     * @param mentionedJids                        see {@link #mentionedJids()}
     * @param conversionSource                     see {@link #conversionSource()}
     * @param conversionData                       see {@link #conversionData()}
     * @param conversionDelaySeconds               see {@link #conversionDelaySeconds()}
     * @param forwardingScore                      see {@link #forwardingScore()}
     * @param isForwarded                          see {@link #isForwarded()}
     * @param quotedAdReply                        see {@link #quotedAdReply()}
     * @param placeholderKey                       see {@link #placeholderKey()}
     * @param ephemeralDuration                    see {@link #ephemeralDuration()}
     * @param ephemeralSettingTimestamp            see {@link #ephemeralSettingTimestamp()}
     * @param ephemeralSharedSecret                see {@link #ephemeralSharedSecret()}
     * @param externalAdReply                      see {@link #externalAdReply()}
     * @param entryPointConversionSource           see {@link #entryPointConversionSource()}
     * @param entryPointConversionApp              see {@link #entryPointConversionApp()}
     * @param entryPointConversionDelaySeconds     see {@link #entryPointConversionDelaySeconds()}
     * @param disappearingMode                     see {@link #disappearingMode()}
     * @param actionLink                           see {@link #actionLink()}
     * @param quotedGroupSubject                   see {@link #quotedGroupSubject()}
     * @param quotedParentGroupJid                 see {@link #quotedParentGroupJid()}
     * @param trustBannerType                      see {@link #trustBannerType()}
     * @param trustBannerAction                    see {@link #trustBannerAction()}
     * @param isSampled                            see {@link #isSampled()}
     * @param groupMentions                        see {@link #groupMentions()}
     * @param utm                                  see {@link #utm()}
     * @param forwardedNewsletterMessageInfo       see {@link #forwardedNewsletterMessageInfo()}
     * @param businessMessageForwardInfo           see {@link #businessMessageForwardInfo()}
     * @param smbClientCampaignId                  see {@link #smbClientCampaignId()}
     * @param smbServerCampaignId                  see {@link #smbServerCampaignId()}
     * @param dataSharingContext                   see {@link #dataSharingContext()}
     * @param alwaysShowAdAttribution              see {@link #alwaysShowAdAttribution()}
     * @param featureEligibilities                 see {@link #featureEligibilities()}
     * @param entryPointConversionExternalSource   see {@link #entryPointConversionExternalSource()}
     * @param entryPointConversionExternalMedium   see {@link #entryPointConversionExternalMedium()}
     * @param ctwaSignals                          see {@link #ctwaSignals()}
     * @param ctwaPayload                          see {@link #ctwaPayload()}
     * @param forwardedAiBotMessageInfo            see {@link #forwardedAiBotMessageInfo()}
     * @param statusAttributionType                see {@link #statusAttributionType()}
     * @param urlTrackingMap                       see {@link #urlTrackingMap()}
     * @param pairedMediaType                      see {@link #pairedMediaType()}
     * @param rankingVersion                       see {@link #rankingVersion()}
     * @param memberLabel                          see {@link #memberLabel()}
     * @param isQuestion                           see {@link #isQuestion()}
     * @param statusSourceType                     see {@link #statusSourceType()}
     * @param statusAttributions                   see {@link #statusAttributions()}
     * @param isGroupStatus                        see {@link #isGroupStatus()}
     * @param forwardOrigin                        see {@link #forwardOrigin()}
     * @param questionReplyQuotedMessage           see {@link #questionReplyQuotedMessage()}
     * @param statusAudienceMetadata               see {@link #statusAudienceMetadata()}
     * @param nonJidMentions                       see {@link #nonJidMentions()}
     * @param quotedType                           see {@link #quotedType()}
     * @param botMessageSharingInfo                see {@link #botMessageSharingInfo()}
     */
    ContextInfo(String quotedMessageId, Jid quotedMessageSenderJid, LinkedMessageContainer quotedMessageContent, Jid quotedMessageParentJid, List<Jid> mentionedJids, String conversionSource, byte[] conversionData, Integer conversionDelaySeconds, Integer forwardingScore, Boolean isForwarded, AdReplyInfo quotedAdReply, MessageKey placeholderKey, Integer ephemeralDuration, Instant ephemeralSettingTimestamp, byte[] ephemeralSharedSecret, ExternalAdReplyInfo externalAdReply, String entryPointConversionSource, String entryPointConversionApp, Integer entryPointConversionDelaySeconds, ChatDisappearingMode disappearingMode, InteractiveActionLink actionLink, String quotedGroupSubject, Jid quotedParentGroupJid, String trustBannerType, Integer trustBannerAction, Boolean isSampled, List<GroupMention> groupMentions, UTMInfo utm, ForwardedNewsletterMessageInfo forwardedNewsletterMessageInfo, BusinessMessageForwardInfo businessMessageForwardInfo, String smbClientCampaignId, String smbServerCampaignId, DataSharingContext dataSharingContext, Boolean alwaysShowAdAttribution, FeatureEligibilities featureEligibilities, String entryPointConversionExternalSource, String entryPointConversionExternalMedium, String ctwaSignals, byte[] ctwaPayload, ForwardedAIBotMessageInfo forwardedAiBotMessageInfo, StatusAttributionType statusAttributionType, UrlTrackingMap urlTrackingMap, PairedMediaType pairedMediaType, Integer rankingVersion, GroupParticipantLabel memberLabel, Boolean isQuestion, StatusSourceType statusSourceType, List<StatusAttribution> statusAttributions, Boolean isGroupStatus, ForwardOrigin forwardOrigin, QuestionReplyQuotedMessage questionReplyQuotedMessage, StatusAudienceMetadata statusAudienceMetadata, Integer nonJidMentions, QuotedType quotedType, BotMessageSharingInfo botMessageSharingInfo) {
        this.quotedMessageId = quotedMessageId;
        this.quotedMessageSenderJid = quotedMessageSenderJid;
        this.quotedMessageContent = quotedMessageContent;
        this.quotedMessageParentJid = quotedMessageParentJid;
        this.mentionedJids = mentionedJids;
        this.conversionSource = conversionSource;
        this.conversionData = conversionData;
        this.conversionDelaySeconds = conversionDelaySeconds;
        this.forwardingScore = forwardingScore;
        this.isForwarded = isForwarded;
        this.quotedAdReply = quotedAdReply;
        this.placeholderKey = placeholderKey;
        this.ephemeralDuration = ephemeralDuration;
        this.ephemeralSettingTimestamp = ephemeralSettingTimestamp;
        this.ephemeralSharedSecret = ephemeralSharedSecret;
        this.externalAdReply = externalAdReply;
        this.entryPointConversionSource = entryPointConversionSource;
        this.entryPointConversionApp = entryPointConversionApp;
        this.entryPointConversionDelaySeconds = entryPointConversionDelaySeconds;
        this.disappearingMode = disappearingMode;
        this.actionLink = actionLink;
        this.quotedGroupSubject = quotedGroupSubject;
        this.quotedParentGroupJid = quotedParentGroupJid;
        this.trustBannerType = trustBannerType;
        this.trustBannerAction = trustBannerAction;
        this.isSampled = isSampled;
        this.groupMentions = groupMentions;
        this.utm = utm;
        this.forwardedNewsletterMessageInfo = forwardedNewsletterMessageInfo;
        this.businessMessageForwardInfo = businessMessageForwardInfo;
        this.smbClientCampaignId = smbClientCampaignId;
        this.smbServerCampaignId = smbServerCampaignId;
        this.dataSharingContext = dataSharingContext;
        this.alwaysShowAdAttribution = alwaysShowAdAttribution;
        this.featureEligibilities = featureEligibilities;
        this.entryPointConversionExternalSource = entryPointConversionExternalSource;
        this.entryPointConversionExternalMedium = entryPointConversionExternalMedium;
        this.ctwaSignals = ctwaSignals;
        this.ctwaPayload = ctwaPayload;
        this.forwardedAiBotMessageInfo = forwardedAiBotMessageInfo;
        this.statusAttributionType = statusAttributionType;
        this.urlTrackingMap = urlTrackingMap;
        this.pairedMediaType = pairedMediaType;
        this.rankingVersion = rankingVersion;
        this.memberLabel = memberLabel;
        this.isQuestion = isQuestion;
        this.statusSourceType = statusSourceType;
        this.statusAttributions = statusAttributions;
        this.isGroupStatus = isGroupStatus;
        this.forwardOrigin = forwardOrigin;
        this.questionReplyQuotedMessage = questionReplyQuotedMessage;
        this.statusAudienceMetadata = statusAudienceMetadata;
        this.nonJidMentions = nonJidMentions;
        this.quotedType = quotedType;
        this.botMessageSharingInfo = botMessageSharingInfo;
    }

    /**
     * Returns the identifier of the quoted message when this message is a
     * reply.
     *
     * @return an {@code Optional} containing the quoted message id, or
     *         empty if the message is not a reply
     */
    public Optional<String> quotedMessageId() {
        return Optional.ofNullable(quotedMessageId);
    }

    /**
     * Returns the JID of the user that originally sent the quoted message.
     *
     * @return an {@code Optional} containing the sender JID, or empty if not
     *         set
     */
    public Optional<Jid> quotedMessageSenderJid() {
        return Optional.ofNullable(quotedMessageSenderJid);
    }

    /**
     * Returns the content preview of the quoted message used to render the
     * reply bubble.
     *
     * @return an {@code Optional} containing the quoted content, or empty if
     *         not set
     */
    public Optional<LinkedMessageContainer> quotedMessageContent() {
        return Optional.ofNullable(quotedMessageContent);
    }

    /**
     * Returns the JID of the parent community when the quoted message was
     * sent inside a community sub-group.
     *
     * @return an {@code Optional} containing the parent JID, or empty if
     *         not set
     */
    public Optional<Jid> quotedMessageParentJid() {
        return Optional.ofNullable(quotedMessageParentJid);
    }

    /**
     * Returns the JIDs of the users mentioned in this message with
     * {@code @user}.
     *
     * @return an unmodifiable list of mentioned JIDs, never {@code null}
     */
    public List<Jid> mentionedJids() {
        return mentionedJids == null ? List.of() : Collections.unmodifiableList(mentionedJids);
    }

    /**
     * Returns the Click-To-WhatsApp conversion source that led to this
     * message.
     *
     * @return an {@code Optional} containing the conversion source, or
     *         empty if not set
     */
    public Optional<String> conversionSource() {
        return Optional.ofNullable(conversionSource);
    }

    /**
     * Returns the opaque conversion payload associated with a
     * Click-To-WhatsApp attribution event.
     *
     * @return an {@code Optional} containing the raw conversion bytes, or
     *         empty if not set
     */
    public Optional<byte[]> conversionData() {
        return Optional.ofNullable(conversionData);
    }

    /**
     * Returns the delay, in seconds, between the Click-To-WhatsApp
     * conversion event and this message being sent.
     *
     * @return the delay wrapped in an {@code OptionalInt}, or empty if not
     *         set
     */
    public OptionalInt conversionDelaySeconds() {
        return conversionDelaySeconds == null ? OptionalInt.empty() : OptionalInt.of(conversionDelaySeconds);
    }

    /**
     * Returns how many times this message has been forwarded.
     *
     * <p>A value of zero or an empty result both indicate that the message
     * has not yet been forwarded.
     *
     * @return the forwarding score, or empty if not set
     */
    public OptionalInt forwardingScore() {
        return forwardingScore == null ? OptionalInt.empty() : OptionalInt.of(forwardingScore);
    }

    /**
     * Returns whether this message has been forwarded at least once.
     *
     * @return {@code true} if the forwarded flag is set, {@code false}
     *         otherwise
     */
    public boolean isForwarded() {
        return isForwarded != null && isForwarded;
    }

    /**
     * Returns the metadata describing the native ad that originated this
     * message when it is a reply to an ad.
     *
     * @return an {@code Optional} containing the ad reply info, or empty
     *         if not set
     */
    public Optional<AdReplyInfo> quotedAdReply() {
        return Optional.ofNullable(quotedAdReply);
    }

    /**
     * Returns the placeholder key used when the real quoted message is not
     * yet available to the client.
     *
     * @return an {@code Optional} containing the placeholder
     *         {@link MessageKey}, or empty if not set
     */
    public Optional<MessageKey> placeholderKey() {
        return Optional.ofNullable(placeholderKey);
    }

    /**
     * Returns the disappearing-message duration, in seconds, that applies to
     * this message.
     *
     * @return the duration in seconds, or empty if the conversation is not
     *         ephemeral
     */
    public OptionalInt ephemeralDuration() {
        return ephemeralDuration == null ? OptionalInt.empty() : OptionalInt.of(ephemeralDuration);
    }

    /**
     * Returns the instant at which the current ephemeral setting was last
     * changed.
     *
     * @return an {@code Optional} containing the timestamp, or empty if not
     *         set
     */
    public Optional<Instant> ephemeralSettingTimestamp() {
        return Optional.ofNullable(ephemeralSettingTimestamp);
    }

    /**
     * Returns the shared secret used to coordinate ephemeral settings
     * between participants of the conversation.
     *
     * @return an {@code Optional} containing the secret bytes, or empty if
     *         not set
     */
    public Optional<byte[]> ephemeralSharedSecret() {
        return Optional.ofNullable(ephemeralSharedSecret);
    }

    /**
     * Returns the metadata describing the external (off-WhatsApp) ad that
     * led to this message when applicable.
     *
     * @return an {@code Optional} containing the external ad reply info, or
     *         empty if not set
     */
    public Optional<ExternalAdReplyInfo> externalAdReply() {
        return Optional.ofNullable(externalAdReply);
    }

    /**
     * Returns the entry point source identifier that triggered the
     * conversion flow associated with this message.
     *
     * @return an {@code Optional} containing the entry point source, or
     *         empty if not set
     */
    public Optional<String> entryPointConversionSource() {
        return Optional.ofNullable(entryPointConversionSource);
    }

    /**
     * Returns the identifier of the application that generated the
     * conversion entry point.
     *
     * @return an {@code Optional} containing the app identifier, or empty
     *         if not set
     */
    public Optional<String> entryPointConversionApp() {
        return Optional.ofNullable(entryPointConversionApp);
    }

    /**
     * Returns the delay, in seconds, between the entry point conversion
     * event and this message being sent.
     *
     * @return the delay wrapped in an {@code OptionalInt}, or empty if not
     *         set
     */
    public OptionalInt entryPointConversionDelaySeconds() {
        return entryPointConversionDelaySeconds == null ? OptionalInt.empty() : OptionalInt.of(entryPointConversionDelaySeconds);
    }

    /**
     * Returns the disappearing-messages configuration in effect for this
     * conversation at the time the message was sent.
     *
     * @return an {@code Optional} containing the {@link ChatDisappearingMode},
     *         or empty if not set
     */
    public Optional<ChatDisappearingMode> disappearingMode() {
        return Optional.ofNullable(disappearingMode);
    }

    /**
     * Returns the deep link that can be used to open an interactive action
     * related to this message.
     *
     * @return an {@code Optional} containing the {@link InteractiveActionLink},
     *         or empty if not set
     */
    public Optional<InteractiveActionLink> actionLink() {
        return Optional.ofNullable(actionLink);
    }

    /**
     * Returns the display subject of the group that owns the quoted
     * message.
     *
     * @return an {@code Optional} containing the group subject, or empty if
     *         the quoted message is not from a group
     */
    public Optional<String> quotedGroupSubject() {
        return Optional.ofNullable(quotedGroupSubject);
    }

    /**
     * Returns the JID of the parent community of the group that owns the
     * quoted message.
     *
     * @return an {@code Optional} containing the parent community JID, or
     *         empty if not applicable
     */
    public Optional<Jid> quotedParentGroupJid() {
        return Optional.ofNullable(quotedParentGroupJid);
    }

    /**
     * Returns the identifier of the trust banner shown to the recipient.
     *
     * @return an {@code Optional} containing the banner identifier, or
     *         empty if no banner is required
     */
    public Optional<String> trustBannerType() {
        return Optional.ofNullable(trustBannerType);
    }

    /**
     * Returns the numeric identifier of the action bound to the trust
     * banner.
     *
     * @return the banner action code, or empty if not set
     */
    public OptionalInt trustBannerAction() {
        return trustBannerAction == null ? OptionalInt.empty() : OptionalInt.of(trustBannerAction);
    }

    /**
     * Returns whether this message has been selected for telemetry
     * sampling.
     *
     * @return {@code true} if the message is sampled, {@code false}
     *         otherwise
     */
    public boolean isSampled() {
        return isSampled != null && isSampled;
    }

    /**
     * Returns the list of mentions that target whole groups rather than
     * individual users.
     *
     * @return an unmodifiable list of {@link GroupMention} entries, never
     *         {@code null}
     */
    public List<GroupMention> groupMentions() {
        return groupMentions == null ? List.of() : Collections.unmodifiableList(groupMentions);
    }

    /**
     * Returns the UTM attribution parameters propagated from a marketing
     * campaign.
     *
     * @return an {@code Optional} containing the {@link UTMInfo}, or empty
     *         if not set
     */
    public Optional<UTMInfo> utm() {
        return Optional.ofNullable(utm);
    }

    /**
     * Returns the provenance information of a newsletter post that was
     * forwarded into this message.
     *
     * @return an {@code Optional} containing the newsletter forward info,
     *         or empty if the message is not a newsletter forward
     */
    public Optional<ForwardedNewsletterMessageInfo> forwardedNewsletterMessageInfo() {
        return Optional.ofNullable(forwardedNewsletterMessageInfo);
    }

    /**
     * Returns the provenance information of a business conversation that
     * was forwarded into this message.
     *
     * @return an {@code Optional} containing the business forward info, or
     *         empty if the message is not a business forward
     */
    public Optional<BusinessMessageForwardInfo> businessMessageForwardInfo() {
        return Optional.ofNullable(businessMessageForwardInfo);
    }

    /**
     * Returns the campaign identifier assigned by a small-business client
     * to this message.
     *
     * @return an {@code Optional} containing the client campaign id, or
     *         empty if not set
     */
    public Optional<String> smbClientCampaignId() {
        return Optional.ofNullable(smbClientCampaignId);
    }

    /**
     * Returns the server-side campaign identifier for a small-business
     * conversation.
     *
     * @return an {@code Optional} containing the server campaign id, or
     *         empty if not set
     */
    public Optional<String> smbServerCampaignId() {
        return Optional.ofNullable(smbServerCampaignId);
    }

    /**
     * Returns the data sharing context associated with a business data
     * collection flow.
     *
     * @return an {@code Optional} containing the {@link DataSharingContext},
     *         or empty if not set
     */
    public Optional<DataSharingContext> dataSharingContext() {
        return Optional.ofNullable(dataSharingContext);
    }

    /**
     * Returns whether the ad attribution banner must always be shown
     * regardless of other heuristics.
     *
     * @return {@code true} when the banner must always be shown,
     *         {@code false} otherwise
     */
    public boolean alwaysShowAdAttribution() {
        return alwaysShowAdAttribution != null && alwaysShowAdAttribution;
    }

    /**
     * Returns the feature eligibility flags that describe which interactive
     * features are available on this message.
     *
     * @return an {@code Optional} containing the {@link FeatureEligibilities},
     *         or empty if not set
     */
    public Optional<FeatureEligibilities> featureEligibilities() {
        return Optional.ofNullable(featureEligibilities);
    }

    /**
     * Returns the external source identifier propagated from the entry
     * point conversion flow.
     *
     * @return an {@code Optional} containing the external source id, or
     *         empty if not set
     */
    public Optional<String> entryPointConversionExternalSource() {
        return Optional.ofNullable(entryPointConversionExternalSource);
    }

    /**
     * Returns the external medium identifier propagated from the entry
     * point conversion flow.
     *
     * @return an {@code Optional} containing the external medium id, or
     *         empty if not set
     */
    public Optional<String> entryPointConversionExternalMedium() {
        return Optional.ofNullable(entryPointConversionExternalMedium);
    }

    /**
     * Returns the Click-To-WhatsApp signals carried for attribution
     * purposes.
     *
     * @return an {@code Optional} containing the CTWA signals, or empty if
     *         not set
     */
    public Optional<String> ctwaSignals() {
        return Optional.ofNullable(ctwaSignals);
    }

    /**
     * Returns the binary payload attached to a Click-To-WhatsApp ad
     * conversion.
     *
     * @return an {@code Optional} containing the CTWA payload bytes, or
     *         empty if not set
     */
    public Optional<byte[]> ctwaPayload() {
        return Optional.ofNullable(ctwaPayload);
    }

    /**
     * Returns the provenance of an AI bot response that was forwarded into
     * this message.
     *
     * @return an {@code Optional} containing the forwarded AI bot info, or
     *         empty if not applicable
     */
    public Optional<ForwardedAIBotMessageInfo> forwardedAiBotMessageInfo() {
        return Optional.ofNullable(forwardedAiBotMessageInfo);
    }

    /**
     * Returns the reason this message is attributed to another user in a
     * status update context.
     *
     * @return an {@code Optional} containing the {@link StatusAttributionType},
     *         or empty if not set
     */
    public Optional<StatusAttributionType> statusAttributionType() {
        return Optional.ofNullable(statusAttributionType);
    }

    /**
     * Returns the map of trackable URLs present in this message.
     *
     * @return an {@code Optional} containing the {@link UrlTrackingMap}, or
     *         empty if not set
     */
    public Optional<UrlTrackingMap> urlTrackingMap() {
        return Optional.ofNullable(urlTrackingMap);
    }

    /**
     * Returns the paired media relationship that links this message's
     * media to a companion low-resolution or high-resolution copy.
     *
     * @return an {@code Optional} containing the {@link PairedMediaType},
     *         or empty if not paired
     */
    public Optional<PairedMediaType> pairedMediaType() {
        return Optional.ofNullable(pairedMediaType);
    }

    /**
     * Returns the version of the server-side ranking algorithm used to
     * score this message.
     *
     * @return the ranking version, or empty if not set
     */
    public OptionalInt rankingVersion() {
        return rankingVersion == null ? OptionalInt.empty() : OptionalInt.of(rankingVersion);
    }

    /**
     * Returns the label attached to the sender when the message is shown
     * in a group context.
     *
     * @return an {@code Optional} containing the {@link GroupParticipantLabel},
     *         or empty if not set
     */
    public Optional<GroupParticipantLabel> memberLabel() {
        return Optional.ofNullable(memberLabel);
    }

    /**
     * Returns whether this message is a question that expects a reply.
     *
     * @return {@code true} if the message is a question, {@code false}
     *         otherwise
     */
    public boolean isQuestion() {
        return isQuestion != null && isQuestion;
    }

    /**
     * Returns the type of the source media attached to this status update.
     *
     * @return an {@code Optional} containing the {@link StatusSourceType},
     *         or empty if not set
     */
    public Optional<StatusSourceType> statusSourceType() {
        return Optional.ofNullable(statusSourceType);
    }

    /**
     * Returns the list of attributions declared for this status update.
     *
     * @return an unmodifiable list of {@link StatusAttribution} entries,
     *         never {@code null}
     */
    public List<StatusAttribution> statusAttributions() {
        return statusAttributions == null ? List.of() : Collections.unmodifiableList(statusAttributions);
    }

    /**
     * Returns whether this status update targets a group audience rather
     * than the sender's contacts.
     *
     * @return {@code true} if the status is for a group, {@code false}
     *         otherwise
     */
    public boolean isGroupStatus() {
        return isGroupStatus != null && isGroupStatus;
    }

    /**
     * Returns the origin from which this message was forwarded.
     *
     * @return an {@code Optional} containing the {@link ForwardOrigin}, or
     *         empty if the message is not a forward
     */
    public Optional<ForwardOrigin> forwardOrigin() {
        return Optional.ofNullable(forwardOrigin);
    }

    /**
     * Returns the bundle pairing a quoted question and its response when
     * this message is a reply inside a question-and-answer flow.
     *
     * @return an {@code Optional} containing the
     *         {@link QuestionReplyQuotedMessage}, or empty if not set
     */
    public Optional<QuestionReplyQuotedMessage> questionReplyQuotedMessage() {
        return Optional.ofNullable(questionReplyQuotedMessage);
    }

    /**
     * Returns the audience metadata describing who is allowed to view this
     * status update.
     *
     * @return an {@code Optional} containing the
     *         {@link StatusAudienceMetadata}, or empty if not set
     */
    public Optional<StatusAudienceMetadata> statusAudienceMetadata() {
        return Optional.ofNullable(statusAudienceMetadata);
    }

    /**
     * Returns the number of mentions that do not correspond to a known
     * JID, such as phone numbers typed in plain text.
     *
     * @return the count of non-JID mentions, or empty if not set
     */
    public OptionalInt nonJidMentions() {
        return nonJidMentions == null ? OptionalInt.empty() : OptionalInt.of(nonJidMentions);
    }

    /**
     * Returns whether the quoted message was selected explicitly by the
     * user or attached automatically by the client.
     *
     * @return an {@code Optional} containing the {@link QuotedType}, or
     *         empty if no quoted message is set
     */
    public Optional<QuotedType> quotedType() {
        return Optional.ofNullable(quotedType);
    }

    /**
     * Returns the context attached to messages shared from an AI bot
     * session.
     *
     * @return an {@code Optional} containing the {@link BotMessageSharingInfo},
     *         or empty if not set
     */
    public Optional<BotMessageSharingInfo> botMessageSharingInfo() {
        return Optional.ofNullable(botMessageSharingInfo);
    }

    /**
     * Sets the identifier of the quoted message.
     *
     * @param quotedMessageId the quoted message id, or {@code null} to
     *                        clear it
     */
    public void setQuotedMessageId(String quotedMessageId) {
        this.quotedMessageId = quotedMessageId;
    }

    /**
     * Sets the JID of the user that originally sent the quoted message.
     *
     * @param quotedMessageSenderJid the sender JID, or {@code null} to
     *                               clear it
     */
    public void setQuotedMessageSenderJid(Jid quotedMessageSenderJid) {
        this.quotedMessageSenderJid = quotedMessageSenderJid;
    }

    /**
     * Sets the content preview of the quoted message.
     *
     * @param quotedMessageContent the preview content, or {@code null} to
     *                             clear it
     */
    public void setQuotedMessageContent(LinkedMessageContainer quotedMessageContent) {
        this.quotedMessageContent = quotedMessageContent;
    }

    /**
     * Sets the JID of the parent community of the group that owns the
     * quoted message.
     *
     * @param quotedMessageParentJid the parent community JID, or
     *                               {@code null} to clear it
     */
    public void setQuotedMessageParentJid(Jid quotedMessageParentJid) {
        this.quotedMessageParentJid = quotedMessageParentJid;
    }

    /**
     * Sets the list of individual users mentioned in this message.
     *
     * @param mentionedJids the list of mentioned JIDs, or {@code null} to
     *                      clear it
     */
    public void setMentionedJids(List<Jid> mentionedJids) {
        this.mentionedJids = mentionedJids;
    }

    /**
     * Sets the Click-To-WhatsApp conversion source that led to this
     * message.
     *
     * @param conversionSource the conversion source, or {@code null} to
     *                         clear it
     */
    public void setConversionSource(String conversionSource) {
        this.conversionSource = conversionSource;
    }

    /**
     * Sets the opaque conversion data payload associated with a
     * Click-To-WhatsApp attribution event.
     *
     * @param conversionData the conversion bytes, or {@code null} to clear
     *                       them
     */
    public void setConversionData(byte[] conversionData) {
        this.conversionData = conversionData;
    }

    /**
     * Sets the delay in seconds between the Click-To-WhatsApp conversion
     * event and this message.
     *
     * @param conversionDelaySeconds the delay in seconds, or {@code null}
     *                               to clear it
     */
    public void setConversionDelaySeconds(Integer conversionDelaySeconds) {
        this.conversionDelaySeconds = conversionDelaySeconds;
    }

    /**
     * Sets the number of times this message has been forwarded.
     *
     * @param forwardingScore the forwarding count, or {@code null} to
     *                        clear it
     */
    public void setForwardingScore(Integer forwardingScore) {
        this.forwardingScore = forwardingScore;
    }

    /**
     * Sets whether this message is marked as forwarded.
     *
     * @param isForwarded the forwarded flag, or {@code null} to clear it
     */
    public void setForwarded(Boolean isForwarded) {
        this.isForwarded = isForwarded;
    }

    /**
     * Sets the metadata describing a native ad that the user replied to in
     * order to produce this message.
     *
     * @param quotedAdReply the ad reply info, or {@code null} to clear it
     */
    public void setQuotedAdReply(AdReplyInfo quotedAdReply) {
        this.quotedAdReply = quotedAdReply;
    }

    /**
     * Sets the placeholder key used when the real quoted message is not
     * yet available to the client.
     *
     * @param placeholderKey the placeholder {@link MessageKey}, or
     *                       {@code null} to clear it
     */
    public void setPlaceholderKey(MessageKey placeholderKey) {
        this.placeholderKey = placeholderKey;
    }

    /**
     * Sets the disappearing-message duration, in seconds.
     *
     * @param ephemeralDuration the duration in seconds, or {@code null} to
     *                          clear it
     */
    public void setEphemeralDuration(Integer ephemeralDuration) {
        this.ephemeralDuration = ephemeralDuration;
    }

    /**
     * Sets the instant at which the current ephemeral setting was last
     * changed.
     *
     * @param ephemeralSettingTimestamp the timestamp, or {@code null} to
     *                                  clear it
     */
    public void setEphemeralSettingTimestamp(Instant ephemeralSettingTimestamp) {
        this.ephemeralSettingTimestamp = ephemeralSettingTimestamp;
    }

    /**
     * Sets the shared secret used to coordinate ephemeral settings between
     * participants.
     *
     * @param ephemeralSharedSecret the secret bytes, or {@code null} to
     *                              clear them
     */
    public void setEphemeralSharedSecret(byte[] ephemeralSharedSecret) {
        this.ephemeralSharedSecret = ephemeralSharedSecret;
    }

    /**
     * Sets the metadata describing the external ad that led to this
     * message.
     *
     * @param externalAdReply the external ad reply info, or {@code null}
     *                        to clear it
     */
    public void setExternalAdReply(ExternalAdReplyInfo externalAdReply) {
        this.externalAdReply = externalAdReply;
    }

    /**
     * Sets the entry point source identifier for the conversion flow.
     *
     * @param entryPointConversionSource the entry point source, or
     *                                   {@code null} to clear it
     */
    public void setEntryPointConversionSource(String entryPointConversionSource) {
        this.entryPointConversionSource = entryPointConversionSource;
    }

    /**
     * Sets the identifier of the application that generated the conversion
     * entry point.
     *
     * @param entryPointConversionApp the app identifier, or {@code null}
     *                                to clear it
     */
    public void setEntryPointConversionApp(String entryPointConversionApp) {
        this.entryPointConversionApp = entryPointConversionApp;
    }

    /**
     * Sets the delay, in seconds, between the entry point conversion event
     * and this message.
     *
     * @param entryPointConversionDelaySeconds the delay in seconds, or
     *                                         {@code null} to clear it
     */
    public void setEntryPointConversionDelaySeconds(Integer entryPointConversionDelaySeconds) {
        this.entryPointConversionDelaySeconds = entryPointConversionDelaySeconds;
    }

    /**
     * Sets the disappearing-messages configuration associated with this
     * message.
     *
     * @param disappearingMode the {@link ChatDisappearingMode}, or
     *                         {@code null} to clear it
     */
    public void setDisappearingMode(ChatDisappearingMode disappearingMode) {
        this.disappearingMode = disappearingMode;
    }

    /**
     * Sets the deep link to an interactive action related to this message.
     *
     * @param actionLink the {@link InteractiveActionLink}, or {@code null}
     *                   to clear it
     */
    public void setActionLink(InteractiveActionLink actionLink) {
        this.actionLink = actionLink;
    }

    /**
     * Sets the display subject of the group that owns the quoted message.
     *
     * @param quotedGroupSubject the group subject, or {@code null} to
     *                           clear it
     */
    public void setQuotedGroupSubject(String quotedGroupSubject) {
        this.quotedGroupSubject = quotedGroupSubject;
    }

    /**
     * Sets the JID of the parent community of the group that owns the
     * quoted message.
     *
     * @param quotedParentGroupJid the parent community JID, or
     *                             {@code null} to clear it
     */
    public void setQuotedParentGroupJid(Jid quotedParentGroupJid) {
        this.quotedParentGroupJid = quotedParentGroupJid;
    }

    /**
     * Sets the identifier of the trust banner shown to the recipient.
     *
     * @param trustBannerType the banner identifier, or {@code null} to
     *                        clear it
     */
    public void setTrustBannerType(String trustBannerType) {
        this.trustBannerType = trustBannerType;
    }

    /**
     * Sets the numeric identifier of the action bound to the trust banner.
     *
     * @param trustBannerAction the action code, or {@code null} to clear
     *                          it
     */
    public void setTrustBannerAction(Integer trustBannerAction) {
        this.trustBannerAction = trustBannerAction;
    }

    /**
     * Sets whether this message has been selected for telemetry sampling.
     *
     * @param isSampled the sampling flag, or {@code null} to clear it
     */
    public void setSampled(Boolean isSampled) {
        this.isSampled = isSampled;
    }

    /**
     * Sets the list of mentions that target whole groups rather than
     * individual users.
     *
     * @param groupMentions the list of {@link GroupMention} entries, or
     *                      {@code null} to clear it
     */
    public void setGroupMentions(List<GroupMention> groupMentions) {
        this.groupMentions = groupMentions;
    }

    /**
     * Sets the UTM attribution parameters propagated from a marketing
     * campaign.
     *
     * @param utm the {@link UTMInfo}, or {@code null} to clear it
     */
    public void setUtm(UTMInfo utm) {
        this.utm = utm;
    }

    /**
     * Sets the provenance information of a newsletter post forwarded into
     * this message.
     *
     * @param forwardedNewsletterMessageInfo the newsletter forward info,
     *                                       or {@code null} to clear it
     */
    public void setForwardedNewsletterMessageInfo(ForwardedNewsletterMessageInfo forwardedNewsletterMessageInfo) {
        this.forwardedNewsletterMessageInfo = forwardedNewsletterMessageInfo;
    }

    /**
     * Sets the provenance information of a business conversation
     * forwarded into this message.
     *
     * @param businessMessageForwardInfo the business forward info, or
     *                                   {@code null} to clear it
     */
    public void setBusinessMessageForwardInfo(BusinessMessageForwardInfo businessMessageForwardInfo) {
        this.businessMessageForwardInfo = businessMessageForwardInfo;
    }

    /**
     * Sets the campaign identifier assigned by a small-business client.
     *
     * @param smbClientCampaignId the client campaign id, or {@code null}
     *                            to clear it
     */
    public void setSmbClientCampaignId(String smbClientCampaignId) {
        this.smbClientCampaignId = smbClientCampaignId;
    }

    /**
     * Sets the server-side campaign identifier for a small-business
     * conversation.
     *
     * @param smbServerCampaignId the server campaign id, or {@code null}
     *                            to clear it
     */
    public void setSmbServerCampaignId(String smbServerCampaignId) {
        this.smbServerCampaignId = smbServerCampaignId;
    }

    /**
     * Sets the data sharing context for a business data collection flow.
     *
     * @param dataSharingContext the {@link DataSharingContext}, or
     *                           {@code null} to clear it
     */
    public void setDataSharingContext(DataSharingContext dataSharingContext) {
        this.dataSharingContext = dataSharingContext;
    }

    /**
     * Sets whether the ad attribution banner must always be shown.
     *
     * @param alwaysShowAdAttribution the flag, or {@code null} to clear it
     */
    public void setAlwaysShowAdAttribution(Boolean alwaysShowAdAttribution) {
        this.alwaysShowAdAttribution = alwaysShowAdAttribution;
    }

    /**
     * Sets the feature eligibility flags declared for this message.
     *
     * @param featureEligibilities the {@link FeatureEligibilities}, or
     *                             {@code null} to clear it
     */
    public void setFeatureEligibilities(FeatureEligibilities featureEligibilities) {
        this.featureEligibilities = featureEligibilities;
    }

    /**
     * Sets the external source identifier propagated from the entry point
     * conversion flow.
     *
     * @param entryPointConversionExternalSource the external source id,
     *                                           or {@code null} to clear
     *                                           it
     */
    public void setEntryPointConversionExternalSource(String entryPointConversionExternalSource) {
        this.entryPointConversionExternalSource = entryPointConversionExternalSource;
    }

    /**
     * Sets the external medium identifier propagated from the entry point
     * conversion flow.
     *
     * @param entryPointConversionExternalMedium the external medium id,
     *                                           or {@code null} to clear
     *                                           it
     */
    public void setEntryPointConversionExternalMedium(String entryPointConversionExternalMedium) {
        this.entryPointConversionExternalMedium = entryPointConversionExternalMedium;
    }

    /**
     * Sets the Click-To-WhatsApp signals carried for attribution purposes.
     *
     * @param ctwaSignals the CTWA signals, or {@code null} to clear them
     */
    public void setCtwaSignals(String ctwaSignals) {
        this.ctwaSignals = ctwaSignals;
    }

    /**
     * Sets the binary payload attached to a Click-To-WhatsApp ad
     * conversion.
     *
     * @param ctwaPayload the CTWA payload bytes, or {@code null} to clear
     *                    them
     */
    public void setCtwaPayload(byte[] ctwaPayload) {
        this.ctwaPayload = ctwaPayload;
    }

    /**
     * Sets the provenance of an AI bot response forwarded into this
     * message.
     *
     * @param forwardedAiBotMessageInfo the forwarded AI bot info, or
     *                                  {@code null} to clear it
     */
    public void setForwardedAiBotMessageInfo(ForwardedAIBotMessageInfo forwardedAiBotMessageInfo) {
        this.forwardedAiBotMessageInfo = forwardedAiBotMessageInfo;
    }

    /**
     * Sets the reason this message is attributed to another user in a
     * status context.
     *
     * @param statusAttributionType the {@link StatusAttributionType}, or
     *                              {@code null} to clear it
     */
    public void setStatusAttributionType(StatusAttributionType statusAttributionType) {
        this.statusAttributionType = statusAttributionType;
    }

    /**
     * Sets the map of trackable URLs present in this message.
     *
     * @param urlTrackingMap the {@link UrlTrackingMap}, or {@code null}
     *                       to clear it
     */
    public void setUrlTrackingMap(UrlTrackingMap urlTrackingMap) {
        this.urlTrackingMap = urlTrackingMap;
    }

    /**
     * Sets the paired media relationship for this message's media.
     *
     * @param pairedMediaType the {@link PairedMediaType}, or {@code null}
     *                        to clear it
     */
    public void setPairedMediaType(PairedMediaType pairedMediaType) {
        this.pairedMediaType = pairedMediaType;
    }

    /**
     * Sets the version of the server-side ranking algorithm used to score
     * this message.
     *
     * @param rankingVersion the ranking version, or {@code null} to clear
     *                       it
     */
    public void setRankingVersion(Integer rankingVersion) {
        this.rankingVersion = rankingVersion;
    }

    /**
     * Sets the label attached to the sender in a group context.
     *
     * @param memberLabel the {@link GroupParticipantLabel}, or
     *                    {@code null} to clear it
     */
    public void setMemberLabel(GroupParticipantLabel memberLabel) {
        this.memberLabel = memberLabel;
    }

    /**
     * Sets whether this message is a question expecting a reply.
     *
     * @param isQuestion the question flag, or {@code null} to clear it
     */
    public void setQuestion(Boolean isQuestion) {
        this.isQuestion = isQuestion;
    }

    /**
     * Sets the type of the source media attached to this status update.
     *
     * @param statusSourceType the {@link StatusSourceType}, or
     *                         {@code null} to clear it
     */
    public void setStatusSourceType(StatusSourceType statusSourceType) {
        this.statusSourceType = statusSourceType;
    }

    /**
     * Sets the list of attributions declared for this status update.
     *
     * @param statusAttributions the list of {@link StatusAttribution}
     *                           entries, or {@code null} to clear it
     */
    public void setStatusAttributions(List<StatusAttribution> statusAttributions) {
        this.statusAttributions = statusAttributions;
    }

    /**
     * Sets whether this status update targets a group audience.
     *
     * @param isGroupStatus the group-status flag, or {@code null} to
     *                      clear it
     */
    public void setGroupStatus(Boolean isGroupStatus) {
        this.isGroupStatus = isGroupStatus;
    }

    /**
     * Sets the origin from which this message was forwarded.
     *
     * @param forwardOrigin the {@link ForwardOrigin}, or {@code null} to
     *                      clear it
     */
    public void setForwardOrigin(ForwardOrigin forwardOrigin) {
        this.forwardOrigin = forwardOrigin;
    }

    /**
     * Sets the bundle pairing a quoted question and its response in a
     * question-and-answer flow.
     *
     * @param questionReplyQuotedMessage the {@link QuestionReplyQuotedMessage},
     *                                   or {@code null} to clear it
     */
    public void setQuestionReplyQuotedMessage(QuestionReplyQuotedMessage questionReplyQuotedMessage) {
        this.questionReplyQuotedMessage = questionReplyQuotedMessage;
    }

    /**
     * Sets the audience metadata for this status update.
     *
     * @param statusAudienceMetadata the {@link StatusAudienceMetadata},
     *                               or {@code null} to clear it
     */
    public void setStatusAudienceMetadata(StatusAudienceMetadata statusAudienceMetadata) {
        this.statusAudienceMetadata = statusAudienceMetadata;
    }

    /**
     * Sets the count of mentions that do not correspond to a known JID.
     *
     * @param nonJidMentions the non-JID mention count, or {@code null} to
     *                       clear it
     */
    public void setNonJidMentions(Integer nonJidMentions) {
        this.nonJidMentions = nonJidMentions;
    }

    /**
     * Sets whether the quoted message was selected explicitly or attached
     * automatically.
     *
     * @param quotedType the {@link QuotedType}, or {@code null} to clear
     *                   it
     */
    public void setQuotedType(QuotedType quotedType) {
        this.quotedType = quotedType;
    }

    /**
     * Sets the context attached to messages shared from an AI bot
     * session.
     *
     * @param botMessageSharingInfo the {@link BotMessageSharingInfo}, or
     *                              {@code null} to clear it
     */
    public void setBotMessageSharingInfo(BotMessageSharingInfo botMessageSharingInfo) {
        this.botMessageSharingInfo = botMessageSharingInfo;
    }

    /**
     * Removes all quoted-message related fields from this context info.
     *
     * <p>After this call the message is no longer considered a reply:
     * {@link #quotedMessageId()}, {@link #quotedMessageSenderJid()},
     * {@link #quotedMessageContent()} and {@link #quotedMessageParentJid()}
     * all return an empty {@code Optional}.
     */
    public void clearQuotedMessage() {
        this.quotedMessageId = null;
        this.quotedMessageSenderJid = null;
        this.quotedMessageContent = null;
        this.quotedMessageParentJid = null;
    }

    /**
     * Enumerates the possible surfaces a forwarded message may have come
     * from.
     *
     * <p>This value is set when a message is marked as a forward and lets
     * the receiver understand whether the original content was authored in
     * a regular chat, in a status update, in a channel, in a Meta AI
     * session or in user-generated content.
     */
    @ProtobufEnum(name = "ContextInfo.ForwardOrigin")
    public static enum ForwardOrigin {
        /**
         * The origin is not known or has not been populated by the sender.
         */
        UNKNOWN(0),
        /**
         * The message was forwarded from a regular one-to-one or group chat.
         */
        CHAT(1),
        /**
         * The message was forwarded from a status update.
         */
        STATUS(2),
        /**
         * The message was forwarded from a channel.
         */
        CHANNELS(3),
        /**
         * The message was forwarded from a Meta AI conversation.
         */
        META_AI(4),
        /**
         * The message was forwarded from user-generated content (for
         * example shared media outside a regular chat).
         */
        UGC(5);

        /**
         * Constructs a forward origin with the given protobuf index.
         *
         * @param index the protobuf wire index
         */
        ForwardOrigin(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index associated with this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Enumerates the relationships between paired media assets.
     *
     * <p>WhatsApp ships low-resolution and high-resolution copies of the
     * same media item together. The paired type tells the client whether
     * the current message is the parent (low resolution) or the child
     * (high resolution) variant for a given codec family.
     */
    @ProtobufEnum(name = "ContextInfo.PairedMediaType")
    public static enum PairedMediaType {
        /**
         * The media is not paired with any other variant.
         */
        NOT_PAIRED_MEDIA(0),
        /**
         * The message carries the standard-definition video parent.
         */
        SD_VIDEO_PARENT(1),
        /**
         * The message carries the high-definition video child.
         */
        HD_VIDEO_CHILD(2),
        /**
         * The message carries the standard-definition image parent.
         */
        SD_IMAGE_PARENT(3),
        /**
         * The message carries the high-definition image child.
         */
        HD_IMAGE_CHILD(4),
        /**
         * The message carries the motion-photo parent asset.
         */
        MOTION_PHOTO_PARENT(5),
        /**
         * The message carries the motion-photo child asset.
         */
        MOTION_PHOTO_CHILD(6),
        /**
         * The message carries the HEVC video parent asset.
         */
        HEVC_VIDEO_PARENT(7),
        /**
         * The message carries the HEVC video child asset.
         */
        HEVC_VIDEO_CHILD(8);

        /**
         * Constructs a paired media type with the given protobuf index.
         *
         * @param index the protobuf wire index
         */
        PairedMediaType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index associated with this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Enumerates how a quoted message came to be attached to a reply.
     */
    @ProtobufEnum(name = "ContextInfo.QuotedType")
    public static enum QuotedType {
        /**
         * The user explicitly chose the quoted message by swiping on it or
         * tapping the reply action.
         */
        EXPLICIT(0),
        /**
         * The client attached the quoted message automatically (for
         * example when replying to an ad or a question).
         */
        AUTO(1);

        /**
         * Constructs a quoted type with the given protobuf index.
         *
         * @param index the protobuf wire index
         */
        QuotedType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index associated with this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Enumerates the reasons a status update can be attributed to another
     * user.
     *
     * <p>WhatsApp attaches an attribution tag to status reshares in order
     * to credit the original author and to distinguish mentions from full
     * post reshares and from cross-posted forwards.
     */
    @ProtobufEnum(name = "ContextInfo.StatusAttributionType")
    public static enum StatusAttributionType {
        /**
         * No status attribution is declared.
         */
        NONE(0),
        /**
         * The status update was reshared from a mention in another user's
         * status.
         */
        RESHARED_FROM_MENTION(1),
        /**
         * The status update was reshared from a previous post.
         */
        RESHARED_FROM_POST(2),
        /**
         * The status update was reshared from a post that has been
         * reshared many times.
         */
        RESHARED_FROM_POST_MANY_TIMES(3),
        /**
         * The status update was forwarded from another status.
         */
        FORWARDED_FROM_STATUS(4);

        /**
         * Constructs a status attribution type with the given protobuf
         * index.
         *
         * @param index the protobuf wire index
         */
        StatusAttributionType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index associated with this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Enumerates the kinds of source media that can be posted as a status
     * update.
     */
    @ProtobufEnum(name = "ContextInfo.StatusSourceType")
    public static enum StatusSourceType {
        /**
         * The status update contains an image.
         */
        IMAGE(0),
        /**
         * The status update contains a video.
         */
        VIDEO(1),
        /**
         * The status update contains an animated GIF.
         */
        GIF(2),
        /**
         * The status update contains an audio track or a voice note.
         */
        AUDIO(3),
        /**
         * The status update contains a text post.
         */
        TEXT(4),
        /**
         * The status update contains a standalone music track.
         */
        MUSIC_STANDALONE(5);

        /**
         * Constructs a status source type with the given protobuf index.
         *
         * @param index the protobuf wire index
         */
        StatusSourceType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index associated with this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Metadata describing a native WhatsApp ad that was replied to in
     * order to start the conversation carrying this {@link ContextInfo}.
     *
     * <p>When a user taps the reply button on an ad rendered inside
     * WhatsApp, the resulting message carries an {@code AdReplyInfo} with
     * enough data to render a preview of the ad (advertiser name, media
     * type, thumbnail and caption) on both sides of the conversation.
     */
    @ProtobufMessage(name = "ContextInfo.AdReplyInfo")
    public static final class AdReplyInfo {
        /**
         * Display name of the advertiser that published the ad.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String advertiserName;

        /**
         * Type of the media attached to the ad (image, video or none).
         */
        @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
        AdReplyInfo.MediaType mediaType;

        /**
         * JPEG thumbnail used to render a preview of the ad.
         */
        @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
        byte[] jpegThumbnail;

        /**
         * Textual caption attached to the ad by the advertiser.
         */
        @ProtobufProperty(index = 17, type = ProtobufType.STRING)
        String caption;


        /**
         * Constructs a fully populated {@code AdReplyInfo}.
         *
         * @param advertiserName see {@link #advertiserName()}
         * @param mediaType      see {@link #mediaType()}
         * @param jpegThumbnail  see {@link #jpegThumbnail()}
         * @param caption        see {@link #caption()}
         */
        AdReplyInfo(String advertiserName, MediaType mediaType, byte[] jpegThumbnail, String caption) {
            this.advertiserName = advertiserName;
            this.mediaType = mediaType;
            this.jpegThumbnail = jpegThumbnail;
            this.caption = caption;
        }

        /**
         * Returns the display name of the advertiser.
         *
         * @return an {@code Optional} containing the advertiser name, or
         *         empty if not set
         */
        public Optional<String> advertiserName() {
            return Optional.ofNullable(advertiserName);
        }

        /**
         * Returns the media type of the original ad.
         *
         * @return an {@code Optional} containing the {@link MediaType}, or
         *         empty if not set
         */
        public Optional<MediaType> mediaType() {
            return Optional.ofNullable(mediaType);
        }

        /**
         * Returns the JPEG thumbnail used to render the ad preview.
         *
         * @return an {@code Optional} containing the thumbnail bytes, or
         *         empty if not set
         */
        public Optional<byte[]> jpegThumbnail() {
            return Optional.ofNullable(jpegThumbnail);
        }

        /**
         * Returns the textual caption attached to the ad.
         *
         * @return an {@code Optional} containing the caption, or empty if
         *         not set
         */
        public Optional<String> caption() {
            return Optional.ofNullable(caption);
        }

        /**
         * Sets the display name of the advertiser.
         *
         * @param advertiserName the advertiser name, or {@code null} to
         *                       clear it
         */
        public void setAdvertiserName(String advertiserName) {
            this.advertiserName = advertiserName;
    }

        /**
         * Sets the media type of the original ad.
         *
         * @param mediaType the {@link MediaType}, or {@code null} to clear
         *                  it
         */
        public void setMediaType(MediaType mediaType) {
            this.mediaType = mediaType;
    }

        /**
         * Sets the JPEG thumbnail used to render the ad preview.
         *
         * @param jpegThumbnail the thumbnail bytes, or {@code null} to
         *                      clear it
         */
        public void setJpegThumbnail(byte[] jpegThumbnail) {
            this.jpegThumbnail = jpegThumbnail;
    }

        /**
         * Sets the textual caption attached to the ad.
         *
         * @param caption the caption, or {@code null} to clear it
         */
        public void setCaption(String caption) {
            this.caption = caption;
    }

        /**
         * Enumerates the possible media types for a native WhatsApp ad.
         */
        @ProtobufEnum(name = "ContextInfo.AdReplyInfo.MediaType")
        public static enum MediaType {
            /**
             * The ad does not carry any media.
             */
            NONE(0),
            /**
             * The ad carries an image.
             */
            IMAGE(1),
            /**
             * The ad carries a video.
             */
            VIDEO(2);

            /**
             * Constructs a media type with the given protobuf index.
             *
             * @param index the protobuf wire index
             */
            MediaType(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf wire index associated with this constant.
             */
            final int index;

            /**
             * Returns the protobuf wire index of this constant.
             *
             * @return the protobuf index
             */
            public int index() {
                return this.index;
            }
        }
    }

    /**
     * Provenance information attached to a message that was forwarded out
     * of a business conversation.
     *
     * <p>The receiver uses the business owner JID to render an attribution
     * tag on the forwarded message.
     */
    @ProtobufMessage(name = "ContextInfo.BusinessMessageForwardInfo")
    public static final class BusinessMessageForwardInfo {
        /**
         * JID of the business account whose conversation originated the
         * forwarded message.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid businessOwnerJid;


        /**
         * Constructs a {@code BusinessMessageForwardInfo} with the given
         * business owner.
         *
         * @param businessOwnerJid see {@link #businessOwnerJid()}
         */
        BusinessMessageForwardInfo(Jid businessOwnerJid) {
            this.businessOwnerJid = businessOwnerJid;
        }

        /**
         * Returns the JID of the business account whose conversation
         * originated the forwarded message.
         *
         * @return an {@code Optional} containing the business owner JID,
         *         or empty if not set
         */
        public Optional<Jid> businessOwnerJid() {
            return Optional.ofNullable(businessOwnerJid);
        }

        /**
         * Sets the JID of the business account whose conversation
         * originated the forwarded message.
         *
         * @param businessOwnerJid the business owner JID, or {@code null}
         *                         to clear it
         */
        public void setBusinessOwnerJid(Jid businessOwnerJid) {
            this.businessOwnerJid = businessOwnerJid;
    }
    }

    /**
     * Describes the data-sharing consent and the accompanying parameters
     * attached to a message that participates in a business data
     * collection flow.
     *
     * <p>WhatsApp business messaging can expose disclosure banners and
     * collect structured parameters that the user has consented to share
     * with the business. This type carries the consent token, the flags
     * controlling when the disclosure is shown, and an arbitrary list of
     * typed parameters.
     */
    @ProtobufMessage(name = "ContextInfo.DataSharingContext")
    public static final class DataSharingContext {
        /**
         * Whether the messenger disclosure banner must be shown to the
         * user.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        Boolean showMmDisclosure;

        /**
         * Encrypted token proving that the user has consented to share
         * signal data with the business.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String encryptedSignalTokenConsented;

        /**
         * Typed parameters attached to the data sharing payload.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        List<Parameters> parameters;

        /**
         * Bitmask of {@link DataSharingFlags} values controlling when the
         * disclosure must be shown.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.INT32)
        Integer dataSharingFlags;


        /**
         * Constructs a {@code DataSharingContext} with the given attributes.
         *
         * @param showMmDisclosure             see {@link #showMmDisclosure()}
         * @param encryptedSignalTokenConsented see {@link #encryptedSignalTokenConsented()}
         * @param parameters                   see {@link #parameters()}
         * @param dataSharingFlags             see {@link #dataSharingFlags()}
         */
        DataSharingContext(Boolean showMmDisclosure, String encryptedSignalTokenConsented, List<Parameters> parameters, Integer dataSharingFlags) {
            this.showMmDisclosure = showMmDisclosure;
            this.encryptedSignalTokenConsented = encryptedSignalTokenConsented;
            this.parameters = parameters;
            this.dataSharingFlags = dataSharingFlags;
        }

        /**
         * Returns whether the messenger disclosure banner must be shown.
         *
         * @return {@code true} if the disclosure banner is required,
         *         {@code false} otherwise
         */
        public boolean showMmDisclosure() {
            return showMmDisclosure != null && showMmDisclosure;
        }

        /**
         * Returns the encrypted token proving that the user has consented
         * to share signal data with the business.
         *
         * @return an {@code Optional} containing the consent token, or
         *         empty if not set
         */
        public Optional<String> encryptedSignalTokenConsented() {
            return Optional.ofNullable(encryptedSignalTokenConsented);
        }

        /**
         * Returns the typed parameters attached to the data sharing
         * payload.
         *
         * @return an unmodifiable list of {@link Parameters}, never
         *         {@code null}
         */
        public List<Parameters> parameters() {
            return parameters == null ? List.of() : Collections.unmodifiableList(parameters);
        }

        /**
         * Returns the bitmask of {@link DataSharingFlags} values that
         * control when the disclosure must be shown.
         *
         * @return the flags bitmask, or empty if not set
         */
        public OptionalInt dataSharingFlags() {
            return dataSharingFlags == null ? OptionalInt.empty() : OptionalInt.of(dataSharingFlags);
        }

        /**
         * Sets whether the messenger disclosure banner must be shown.
         *
         * @param showMmDisclosure the banner flag, or {@code null} to
         *                         clear it
         */
        public void setShowMmDisclosure(Boolean showMmDisclosure) {
            this.showMmDisclosure = showMmDisclosure;
    }

        /**
         * Sets the encrypted consent token.
         *
         * @param encryptedSignalTokenConsented the consent token, or
         *                                      {@code null} to clear it
         */
        public void setEncryptedSignalTokenConsented(String encryptedSignalTokenConsented) {
            this.encryptedSignalTokenConsented = encryptedSignalTokenConsented;
    }

        /**
         * Sets the list of typed parameters attached to the data sharing
         * payload.
         *
         * @param parameters the list of {@link Parameters}, or
         *                   {@code null} to clear it
         */
        public void setParameters(List<Parameters> parameters) {
            this.parameters = parameters;
    }

        /**
         * Sets the bitmask of {@link DataSharingFlags} values.
         *
         * @param dataSharingFlags the flags bitmask, or {@code null} to
         *                         clear it
         */
        public void setDataSharingFlags(Integer dataSharingFlags) {
            this.dataSharingFlags = dataSharingFlags;
    }

        /**
         * Enumerates the events that trigger the messenger disclosure
         * banner in a data sharing flow.
         */
        @ProtobufEnum(name = "ContextInfo.DataSharingContext.DataSharingFlags")
        public static enum DataSharingFlags {
            /**
             * Show the messenger disclosure banner when the user taps on
             * the message.
             */
            SHOW_MM_DISCLOSURE_ON_CLICK(1),
            /**
             * Show the messenger disclosure banner when the user reads
             * the message.
             */
            SHOW_MM_DISCLOSURE_ON_READ(2);

            /**
             * Constructs a data sharing flag with the given protobuf
             * index.
             *
             * @param index the protobuf wire index
             */
            DataSharingFlags(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf wire index associated with this constant.
             */
            final int index;

            /**
             * Returns the protobuf wire index of this constant.
             *
             * @return the protobuf index
             */
            public int index() {
                return this.index;
            }
        }

        /**
         * A typed key-value pair that is part of a data sharing payload.
         *
         * <p>Each parameter carries a mandatory textual key plus one of
         * several typed values (string, integer, float or nested
         * parameters). Only one of the typed values is expected to be
         * populated per parameter.
         */
        @ProtobufMessage(name = "ContextInfo.DataSharingContext.Parameters")
        public static final class Parameters {
            /**
             * Identifier of the parameter.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String key;

            /**
             * Textual value of the parameter, when applicable.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String stringData;

            /**
             * Integer value of the parameter, when applicable.
             */
            @ProtobufProperty(index = 3, type = ProtobufType.INT64)
            Long intData;

            /**
             * Floating-point value of the parameter, when applicable.
             */
            @ProtobufProperty(index = 4, type = ProtobufType.FLOAT)
            Float floatData;

            /**
             * Nested parameter value used to model hierarchical payloads.
             */
            @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
            DataSharingContext.Parameters contents;


            /**
             * Constructs a typed parameter.
             *
             * @param key        see {@link #key()}
             * @param stringData see {@link #stringData()}
             * @param intData    see {@link #intData()}
             * @param floatData  see {@link #floatData()}
             * @param contents   see {@link #contents()}
             */
            Parameters(String key, String stringData, Long intData, Float floatData, Parameters contents) {
                this.key = key;
                this.stringData = stringData;
                this.intData = intData;
                this.floatData = floatData;
                this.contents = contents;
            }

            /**
             * Returns the identifier of this parameter.
             *
             * @return an {@code Optional} containing the key, or empty
             *         if not set
             */
            public Optional<String> key() {
                return Optional.ofNullable(key);
            }

            /**
             * Returns the textual value of this parameter.
             *
             * @return an {@code Optional} containing the string value,
             *         or empty if not set
             */
            public Optional<String> stringData() {
                return Optional.ofNullable(stringData);
            }

            /**
             * Returns the integer value of this parameter.
             *
             * @return the integer value, or empty if not set
             */
            public OptionalLong intData() {
                return intData == null ? OptionalLong.empty() : OptionalLong.of(intData);
            }

            /**
             * Returns the floating-point value of this parameter.
             *
             * @return the floating-point value, or empty if not set
             */
            public OptionalDouble floatData() {
                return floatData == null ? OptionalDouble.empty() : OptionalDouble.of(floatData);
            }

            /**
             * Returns the nested parameter that this entry wraps, used
             * to model hierarchical payloads.
             *
             * @return an {@code Optional} containing the nested
             *         parameter, or empty if not set
             */
            public Optional<Parameters> contents() {
                return Optional.ofNullable(contents);
            }

            /**
             * Sets the identifier of this parameter.
             *
             * @param key the key, or {@code null} to clear it
             */
            public void setKey(String key) {
                this.key = key;
    }

            /**
             * Sets the textual value of this parameter.
             *
             * @param stringData the string value, or {@code null} to
             *                   clear it
             */
            public void setStringData(String stringData) {
                this.stringData = stringData;
    }

            /**
             * Sets the integer value of this parameter.
             *
             * @param intData the integer value, or {@code null} to clear
             *                it
             */
            public void setIntData(Long intData) {
                this.intData = intData;
    }

            /**
             * Sets the floating-point value of this parameter.
             *
             * @param floatData the floating-point value, or {@code null}
             *                  to clear it
             */
            public void setFloatData(Float floatData) {
                this.floatData = floatData;
    }

            /**
             * Sets the nested parameter value.
             *
             * @param contents the nested parameter, or {@code null} to
             *                 clear it
             */
            public void setContents(Parameters contents) {
                this.contents = contents;
    }
        }
    }

    /**
     * Metadata describing an external (off-WhatsApp) ad that led the user
     * to start the conversation carrying this {@link ContextInfo}.
     *
     * <p>External ads include Click-To-WhatsApp placements served on
     * Facebook, Instagram or other Meta surfaces. This type carries the
     * full creative data (title, body, thumbnails, media URLs), the
     * attribution identifiers (CTWA click id, source, ref), the
     * configuration of the business greeting flow and the ad format
     * classification.
     */
    @ProtobufMessage(name = "ContextInfo.ExternalAdReplyInfo")
    public static final class ExternalAdReplyInfo {
        /**
         * Headline of the external ad.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String title;

        /**
         * Descriptive body text of the external ad.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String body;

        /**
         * Type of the media attached to the external ad.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
        ExternalAdReplyInfo.MediaType mediaType;

        /**
         * URL of the thumbnail associated with the ad creative.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String thumbnailUrl;

        /**
         * URL of the full-resolution media attached to the ad.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        String mediaUrl;

        /**
         * Inline thumbnail bytes used to render the ad preview when the
         * remote thumbnail URL is unavailable.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
        byte[] thumbnail;

        /**
         * Identifier of the surface that served the ad (for example
         * Facebook or Instagram).
         */
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        String sourceType;

        /**
         * Identifier of the ad unit within the source surface.
         */
        @ProtobufProperty(index = 8, type = ProtobufType.STRING)
        String sourceId;

        /**
         * Deep-link URL of the original ad.
         */
        @ProtobufProperty(index = 9, type = ProtobufType.STRING)
        String sourceUrl;

        /**
         * Whether the business account has an automated greeting enabled
         * for this ad.
         */
        @ProtobufProperty(index = 10, type = ProtobufType.BOOL)
        Boolean containsAutoReply;

        /**
         * Whether the ad preview must be rendered with a larger thumbnail.
         */
        @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
        Boolean renderLargerThumbnail;

        /**
         * Whether the ad attribution banner must be shown above the
         * message.
         */
        @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
        Boolean showAdAttribution;

        /**
         * Click-To-WhatsApp click identifier propagated from the ad
         * platform.
         */
        @ProtobufProperty(index = 13, type = ProtobufType.STRING)
        String ctwaClid;

        /**
         * Ad reference string used by Meta attribution pipelines.
         */
        @ProtobufProperty(index = 14, type = ProtobufType.STRING)
        String ref;

        /**
         * Whether the ad produced a Click-To-WhatsApp voice call instead
         * of a chat.
         */
        @ProtobufProperty(index = 15, type = ProtobufType.BOOL)
        Boolean clickToWhatsappCall;

        /**
         * Whether the ad context preview has been dismissed by the user.
         */
        @ProtobufProperty(index = 16, type = ProtobufType.BOOL)
        Boolean adContextPreviewDismissed;

        /**
         * Identifier of the application that served the ad (for example
         * {@code FB} or {@code IG}).
         */
        @ProtobufProperty(index = 17, type = ProtobufType.STRING)
        String sourceApp;

        /**
         * Whether the automated greeting message has been shown to the
         * user.
         */
        @ProtobufProperty(index = 18, type = ProtobufType.BOOL)
        Boolean automatedGreetingMessageShown;

        /**
         * Body text of the automated greeting message.
         */
        @ProtobufProperty(index = 19, type = ProtobufType.STRING)
        String greetingMessageBody;

        /**
         * Opaque payload attached to the call-to-action of the ad.
         */
        @ProtobufProperty(index = 20, type = ProtobufType.STRING)
        String ctaPayload;

        /**
         * Whether the nudge prompting the user to reply must be
         * suppressed for this ad.
         */
        @ProtobufProperty(index = 21, type = ProtobufType.BOOL)
        Boolean disableNudge;

        /**
         * URL of the original unprocessed image for the ad creative.
         */
        @ProtobufProperty(index = 22, type = ProtobufType.STRING)
        String originalImageUrl;

        /**
         * Identifier of the call-to-action type displayed in the
         * automated greeting message.
         */
        @ProtobufProperty(index = 23, type = ProtobufType.STRING)
        String automatedGreetingMessageCtaType;

        /**
         * Whether the ad uses the WhatsApp-to-WhatsApp format.
         */
        @ProtobufProperty(index = 24, type = ProtobufType.BOOL)
        Boolean wtwaAdFormat;

        /**
         * Type of the ad (Click-To-WhatsApp, call-as-web-click, etc.).
         */
        @ProtobufProperty(index = 25, type = ProtobufType.ENUM)
        ExternalAdReplyInfo.AdType adType;

        /**
         * URL of the advertiser website linked from the WhatsApp-to-
         * WhatsApp ad format.
         */
        @ProtobufProperty(index = 26, type = ProtobufType.STRING)
        String wtwaWebsiteUrl;

        /**
         * URL used to render a live preview of the ad in the chat.
         */
        @ProtobufProperty(index = 27, type = ProtobufType.STRING)
        String adPreviewUrl;


        /**
         * Constructs a fully populated {@code ExternalAdReplyInfo}.
         *
         * @param title                           see {@link #title()}
         * @param body                            see {@link #body()}
         * @param mediaType                       see {@link #mediaType()}
         * @param thumbnailUrl                    see {@link #thumbnailUrl()}
         * @param mediaUrl                        see {@link #mediaUrl()}
         * @param thumbnail                       see {@link #thumbnail()}
         * @param sourceType                      see {@link #sourceType()}
         * @param sourceId                        see {@link #sourceId()}
         * @param sourceUrl                       see {@link #sourceUrl()}
         * @param containsAutoReply               see {@link #containsAutoReply()}
         * @param renderLargerThumbnail           see {@link #renderLargerThumbnail()}
         * @param showAdAttribution               see {@link #showAdAttribution()}
         * @param ctwaClid                        see {@link #ctwaClid()}
         * @param ref                             see {@link #ref()}
         * @param clickToWhatsappCall             see {@link #clickToWhatsappCall()}
         * @param adContextPreviewDismissed       see {@link #adContextPreviewDismissed()}
         * @param sourceApp                       see {@link #sourceApp()}
         * @param automatedGreetingMessageShown   see {@link #automatedGreetingMessageShown()}
         * @param greetingMessageBody             see {@link #greetingMessageBody()}
         * @param ctaPayload                      see {@link #ctaPayload()}
         * @param disableNudge                    see {@link #disableNudge()}
         * @param originalImageUrl                see {@link #originalImageUrl()}
         * @param automatedGreetingMessageCtaType see {@link #automatedGreetingMessageCtaType()}
         * @param wtwaAdFormat                    see {@link #wtwaAdFormat()}
         * @param adType                          see {@link #adType()}
         * @param wtwaWebsiteUrl                  see {@link #wtwaWebsiteUrl()}
         * @param adPreviewUrl                    see {@link #adPreviewUrl()}
         */
        ExternalAdReplyInfo(String title, String body, MediaType mediaType, String thumbnailUrl, String mediaUrl, byte[] thumbnail, String sourceType, String sourceId, String sourceUrl, Boolean containsAutoReply, Boolean renderLargerThumbnail, Boolean showAdAttribution, String ctwaClid, String ref, Boolean clickToWhatsappCall, Boolean adContextPreviewDismissed, String sourceApp, Boolean automatedGreetingMessageShown, String greetingMessageBody, String ctaPayload, Boolean disableNudge, String originalImageUrl, String automatedGreetingMessageCtaType, Boolean wtwaAdFormat, AdType adType, String wtwaWebsiteUrl, String adPreviewUrl) {
            this.title = title;
            this.body = body;
            this.mediaType = mediaType;
            this.thumbnailUrl = thumbnailUrl;
            this.mediaUrl = mediaUrl;
            this.thumbnail = thumbnail;
            this.sourceType = sourceType;
            this.sourceId = sourceId;
            this.sourceUrl = sourceUrl;
            this.containsAutoReply = containsAutoReply;
            this.renderLargerThumbnail = renderLargerThumbnail;
            this.showAdAttribution = showAdAttribution;
            this.ctwaClid = ctwaClid;
            this.ref = ref;
            this.clickToWhatsappCall = clickToWhatsappCall;
            this.adContextPreviewDismissed = adContextPreviewDismissed;
            this.sourceApp = sourceApp;
            this.automatedGreetingMessageShown = automatedGreetingMessageShown;
            this.greetingMessageBody = greetingMessageBody;
            this.ctaPayload = ctaPayload;
            this.disableNudge = disableNudge;
            this.originalImageUrl = originalImageUrl;
            this.automatedGreetingMessageCtaType = automatedGreetingMessageCtaType;
            this.wtwaAdFormat = wtwaAdFormat;
            this.adType = adType;
            this.wtwaWebsiteUrl = wtwaWebsiteUrl;
            this.adPreviewUrl = adPreviewUrl;
        }

        /**
         * Returns the headline of the external ad.
         *
         * @return an {@code Optional} containing the title, or empty if
         *         not set
         */
        public Optional<String> title() {
            return Optional.ofNullable(title);
        }

        /**
         * Returns the descriptive body text of the external ad.
         *
         * @return an {@code Optional} containing the body, or empty if
         *         not set
         */
        public Optional<String> body() {
            return Optional.ofNullable(body);
        }

        /**
         * Returns the type of media attached to the external ad.
         *
         * @return an {@code Optional} containing the {@link MediaType},
         *         or empty if not set
         */
        public Optional<MediaType> mediaType() {
            return Optional.ofNullable(mediaType);
        }

        /**
         * Returns the URL of the thumbnail attached to the ad creative.
         *
         * @return an {@code Optional} containing the thumbnail URL, or
         *         empty if not set
         */
        public Optional<String> thumbnailUrl() {
            return Optional.ofNullable(thumbnailUrl);
        }

        /**
         * Returns the URL of the full-resolution media attached to the ad.
         *
         * @return an {@code Optional} containing the media URL, or empty
         *         if not set
         */
        public Optional<String> mediaUrl() {
            return Optional.ofNullable(mediaUrl);
        }

        /**
         * Returns the inline thumbnail bytes for the ad preview.
         *
         * @return an {@code Optional} containing the thumbnail bytes, or
         *         empty if not set
         */
        public Optional<byte[]> thumbnail() {
            return Optional.ofNullable(thumbnail);
        }

        /**
         * Returns the identifier of the surface that served the ad.
         *
         * @return an {@code Optional} containing the source type, or
         *         empty if not set
         */
        public Optional<String> sourceType() {
            return Optional.ofNullable(sourceType);
        }

        /**
         * Returns the identifier of the ad unit within the source
         * surface.
         *
         * @return an {@code Optional} containing the source id, or empty
         *         if not set
         */
        public Optional<String> sourceId() {
            return Optional.ofNullable(sourceId);
        }

        /**
         * Returns the deep-link URL of the original ad.
         *
         * @return an {@code Optional} containing the source URL, or empty
         *         if not set
         */
        public Optional<String> sourceUrl() {
            return Optional.ofNullable(sourceUrl);
        }

        /**
         * Returns whether the business account has an automated greeting
         * enabled for this ad.
         *
         * @return {@code true} if an auto reply is configured,
         *         {@code false} otherwise
         */
        public boolean containsAutoReply() {
            return containsAutoReply != null && containsAutoReply;
        }

        /**
         * Returns whether the ad preview must be rendered with a larger
         * thumbnail.
         *
         * @return {@code true} if a larger thumbnail must be rendered,
         *         {@code false} otherwise
         */
        public boolean renderLargerThumbnail() {
            return renderLargerThumbnail != null && renderLargerThumbnail;
        }

        /**
         * Returns whether the ad attribution banner must be shown above
         * the message.
         *
         * @return {@code true} if the banner must be shown, {@code false}
         *         otherwise
         */
        public boolean showAdAttribution() {
            return showAdAttribution != null && showAdAttribution;
        }

        /**
         * Returns the Click-To-WhatsApp click identifier propagated from
         * the ad platform.
         *
         * @return an {@code Optional} containing the CTWA click id, or
         *         empty if not set
         */
        public Optional<String> ctwaClid() {
            return Optional.ofNullable(ctwaClid);
        }

        /**
         * Returns the ad reference string used by Meta attribution
         * pipelines.
         *
         * @return an {@code Optional} containing the ref string, or
         *         empty if not set
         */
        public Optional<String> ref() {
            return Optional.ofNullable(ref);
        }

        /**
         * Returns whether the ad produced a Click-To-WhatsApp voice call
         * instead of a chat.
         *
         * @return {@code true} if the ad triggered a call, {@code false}
         *         otherwise
         */
        public boolean clickToWhatsappCall() {
            return clickToWhatsappCall != null && clickToWhatsappCall;
        }

        /**
         * Returns whether the ad context preview has been dismissed by
         * the user.
         *
         * @return {@code true} if the preview was dismissed,
         *         {@code false} otherwise
         */
        public boolean adContextPreviewDismissed() {
            return adContextPreviewDismissed != null && adContextPreviewDismissed;
        }

        /**
         * Returns the identifier of the application that served the ad.
         *
         * @return an {@code Optional} containing the source app, or
         *         empty if not set
         */
        public Optional<String> sourceApp() {
            return Optional.ofNullable(sourceApp);
        }

        /**
         * Returns whether the automated greeting message has been shown
         * to the user.
         *
         * @return {@code true} if the greeting was shown, {@code false}
         *         otherwise
         */
        public boolean automatedGreetingMessageShown() {
            return automatedGreetingMessageShown != null && automatedGreetingMessageShown;
        }

        /**
         * Returns the body text of the automated greeting message.
         *
         * @return an {@code Optional} containing the greeting body, or
         *         empty if not set
         */
        public Optional<String> greetingMessageBody() {
            return Optional.ofNullable(greetingMessageBody);
        }

        /**
         * Returns the opaque payload attached to the call-to-action of
         * the ad.
         *
         * @return an {@code Optional} containing the CTA payload, or
         *         empty if not set
         */
        public Optional<String> ctaPayload() {
            return Optional.ofNullable(ctaPayload);
        }

        /**
         * Returns whether the nudge prompting the user to reply must be
         * suppressed for this ad.
         *
         * @return {@code true} if the nudge is disabled, {@code false}
         *         otherwise
         */
        public boolean disableNudge() {
            return disableNudge != null && disableNudge;
        }

        /**
         * Returns the URL of the original unprocessed image for the ad
         * creative.
         *
         * @return an {@code Optional} containing the original image URL,
         *         or empty if not set
         */
        public Optional<String> originalImageUrl() {
            return Optional.ofNullable(originalImageUrl);
        }

        /**
         * Returns the identifier of the call-to-action type displayed in
         * the automated greeting message.
         *
         * @return an {@code Optional} containing the CTA type id, or
         *         empty if not set
         */
        public Optional<String> automatedGreetingMessageCtaType() {
            return Optional.ofNullable(automatedGreetingMessageCtaType);
        }

        /**
         * Returns whether the ad uses the WhatsApp-to-WhatsApp format.
         *
         * @return {@code true} if the WhatsApp-to-WhatsApp format is in
         *         use, {@code false} otherwise
         */
        public boolean wtwaAdFormat() {
            return wtwaAdFormat != null && wtwaAdFormat;
        }

        /**
         * Returns the type of the ad.
         *
         * @return an {@code Optional} containing the {@link AdType}, or
         *         empty if not set
         */
        public Optional<AdType> adType() {
            return Optional.ofNullable(adType);
        }

        /**
         * Returns the URL of the advertiser website linked from the
         * WhatsApp-to-WhatsApp ad format.
         *
         * @return an {@code Optional} containing the advertiser website
         *         URL, or empty if not set
         */
        public Optional<String> wtwaWebsiteUrl() {
            return Optional.ofNullable(wtwaWebsiteUrl);
        }

        /**
         * Returns the URL used to render a live preview of the ad in the
         * chat.
         *
         * @return an {@code Optional} containing the ad preview URL, or
         *         empty if not set
         */
        public Optional<String> adPreviewUrl() {
            return Optional.ofNullable(adPreviewUrl);
        }

        /**
         * Sets the headline of the external ad.
         *
         * @param title the title, or {@code null} to clear it
         */
        public void setTitle(String title) {
            this.title = title;
    }

        /**
         * Sets the descriptive body text of the external ad.
         *
         * @param body the body, or {@code null} to clear it
         */
        public void setBody(String body) {
            this.body = body;
    }

        /**
         * Sets the type of media attached to the external ad.
         *
         * @param mediaType the {@link MediaType}, or {@code null} to
         *                  clear it
         */
        public void setMediaType(MediaType mediaType) {
            this.mediaType = mediaType;
    }

        /**
         * Sets the URL of the thumbnail associated with the ad creative.
         *
         * @param thumbnailUrl the thumbnail URL, or {@code null} to
         *                     clear it
         */
        public void setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
    }

        /**
         * Sets the URL of the full-resolution media attached to the ad.
         *
         * @param mediaUrl the media URL, or {@code null} to clear it
         */
        public void setMediaUrl(String mediaUrl) {
            this.mediaUrl = mediaUrl;
    }

        /**
         * Sets the inline thumbnail bytes.
         *
         * @param thumbnail the thumbnail bytes, or {@code null} to clear
         *                  them
         */
        public void setThumbnail(byte[] thumbnail) {
            this.thumbnail = thumbnail;
    }

        /**
         * Sets the identifier of the surface that served the ad.
         *
         * @param sourceType the source type, or {@code null} to clear it
         */
        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
    }

        /**
         * Sets the identifier of the ad unit within the source surface.
         *
         * @param sourceId the source id, or {@code null} to clear it
         */
        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
    }

        /**
         * Sets the deep-link URL of the original ad.
         *
         * @param sourceUrl the source URL, or {@code null} to clear it
         */
        public void setSourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
    }

        /**
         * Sets whether the business account has an automated greeting
         * enabled.
         *
         * @param containsAutoReply the auto-reply flag, or {@code null}
         *                          to clear it
         */
        public void setContainsAutoReply(Boolean containsAutoReply) {
            this.containsAutoReply = containsAutoReply;
    }

        /**
         * Sets whether the ad preview must be rendered with a larger
         * thumbnail.
         *
         * @param renderLargerThumbnail the flag, or {@code null} to
         *                              clear it
         */
        public void setRenderLargerThumbnail(Boolean renderLargerThumbnail) {
            this.renderLargerThumbnail = renderLargerThumbnail;
    }

        /**
         * Sets whether the ad attribution banner must be shown above the
         * message.
         *
         * @param showAdAttribution the flag, or {@code null} to clear it
         */
        public void setShowAdAttribution(Boolean showAdAttribution) {
            this.showAdAttribution = showAdAttribution;
    }

        /**
         * Sets the Click-To-WhatsApp click identifier.
         *
         * @param ctwaClid the click id, or {@code null} to clear it
         */
        public void setCtwaClid(String ctwaClid) {
            this.ctwaClid = ctwaClid;
    }

        /**
         * Sets the ad reference string.
         *
         * @param ref the ref string, or {@code null} to clear it
         */
        public void setRef(String ref) {
            this.ref = ref;
    }

        /**
         * Sets whether the ad produced a Click-To-WhatsApp voice call.
         *
         * @param clickToWhatsappCall the call flag, or {@code null} to
         *                            clear it
         */
        public void setClickToWhatsappCall(Boolean clickToWhatsappCall) {
            this.clickToWhatsappCall = clickToWhatsappCall;
    }

        /**
         * Sets whether the ad context preview has been dismissed by the
         * user.
         *
         * @param adContextPreviewDismissed the dismissed flag, or
         *                                  {@code null} to clear it
         */
        public void setAdContextPreviewDismissed(Boolean adContextPreviewDismissed) {
            this.adContextPreviewDismissed = adContextPreviewDismissed;
    }

        /**
         * Sets the identifier of the application that served the ad.
         *
         * @param sourceApp the source app, or {@code null} to clear it
         */
        public void setSourceApp(String sourceApp) {
            this.sourceApp = sourceApp;
    }

        /**
         * Sets whether the automated greeting message has been shown.
         *
         * @param automatedGreetingMessageShown the flag, or {@code null}
         *                                      to clear it
         */
        public void setAutomatedGreetingMessageShown(Boolean automatedGreetingMessageShown) {
            this.automatedGreetingMessageShown = automatedGreetingMessageShown;
    }

        /**
         * Sets the body text of the automated greeting message.
         *
         * @param greetingMessageBody the greeting body, or {@code null}
         *                            to clear it
         */
        public void setGreetingMessageBody(String greetingMessageBody) {
            this.greetingMessageBody = greetingMessageBody;
    }

        /**
         * Sets the opaque payload attached to the call-to-action of the
         * ad.
         *
         * @param ctaPayload the CTA payload, or {@code null} to clear it
         */
        public void setCtaPayload(String ctaPayload) {
            this.ctaPayload = ctaPayload;
    }

        /**
         * Sets whether the reply nudge must be suppressed.
         *
         * @param disableNudge the nudge-disable flag, or {@code null} to
         *                     clear it
         */
        public void setDisableNudge(Boolean disableNudge) {
            this.disableNudge = disableNudge;
    }

        /**
         * Sets the URL of the original unprocessed image for the ad
         * creative.
         *
         * @param originalImageUrl the original image URL, or
         *                         {@code null} to clear it
         */
        public void setOriginalImageUrl(String originalImageUrl) {
            this.originalImageUrl = originalImageUrl;
    }

        /**
         * Sets the identifier of the CTA type displayed in the automated
         * greeting message.
         *
         * @param automatedGreetingMessageCtaType the CTA type id, or
         *                                        {@code null} to clear it
         */
        public void setAutomatedGreetingMessageCtaType(String automatedGreetingMessageCtaType) {
            this.automatedGreetingMessageCtaType = automatedGreetingMessageCtaType;
    }

        /**
         * Sets whether the ad uses the WhatsApp-to-WhatsApp format.
         *
         * @param wtwaAdFormat the flag, or {@code null} to clear it
         */
        public void setWtwaAdFormat(Boolean wtwaAdFormat) {
            this.wtwaAdFormat = wtwaAdFormat;
    }

        /**
         * Sets the type of the ad.
         *
         * @param adType the {@link AdType}, or {@code null} to clear it
         */
        public void setAdType(AdType adType) {
            this.adType = adType;
    }

        /**
         * Sets the URL of the advertiser website.
         *
         * @param wtwaWebsiteUrl the website URL, or {@code null} to
         *                       clear it
         */
        public void setWtwaWebsiteUrl(String wtwaWebsiteUrl) {
            this.wtwaWebsiteUrl = wtwaWebsiteUrl;
    }

        /**
         * Sets the URL used to render a live preview of the ad in the
         * chat.
         *
         * @param adPreviewUrl the preview URL, or {@code null} to clear
         *                     it
         */
        public void setAdPreviewUrl(String adPreviewUrl) {
            this.adPreviewUrl = adPreviewUrl;
    }

        /**
         * Enumerates the supported external ad delivery formats.
         */
        @ProtobufEnum(name = "ContextInfo.ExternalAdReplyInfo.AdType")
        public static enum AdType {
            /**
             * Click-To-WhatsApp ad (the user clicks the ad to open a
             * WhatsApp chat).
             */
            CTWA(0),
            /**
             * Call-as-web-click ad (the click on the ad initiates a
             * WhatsApp voice or video call).
             */
            CAWC(1);

            /**
             * Constructs an ad type with the given protobuf index.
             *
             * @param index the protobuf wire index
             */
            AdType(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf wire index associated with this constant.
             */
            final int index;

            /**
             * Returns the protobuf wire index of this constant.
             *
             * @return the protobuf index
             */
            public int index() {
                return this.index;
            }
        }

        /**
         * Enumerates the possible media kinds for an external ad.
         */
        @ProtobufEnum(name = "ContextInfo.ExternalAdReplyInfo.MediaType")
        public static enum MediaType {
            /**
             * The ad carries no media.
             */
            NONE(0),
            /**
             * The ad carries an image.
             */
            IMAGE(1),
            /**
             * The ad carries a video.
             */
            VIDEO(2);

            /**
             * Constructs a media type with the given protobuf index.
             *
             * @param index the protobuf wire index
             */
            MediaType(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf wire index associated with this constant.
             */
            final int index;

            /**
             * Returns the protobuf wire index of this constant.
             *
             * @return the protobuf index
             */
            public int index() {
                return this.index;
            }
        }
    }

    /**
     * Declares which interactive features are available on a message.
     *
     * <p>The flags carried here let the receiving client know whether
     * reactions, ranking, feedback, resharing and multi-reactions are
     * enabled for the associated message. They are typically populated by
     * the server based on the sender's capabilities and chat settings.
     */
    @ProtobufMessage(name = "ContextInfo.FeatureEligibilities")
    public static final class FeatureEligibilities {
        /**
         * Whether reactions are forbidden on this message.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        Boolean cannotBeReactedTo;

        /**
         * Whether the message must be excluded from the ranking algorithm.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
        Boolean cannotBeRanked;

        /**
         * Whether users can request feedback on the content of this
         * message.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
        Boolean canRequestFeedback;

        /**
         * Whether this message can be reshared to other chats or status.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
        Boolean canBeReshared;

        /**
         * Whether this message can accept multiple reactions from the
         * same user.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
        Boolean canReceiveMultiReact;


        /**
         * Constructs a {@code FeatureEligibilities} instance.
         *
         * @param cannotBeReactedTo    see {@link #cannotBeReactedTo()}
         * @param cannotBeRanked       see {@link #cannotBeRanked()}
         * @param canRequestFeedback   see {@link #canRequestFeedback()}
         * @param canBeReshared        see {@link #canBeReshared()}
         * @param canReceiveMultiReact see {@link #canReceiveMultiReact()}
         */
        FeatureEligibilities(Boolean cannotBeReactedTo, Boolean cannotBeRanked, Boolean canRequestFeedback, Boolean canBeReshared, Boolean canReceiveMultiReact) {
            this.cannotBeReactedTo = cannotBeReactedTo;
            this.cannotBeRanked = cannotBeRanked;
            this.canRequestFeedback = canRequestFeedback;
            this.canBeReshared = canBeReshared;
            this.canReceiveMultiReact = canReceiveMultiReact;
        }

        /**
         * Returns whether reactions are forbidden on this message.
         *
         * @return {@code true} if reactions are forbidden, {@code false}
         *         otherwise
         */
        public boolean cannotBeReactedTo() {
            return cannotBeReactedTo != null && cannotBeReactedTo;
        }

        /**
         * Returns whether the message must be excluded from ranking.
         *
         * @return {@code true} if excluded from ranking, {@code false}
         *         otherwise
         */
        public boolean cannotBeRanked() {
            return cannotBeRanked != null && cannotBeRanked;
        }

        /**
         * Returns whether users can request feedback on this message.
         *
         * @return {@code true} if feedback can be requested,
         *         {@code false} otherwise
         */
        public boolean canRequestFeedback() {
            return canRequestFeedback != null && canRequestFeedback;
        }

        /**
         * Returns whether this message can be reshared.
         *
         * @return {@code true} if the message can be reshared,
         *         {@code false} otherwise
         */
        public boolean canBeReshared() {
            return canBeReshared != null && canBeReshared;
        }

        /**
         * Returns whether this message accepts multiple reactions from
         * the same user.
         *
         * @return {@code true} if multi-react is enabled, {@code false}
         *         otherwise
         */
        public boolean canReceiveMultiReact() {
            return canReceiveMultiReact != null && canReceiveMultiReact;
        }

        /**
         * Sets whether reactions are forbidden on this message.
         *
         * @param cannotBeReactedTo the flag, or {@code null} to clear it
         */
        public void setCannotBeReactedTo(Boolean cannotBeReactedTo) {
            this.cannotBeReactedTo = cannotBeReactedTo;
    }

        /**
         * Sets whether the message must be excluded from ranking.
         *
         * @param cannotBeRanked the flag, or {@code null} to clear it
         */
        public void setCannotBeRanked(Boolean cannotBeRanked) {
            this.cannotBeRanked = cannotBeRanked;
    }

        /**
         * Sets whether users can request feedback on this message.
         *
         * @param canRequestFeedback the flag, or {@code null} to clear
         *                           it
         */
        public void setCanRequestFeedback(Boolean canRequestFeedback) {
            this.canRequestFeedback = canRequestFeedback;
    }

        /**
         * Sets whether this message can be reshared.
         *
         * @param canBeReshared the flag, or {@code null} to clear it
         */
        public void setCanBeReshared(Boolean canBeReshared) {
            this.canBeReshared = canBeReshared;
    }

        /**
         * Sets whether this message accepts multiple reactions from the
         * same user.
         *
         * @param canReceiveMultiReact the flag, or {@code null} to clear
         *                             it
         */
        public void setCanReceiveMultiReact(Boolean canReceiveMultiReact) {
            this.canReceiveMultiReact = canReceiveMultiReact;
    }
    }

    /**
     * Provenance information attached to a message that forwards a
     * newsletter post into a regular chat.
     *
     * <p>The receiver uses this data to render a newsletter attribution
     * header (name, profile picture reference, content type) and to
     * deep-link back to the original newsletter when the user taps the
     * header.
     *
     * <p>This type also implements {@link InteractiveAction}, so it can be
     * referenced as the target action of an interactive link preview.
     */
    @ProtobufMessage(name = "ContextInfo.ForwardedNewsletterMessageInfo")
    public static final class ForwardedNewsletterMessageInfo implements InteractiveAction {
        /**
         * JID of the newsletter that originated the forwarded message.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid newsletterJid;

        /**
         * Server-assigned identifier of the original newsletter message.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.INT32)
        Integer serverMessageId;

        /**
         * Display name of the newsletter that originated the message.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String newsletterName;

        /**
         * Type of newsletter content being forwarded.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
        ForwardedNewsletterMessageInfo.ContentType contentType;

        /**
         * Textual description used by assistive technologies to announce
         * the forwarded newsletter post.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        String accessibilityText;

        /**
         * Display name of the newsletter profile at the time the forward
         * was created.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        String profileName;


        /**
         * Constructs a {@code ForwardedNewsletterMessageInfo}.
         *
         * @param newsletterJid     see {@link #newsletterJid()}
         * @param serverMessageId   see {@link #serverMessageId()}
         * @param newsletterName    see {@link #newsletterName()}
         * @param contentType       see {@link #contentType()}
         * @param accessibilityText see {@link #accessibilityText()}
         * @param profileName       see {@link #profileName()}
         */
        ForwardedNewsletterMessageInfo(Jid newsletterJid, Integer serverMessageId, String newsletterName, ContentType contentType, String accessibilityText, String profileName) {
            this.newsletterJid = newsletterJid;
            this.serverMessageId = serverMessageId;
            this.newsletterName = newsletterName;
            this.contentType = contentType;
            this.accessibilityText = accessibilityText;
            this.profileName = profileName;
        }

        /**
         * Returns the JID of the newsletter that originated the forwarded
         * message.
         *
         * @return an {@code Optional} containing the newsletter JID, or
         *         empty if not set
         */
        public Optional<Jid> newsletterJid() {
            return Optional.ofNullable(newsletterJid);
        }

        /**
         * Returns the server-assigned identifier of the original
         * newsletter message.
         *
         * @return the server message id, or empty if not set
         */
        public OptionalInt serverMessageId() {
            return serverMessageId == null ? OptionalInt.empty() : OptionalInt.of(serverMessageId);
        }

        /**
         * Returns the display name of the newsletter that originated the
         * message.
         *
         * @return an {@code Optional} containing the newsletter name, or
         *         empty if not set
         */
        public Optional<String> newsletterName() {
            return Optional.ofNullable(newsletterName);
        }

        /**
         * Returns the type of newsletter content being forwarded.
         *
         * @return an {@code Optional} containing the {@link ContentType},
         *         or empty if not set
         */
        public Optional<ContentType> contentType() {
            return Optional.ofNullable(contentType);
        }

        /**
         * Returns the accessibility description for the forwarded
         * newsletter post.
         *
         * @return an {@code Optional} containing the accessibility text,
         *         or empty if not set
         */
        public Optional<String> accessibilityText() {
            return Optional.ofNullable(accessibilityText);
        }

        /**
         * Returns the display name of the newsletter profile at the time
         * the forward was created.
         *
         * @return an {@code Optional} containing the profile name, or
         *         empty if not set
         */
        public Optional<String> profileName() {
            return Optional.ofNullable(profileName);
        }

        /**
         * Sets the JID of the newsletter that originated the forwarded
         * message.
         *
         * @param newsletterJid the newsletter JID, or {@code null} to
         *                      clear it
         */
        public void setNewsletterJid(Jid newsletterJid) {
            this.newsletterJid = newsletterJid;
    }

        /**
         * Sets the server-assigned identifier of the original newsletter
         * message.
         *
         * @param serverMessageId the server message id, or {@code null}
         *                        to clear it
         */
        public void setServerMessageId(Integer serverMessageId) {
            this.serverMessageId = serverMessageId;
    }

        /**
         * Sets the display name of the newsletter.
         *
         * @param newsletterName the newsletter name, or {@code null} to
         *                       clear it
         */
        public void setNewsletterName(String newsletterName) {
            this.newsletterName = newsletterName;
    }

        /**
         * Sets the type of newsletter content being forwarded.
         *
         * @param contentType the {@link ContentType}, or {@code null} to
         *                    clear it
         */
        public void setContentType(ContentType contentType) {
            this.contentType = contentType;
    }

        /**
         * Sets the accessibility description for the forwarded newsletter
         * post.
         *
         * @param accessibilityText the accessibility text, or
         *                          {@code null} to clear it
         */
        public void setAccessibilityText(String accessibilityText) {
            this.accessibilityText = accessibilityText;
    }

        /**
         * Sets the display name of the newsletter profile.
         *
         * @param profileName the profile name, or {@code null} to clear
         *                    it
         */
        public void setProfileName(String profileName) {
            this.profileName = profileName;
    }

        /**
         * Enumerates the possible layouts used to render a forwarded
         * newsletter post.
         */
        @ProtobufEnum(name = "ContextInfo.ForwardedNewsletterMessageInfo.ContentType")
        public static enum ContentType {
            /**
             * Plain newsletter update.
             */
            UPDATE(1),
            /**
             * Newsletter update rendered as a richer card.
             */
            UPDATE_CARD(2),
            /**
             * Link preview card shared through a newsletter.
             */
            LINK_CARD(3);

            /**
             * Constructs a content type with the given protobuf index.
             *
             * @param index the protobuf wire index
             */
            ContentType(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf wire index associated with this constant.
             */
            final int index;

            /**
             * Returns the protobuf wire index of this constant.
             *
             * @return the protobuf index
             */
            public int index() {
                return this.index;
            }
        }
    }

    /**
     * Bundles a quoted question and its response when a message is a
     * reply inside a question-and-answer flow.
     *
     * <p>WhatsApp uses this payload to render a two-level reply bubble
     * that shows both the question the user originally asked and the
     * response that is being replied to.
     */
    @ProtobufMessage(name = "ContextInfo.QuestionReplyQuotedMessage")
    public static final class QuestionReplyQuotedMessage {
        /**
         * Server-assigned identifier of the original question.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.INT32)
        Integer serverQuestionId;

        /**
         * Content of the quoted question.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        LinkedMessageContainer quotedQuestion;

        /**
         * Content of the quoted response to the question.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        LinkedMessageContainer quotedResponse;


        /**
         * Constructs a {@code QuestionReplyQuotedMessage}.
         *
         * @param serverQuestionId see {@link #serverQuestionId()}
         * @param quotedQuestion   see {@link #quotedQuestion()}
         * @param quotedResponse   see {@link #quotedResponse()}
         */
        QuestionReplyQuotedMessage(Integer serverQuestionId, LinkedMessageContainer quotedQuestion, LinkedMessageContainer quotedResponse) {
            this.serverQuestionId = serverQuestionId;
            this.quotedQuestion = quotedQuestion;
            this.quotedResponse = quotedResponse;
        }

        /**
         * Returns the server-assigned identifier of the original
         * question.
         *
         * @return the question id, or empty if not set
         */
        public OptionalInt serverQuestionId() {
            return serverQuestionId == null ? OptionalInt.empty() : OptionalInt.of(serverQuestionId);
        }

        /**
         * Returns the content of the quoted question.
         *
         * @return an {@code Optional} containing the question content, or
         *         empty if not set
         */
        public Optional<LinkedMessageContainer> quotedQuestion() {
            return Optional.ofNullable(quotedQuestion);
        }

        /**
         * Returns the content of the quoted response.
         *
         * @return an {@code Optional} containing the response content, or
         *         empty if not set
         */
        public Optional<LinkedMessageContainer> quotedResponse() {
            return Optional.ofNullable(quotedResponse);
        }

        /**
         * Sets the server-assigned identifier of the original question.
         *
         * @param serverQuestionId the question id, or {@code null} to
         *                         clear it
         */
        public void setServerQuestionId(Integer serverQuestionId) {
            this.serverQuestionId = serverQuestionId;
    }

        /**
         * Sets the content of the quoted question.
         *
         * @param quotedQuestion the question content, or {@code null} to
         *                       clear it
         */
        public void setQuotedQuestion(LinkedMessageContainer quotedQuestion) {
            this.quotedQuestion = quotedQuestion;
    }

        /**
         * Sets the content of the quoted response.
         *
         * @param quotedResponse the response content, or {@code null} to
         *                       clear it
         */
        public void setQuotedResponse(LinkedMessageContainer quotedResponse) {
            this.quotedResponse = quotedResponse;
    }
    }

    /**
     * Audience configuration attached to a status update describing who
     * is allowed to see it.
     *
     * <p>When the sender restricts a status update to a custom audience
     * (for example the close friends list), this metadata carries the
     * audience type and the display decorations (list name and emoji) so
     * the sender's client can render an accurate header on the published
     * status.
     */
    @ProtobufMessage(name = "ContextInfo.StatusAudienceMetadata")
    public static final class StatusAudienceMetadata {
        /**
         * Type of audience targeted by the status update.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        StatusAudienceMetadata.AudienceType audienceType;

        /**
         * Display name of the custom audience list (for example the close
         * friends list name).
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String listName;

        /**
         * Emoji used to decorate the audience list header.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String listEmoji;


        /**
         * Constructs a {@code StatusAudienceMetadata} instance.
         *
         * @param audienceType see {@link #audienceType()}
         * @param listName     see {@link #listName()}
         * @param listEmoji    see {@link #listEmoji()}
         */
        StatusAudienceMetadata(AudienceType audienceType, String listName, String listEmoji) {
            this.audienceType = audienceType;
            this.listName = listName;
            this.listEmoji = listEmoji;
        }

        /**
         * Returns the type of audience targeted by the status update.
         *
         * @return an {@code Optional} containing the {@link AudienceType},
         *         or empty if not set
         */
        public Optional<AudienceType> audienceType() {
            return Optional.ofNullable(audienceType);
        }

        /**
         * Returns the display name of the custom audience list.
         *
         * @return an {@code Optional} containing the list name, or empty
         *         if not set
         */
        public Optional<String> listName() {
            return Optional.ofNullable(listName);
        }

        /**
         * Returns the emoji used to decorate the audience list header.
         *
         * @return an {@code Optional} containing the list emoji, or
         *         empty if not set
         */
        public Optional<String> listEmoji() {
            return Optional.ofNullable(listEmoji);
        }

        /**
         * Sets the type of audience targeted by the status update.
         *
         * @param audienceType the {@link AudienceType}, or {@code null}
         *                     to clear it
         */
        public void setAudienceType(AudienceType audienceType) {
            this.audienceType = audienceType;
    }

        /**
         * Sets the display name of the custom audience list.
         *
         * @param listName the list name, or {@code null} to clear it
         */
        public void setListName(String listName) {
            this.listName = listName;
    }

        /**
         * Sets the emoji used to decorate the audience list header.
         *
         * @param listEmoji the list emoji, or {@code null} to clear it
         */
        public void setListEmoji(String listEmoji) {
            this.listEmoji = listEmoji;
    }

        /**
         * Enumerates the recognised audience presets for a status update.
         */
        @ProtobufEnum(name = "ContextInfo.StatusAudienceMetadata.AudienceType")
        public static enum AudienceType {
            /**
             * The audience preset is unknown or unspecified.
             */
            UNKNOWN(0),
            /**
             * The status is restricted to the close friends list.
             */
            CLOSE_FRIENDS(1);

            /**
             * Constructs an audience type with the given protobuf index.
             *
             * @param index the protobuf wire index
             */
            AudienceType(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf wire index associated with this constant.
             */
            final int index;

            /**
             * Returns the protobuf wire index of this constant.
             *
             * @return the protobuf index
             */
            public int index() {
                return this.index;
            }
        }
    }

    /**
     * UTM attribution parameters propagated from a marketing campaign
     * that led to this message.
     *
     * <p>UTM tags follow the common web analytics convention: they
     * identify the traffic source and the campaign name so the business
     * can measure the performance of external promotions that drive
     * users into WhatsApp conversations.
     */
    @ProtobufMessage(name = "ContextInfo.UTMInfo")
    public static final class UTMInfo {
        /**
         * Identifier of the traffic source (the {@code utm_source}
         * parameter).
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String utmSource;

        /**
         * Identifier of the marketing campaign (the
         * {@code utm_campaign} parameter).
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String utmCampaign;


        /**
         * Constructs a {@code UTMInfo} with the given source and
         * campaign identifiers.
         *
         * @param utmSource   see {@link #utmSource()}
         * @param utmCampaign see {@link #utmCampaign()}
         */
        UTMInfo(String utmSource, String utmCampaign) {
            this.utmSource = utmSource;
            this.utmCampaign = utmCampaign;
        }

        /**
         * Returns the identifier of the traffic source.
         *
         * @return an {@code Optional} containing the UTM source, or
         *         empty if not set
         */
        public Optional<String> utmSource() {
            return Optional.ofNullable(utmSource);
        }

        /**
         * Returns the identifier of the marketing campaign.
         *
         * @return an {@code Optional} containing the UTM campaign, or
         *         empty if not set
         */
        public Optional<String> utmCampaign() {
            return Optional.ofNullable(utmCampaign);
        }

        /**
         * Sets the identifier of the traffic source.
         *
         * @param utmSource the UTM source, or {@code null} to clear it
         */
        public void setUtmSource(String utmSource) {
            this.utmSource = utmSource;
    }

        /**
         * Sets the identifier of the marketing campaign.
         *
         * @param utmCampaign the UTM campaign, or {@code null} to clear
         *                    it
         */
        public void setUtmCampaign(String utmCampaign) {
            this.utmCampaign = utmCampaign;
    }
    }
}
