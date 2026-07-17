package com.github.auties00.cobalt.wire.linked.chat;

import com.github.auties00.cobalt.wire.linked.bot.BotMetadata;
import com.github.auties00.cobalt.wire.linked.message.MessageAssociation;
import com.github.auties00.cobalt.wire.linked.message.MessageThreadId;
import com.github.auties00.cobalt.wire.linked.device.DeviceListMetadata;
import com.github.auties00.cobalt.wire.linked.message.media.MessageLinkRender;
import com.github.auties00.cobalt.wire.linked.message.addon.MessageAddOnContextInfo;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Carries protocol-level context about a WhatsApp message that is separate from
 * the message content itself.
 *
 * <p>This protobuf message (wire name {@code MessageContextInfo}) is included
 * alongside the encrypted message payload and contains metadata used by the
 * WhatsApp protocol for device identity verification, message secret
 * management, bot interactions, message add-on lifecycle, and privacy controls.
 *
 * <p>Key fields include:
 * <ul>
 *   <li>{@link #deviceListMetadata()} for ICDC (Identity Change Detection
 *       Consistency) verification during message delivery</li>
 *   <li>{@link #messageSecret()} for the encryption secret used in
 *       view-once and other secret-based message types</li>
 *   <li>{@link #botMetadata()} and {@link #botMessageSecret()} for AI bot
 *       message handling</li>
 *   <li>{@link #limitSharing()} and {@link #limitSharingV2()} for the
 *       privacy limit-sharing state embedded in messages</li>
 * </ul>
 */
@ProtobufMessage(name = "MessageContextInfo")
public final class ChatMessageContextInfo {
    /**
     * Device list metadata used for identity change detection consistency
     * (ICDC). Contains a hash of known identity keys and device list
     * timestamps, allowing recipients to verify that the sender's device
     * list has not changed unexpectedly.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    DeviceListMetadata deviceListMetadata;

    /**
     * The version number of the device list metadata format. Incremented
     * when the ICDC protocol is updated to support new fields or behaviors.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    Integer deviceListMetadataVersion;

    /**
     * The message secret used for encryption of view-once messages, secret
     * messages, and other message types that require a per-message key
     * beyond the standard Signal session encryption.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] messageSecret;

    /**
     * Padding bytes appended to the message payload. Used for message-size
     * obfuscation to prevent traffic analysis from revealing message content
     * characteristics.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] paddingBytes;

    /**
     * The duration in seconds for which a message add-on (such as a pin or
     * keep action) remains active before it expires.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    Integer messageAddOnDurationInSecs;

    /**
     * The encryption secret specific to bot messages, used when the message
     * originates from or is destined for a WhatsApp AI bot.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    byte[] botMessageSecret;

    /**
     * Metadata about the bot interaction, including the bot identity,
     * session information, and rendering configuration.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    BotMetadata botMetadata;

    /**
     * The version of the reporting token format used for this message.
     * Reporting tokens allow users to report messages to WhatsApp without
     * revealing end-to-end encrypted content.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.INT32)
    Integer reportingTokenVersion;

    /**
     * The expiry type for message add-ons attached to this message (for
     * example, whether the add-on expires based on a timer or upon a
     * specific event).
     */
    @ProtobufProperty(index = 9, type = ProtobufType.ENUM)
    MessageAddOnContextInfo.ExpiryType messageAddOnExpiryType;

    /**
     * Describes the association between this message and another message,
     * such as an edit association or a reply association that goes beyond
     * the standard quoted-message context.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    MessageAssociation messageAssociation;

    /**
     * Whether the group in which this message was sent was created via
     * the Cloud API (CAPI). This flag affects how the message is processed
     * and displayed.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    Boolean capiCreatedGroup;

    /**
     * An opaque payload string attached to messages in WhatsApp support
     * conversations, used for routing and analytics.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.STRING)
    String supportPayload;

    /**
     * The first version of the limit sharing privacy state embedded in
     * this message. Controls whether the sender's personal information is
     * restricted from the recipient.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.MESSAGE)
    ChatLimitSharing limitSharing;

    /**
     * The second version of the limit sharing privacy state, introduced to
     * support an updated privacy protocol. When present, this takes
     * precedence over {@link #limitSharing}.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.MESSAGE)
    ChatLimitSharing limitSharingV2;

    /**
     * The list of thread identifiers associated with this message. Used when
     * messages belong to threaded conversations within a chat.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
    List<MessageThreadId> threadId;

    /**
     * Configuration for how web link previews should be rendered for this
     * message, controlling the visual presentation of URL metadata.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.ENUM)
    MessageLinkRender weblinkRenderConfig;

    /**
     * Constructs a new {@code ChatMessageContextInfo} with the specified values.
     *
     * @param deviceListMetadata         the device list metadata for ICDC
     * @param deviceListMetadataVersion  the metadata format version
     * @param messageSecret              the per-message encryption secret
     * @param paddingBytes               the message padding bytes
     * @param messageAddOnDurationInSecs the add-on duration in seconds
     * @param botMessageSecret           the bot-specific encryption secret
     * @param botMetadata                the bot interaction metadata
     * @param reportingTokenVersion      the reporting token format version
     * @param messageAddOnExpiryType     the add-on expiry type
     * @param messageAssociation         the message association descriptor
     * @param capiCreatedGroup           whether the group was CAPI-created
     * @param supportPayload             the support conversation payload
     * @param limitSharing               the v1 limit sharing state
     * @param limitSharingV2             the v2 limit sharing state
     * @param threadId                   the thread identifiers
     * @param weblinkRenderConfig        the web link rendering configuration
     */
    ChatMessageContextInfo(DeviceListMetadata deviceListMetadata, Integer deviceListMetadataVersion, byte[] messageSecret, byte[] paddingBytes, Integer messageAddOnDurationInSecs, byte[] botMessageSecret, BotMetadata botMetadata, Integer reportingTokenVersion, MessageAddOnContextInfo.ExpiryType messageAddOnExpiryType, MessageAssociation messageAssociation, Boolean capiCreatedGroup, String supportPayload, ChatLimitSharing limitSharing, ChatLimitSharing limitSharingV2, List<MessageThreadId> threadId, MessageLinkRender weblinkRenderConfig) {
        this.deviceListMetadata = deviceListMetadata;
        this.deviceListMetadataVersion = deviceListMetadataVersion;
        this.messageSecret = messageSecret;
        this.paddingBytes = paddingBytes;
        this.messageAddOnDurationInSecs = messageAddOnDurationInSecs;
        this.botMessageSecret = botMessageSecret;
        this.botMetadata = botMetadata;
        this.reportingTokenVersion = reportingTokenVersion;
        this.messageAddOnExpiryType = messageAddOnExpiryType;
        this.messageAssociation = messageAssociation;
        this.capiCreatedGroup = capiCreatedGroup;
        this.supportPayload = supportPayload;
        this.limitSharing = limitSharing;
        this.limitSharingV2 = limitSharingV2;
        this.threadId = threadId;
        this.weblinkRenderConfig = weblinkRenderConfig;
    }

    /**
     * Returns the device list metadata for ICDC verification.
     *
     * @return an {@link Optional} containing the device list metadata, or
     *         empty if not present
     */
    public Optional<DeviceListMetadata> deviceListMetadata() {
        return Optional.ofNullable(deviceListMetadata);
    }

    /**
     * Returns the version of the device list metadata format.
     *
     * @return an {@link OptionalInt} containing the version, or empty if
     *         not set
     */
    public OptionalInt deviceListMetadataVersion() {
        return deviceListMetadataVersion == null ? OptionalInt.empty() : OptionalInt.of(deviceListMetadataVersion);
    }

    /**
     * Returns the per-message encryption secret.
     *
     * @return an {@link Optional} containing the secret bytes, or empty if
     *         not present
     */
    public Optional<byte[]> messageSecret() {
        return Optional.ofNullable(messageSecret);
    }

    /**
     * Returns the padding bytes used for message-size obfuscation.
     *
     * @return an {@link Optional} containing the padding bytes, or empty if
     *         not present
     */
    public Optional<byte[]> paddingBytes() {
        return Optional.ofNullable(paddingBytes);
    }

    /**
     * Returns the duration in seconds for which a message add-on remains active.
     *
     * @return an {@link OptionalInt} containing the duration in seconds, or
     *         empty if not set
     */
    public OptionalInt messageAddOnDurationInSecs() {
        return messageAddOnDurationInSecs == null ? OptionalInt.empty() : OptionalInt.of(messageAddOnDurationInSecs);
    }

    /**
     * Returns the bot-specific encryption secret.
     *
     * @return an {@link Optional} containing the bot secret bytes, or empty
     *         if not present
     */
    public Optional<byte[]> botMessageSecret() {
        return Optional.ofNullable(botMessageSecret);
    }

    /**
     * Returns the metadata about the bot interaction for this message.
     *
     * @return an {@link Optional} containing the bot metadata, or empty if
     *         this is not a bot message
     */
    public Optional<BotMetadata> botMetadata() {
        return Optional.ofNullable(botMetadata);
    }

    /**
     * Returns the version of the reporting token format.
     *
     * @return an {@link OptionalInt} containing the version, or empty if
     *         not set
     */
    public OptionalInt reportingTokenVersion() {
        return reportingTokenVersion == null ? OptionalInt.empty() : OptionalInt.of(reportingTokenVersion);
    }

    /**
     * Returns the expiry type for message add-ons on this message.
     *
     * @return an {@link Optional} containing the expiry type, or empty if
     *         not set
     */
    public Optional<MessageAddOnContextInfo.ExpiryType> messageAddOnExpiryType() {
        return Optional.ofNullable(messageAddOnExpiryType);
    }

    /**
     * Returns the message association descriptor linking this message to
     * another message.
     *
     * @return an {@link Optional} containing the association, or empty if
     *         not present
     */
    public Optional<MessageAssociation> messageAssociation() {
        return Optional.ofNullable(messageAssociation);
    }

    /**
     * Returns whether the group was created via the Cloud API (CAPI).
     *
     * @return {@code true} if the group was CAPI-created, {@code false}
     *         otherwise
     */
    public boolean capiCreatedGroup() {
        return capiCreatedGroup != null && capiCreatedGroup;
    }

    /**
     * Returns the opaque support conversation payload.
     *
     * @return an {@link Optional} containing the payload string, or empty
     *         if not present
     */
    public Optional<String> supportPayload() {
        return Optional.ofNullable(supportPayload);
    }

    /**
     * Returns the first version of the limit sharing privacy state.
     *
     * @return an {@link Optional} containing the limit sharing state, or
     *         empty if not present
     */
    public Optional<ChatLimitSharing> limitSharing() {
        return Optional.ofNullable(limitSharing);
    }

    /**
     * Returns the second version of the limit sharing privacy state.
     *
     * @return an {@link Optional} containing the v2 limit sharing state, or
     *         empty if not present
     */
    public Optional<ChatLimitSharing> limitSharingV2() {
        return Optional.ofNullable(limitSharingV2);
    }

    /**
     * Returns the thread identifiers associated with this message.
     *
     * @return an unmodifiable list of thread identifiers, never {@code null}
     */
    public List<MessageThreadId> threadId() {
        return threadId == null ? List.of() : Collections.unmodifiableList(threadId);
    }

    /**
     * Returns the web link rendering configuration for this message.
     *
     * @return an {@link Optional} containing the render config, or empty if
     *         not set
     */
    public Optional<MessageLinkRender> weblinkRenderConfig() {
        return Optional.ofNullable(weblinkRenderConfig);
    }

    /**
     * Sets the device list metadata for ICDC verification.
     *
     * @param deviceListMetadata the device list metadata, or {@code null} to clear
     */
    public void setDeviceListMetadata(DeviceListMetadata deviceListMetadata) {
        this.deviceListMetadata = deviceListMetadata;
    }

    /**
     * Sets the version of the device list metadata format.
     *
     * @param deviceListMetadataVersion the version, or {@code null} to clear
     */
    public void setDeviceListMetadataVersion(Integer deviceListMetadataVersion) {
        this.deviceListMetadataVersion = deviceListMetadataVersion;
    }

    /**
     * Sets the per-message encryption secret.
     *
     * @param messageSecret the secret bytes, or {@code null} to clear
     */
    public void setMessageSecret(byte[] messageSecret) {
        this.messageSecret = messageSecret;
    }

    /**
     * Sets the padding bytes for message-size obfuscation.
     *
     * @param paddingBytes the padding bytes, or {@code null} to clear
     */
    public void setPaddingBytes(byte[] paddingBytes) {
        this.paddingBytes = paddingBytes;
    }

    /**
     * Sets the duration in seconds for message add-on activity.
     *
     * @param messageAddOnDurationInSecs the duration, or {@code null} to clear
     */
    public void setMessageAddOnDurationInSecs(Integer messageAddOnDurationInSecs) {
        this.messageAddOnDurationInSecs = messageAddOnDurationInSecs;
    }

    /**
     * Sets the bot-specific encryption secret.
     *
     * @param botMessageSecret the bot secret bytes, or {@code null} to clear
     */
    public void setBotMessageSecret(byte[] botMessageSecret) {
        this.botMessageSecret = botMessageSecret;
    }

    /**
     * Sets the bot interaction metadata.
     *
     * @param botMetadata the bot metadata, or {@code null} to clear
     */
    public void setBotMetadata(BotMetadata botMetadata) {
        this.botMetadata = botMetadata;
    }

    /**
     * Sets the reporting token format version.
     *
     * @param reportingTokenVersion the version, or {@code null} to clear
     */
    public void setReportingTokenVersion(Integer reportingTokenVersion) {
        this.reportingTokenVersion = reportingTokenVersion;
    }

    /**
     * Sets the expiry type for message add-ons.
     *
     * @param messageAddOnExpiryType the expiry type, or {@code null} to clear
     */
    public void setMessageAddOnExpiryType(MessageAddOnContextInfo.ExpiryType messageAddOnExpiryType) {
        this.messageAddOnExpiryType = messageAddOnExpiryType;
    }

    /**
     * Sets the message association descriptor.
     *
     * @param messageAssociation the association, or {@code null} to clear
     */
    public void setMessageAssociation(MessageAssociation messageAssociation) {
        this.messageAssociation = messageAssociation;
    }

    /**
     * Sets whether the group was created via the Cloud API.
     *
     * @param capiCreatedGroup {@code true} if CAPI-created, or {@code null}
     *                         to clear
     */
    public void setCapiCreatedGroup(Boolean capiCreatedGroup) {
        this.capiCreatedGroup = capiCreatedGroup;
    }

    /**
     * Sets the support conversation payload.
     *
     * @param supportPayload the payload string, or {@code null} to clear
     */
    public void setSupportPayload(String supportPayload) {
        this.supportPayload = supportPayload;
    }

    /**
     * Sets the first version of the limit sharing privacy state.
     *
     * @param limitSharing the limit sharing state, or {@code null} to clear
     */
    public void setLimitSharing(ChatLimitSharing limitSharing) {
        this.limitSharing = limitSharing;
    }

    /**
     * Sets the second version of the limit sharing privacy state.
     *
     * @param limitSharingV2 the v2 limit sharing state, or {@code null} to clear
     */
    public void setLimitSharingV2(ChatLimitSharing limitSharingV2) {
        this.limitSharingV2 = limitSharingV2;
    }

    /**
     * Sets the thread identifiers for this message.
     *
     * @param threadId the list of thread identifiers, or {@code null} to clear
     */
    public void setThreadId(List<MessageThreadId> threadId) {
        this.threadId = threadId;
    }

    /**
     * Sets the web link rendering configuration.
     *
     * @param weblinkRenderConfig the render config, or {@code null} to clear
     */
    public void setWeblinkRenderConfig(MessageLinkRender weblinkRenderConfig) {
        this.weblinkRenderConfig = weblinkRenderConfig;
    }

}
