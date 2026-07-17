package com.github.auties00.cobalt.wire.linked.message.call;

import com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A message that signals an incoming call offer inside a chat.
 *
 * <p>A call offer is the protocol level invitation the caller sends to the callee when a call is
 * initiated. It carries the cryptographic material needed to negotiate the end-to-end encrypted
 * media streams together with optional metadata coming from Click To WhatsApp (CTWA) advertising
 * campaigns, native flow buttons and deep links that triggered the call.
 *
 * <p>Because a call offer can be sent as a reply to an existing message or from a contextual
 * surface (such as a business catalog), it implements {@link ContextualMessage} and exposes the
 * standard {@link ContextInfo} plus the chat-specific {@link ChatMessageContextInfo}.
 */
@ProtobufMessage(name = "Message.Call")
public final class CallOfferMessage implements ContextualMessage {
    /**
     * Raw key material used during the call's Signal based key agreement.
     *
     * <p>The call key is consumed by the client to derive the symmetric keys that protect the
     * call's media streams. It must be kept confidential.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] callKey;

    /**
     * Identifier of the surface that converted into this call.
     *
     * <p>Populated for calls initiated from advertising flows (such as Click To WhatsApp) or
     * native flow buttons. The value describes where the call was started from.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String conversionSource;

    /**
     * Opaque payload associated with the conversion source.
     *
     * <p>Carries attribution data tied to the surface identified by {@link #conversionSource} and
     * is used by the server for campaign tracking. It is treated as an opaque blob by clients.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] conversionData;

    /**
     * Delay, in seconds, between the conversion event and the moment the call was placed.
     *
     * <p>Used for attribution analytics to relate the user's interaction with an ad or entry
     * surface to the actual call initiation.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    Integer conversionDelaySeconds;

    /**
     * Click To WhatsApp signals attached to the call.
     *
     * <p>Populated when the call originates from a Click To WhatsApp advertising entry point.
     * The string is an opaque payload that the server consumes for ad attribution.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String ctwaSignals;

    /**
     * Additional binary payload attached to Click To WhatsApp originated calls.
     *
     * <p>Carries metadata such as the advertiser identifier or campaign data. Clients treat it as
     * an opaque blob.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    byte[] ctwaPayload;

    /**
     * Standard message context information carried by this call offer.
     *
     * <p>Conveys quoted message data, mentions and other metadata shared with all
     * {@link ContextualMessage} variants.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * Opaque payload associated with a native flow button that triggered the call.
     *
     * <p>Used when the call is initiated by tapping a call-to-action button inside a business
     * native flow. The payload is interpreted by the business server.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String nativeFlowCallButtonPayload;

    /**
     * Opaque payload associated with a deeplink that triggered the call.
     *
     * <p>Populated when the call was launched from a {@code wa.me} or custom scheme URL that
     * carried attribution or routing information.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String deeplinkPayload;

    /**
     * Chat specific context information attached to this call offer.
     *
     * <p>Complements {@link #contextInfo} with data that is only meaningful when the message is
     * rendered inside a chat conversation, such as the message secret used for ephemeral messages.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    ChatMessageContextInfo messageContextInfo;

    /**
     * Constructs a new call offer message with the given cryptographic and attribution data.
     *
     * <p>This constructor is package-private: use the generated {@code CallOfferMessageBuilder}
     * to create instances.
     *
     * @param callKey                     the raw key material used in the call's key agreement, may be {@code null}
     * @param conversionSource            the identifier of the surface that converted into this call, may be {@code null}
     * @param conversionData              the opaque payload tied to the conversion source, may be {@code null}
     * @param conversionDelaySeconds      the delay in seconds between conversion and call, may be {@code null}
     * @param ctwaSignals                 the Click To WhatsApp signals, may be {@code null}
     * @param ctwaPayload                 the Click To WhatsApp binary payload, may be {@code null}
     * @param contextInfo                 the standard message {@link ContextInfo}, may be {@code null}
     * @param nativeFlowCallButtonPayload the native flow button payload, may be {@code null}
     * @param deeplinkPayload             the deeplink payload, may be {@code null}
     * @param messageContextInfo          the chat-specific {@link ChatMessageContextInfo}, may be {@code null}
     */
    CallOfferMessage(byte[] callKey, String conversionSource, byte[] conversionData, Integer conversionDelaySeconds, String ctwaSignals, byte[] ctwaPayload, ContextInfo contextInfo, String nativeFlowCallButtonPayload, String deeplinkPayload, ChatMessageContextInfo messageContextInfo) {
        this.callKey = callKey;
        this.conversionSource = conversionSource;
        this.conversionData = conversionData;
        this.conversionDelaySeconds = conversionDelaySeconds;
        this.ctwaSignals = ctwaSignals;
        this.ctwaPayload = ctwaPayload;
        this.contextInfo = contextInfo;
        this.nativeFlowCallButtonPayload = nativeFlowCallButtonPayload;
        this.deeplinkPayload = deeplinkPayload;
        this.messageContextInfo = messageContextInfo;
    }

    /**
     * Returns the raw call key used during the call's Signal based key agreement.
     *
     * @return an {@link Optional} containing the call key bytes, or empty if not set
     */
    public Optional<byte[]> callKey() {
        return Optional.ofNullable(callKey);
    }

    /**
     * Returns the identifier of the surface that converted into this call.
     *
     * @return an {@link Optional} containing the conversion source, or empty if not set
     */
    public Optional<String> conversionSource() {
        return Optional.ofNullable(conversionSource);
    }

    /**
     * Returns the opaque payload associated with the conversion source.
     *
     * @return an {@link Optional} containing the conversion data bytes, or empty if not set
     */
    public Optional<byte[]> conversionData() {
        return Optional.ofNullable(conversionData);
    }

    /**
     * Returns the delay in seconds between the conversion event and the call start.
     *
     * @return an {@link OptionalInt} with the delay in seconds, or empty if not set
     */
    public OptionalInt conversionDelaySeconds() {
        return conversionDelaySeconds == null ? OptionalInt.empty() : OptionalInt.of(conversionDelaySeconds);
    }

    /**
     * Returns the Click To WhatsApp signals attached to the call.
     *
     * @return an {@link Optional} containing the CTWA signals, or empty if not set
     */
    public Optional<String> ctwaSignals() {
        return Optional.ofNullable(ctwaSignals);
    }

    /**
     * Returns the binary Click To WhatsApp payload.
     *
     * @return an {@link Optional} containing the CTWA payload bytes, or empty if not set
     */
    public Optional<byte[]> ctwaPayload() {
        return Optional.ofNullable(ctwaPayload);
    }

    /**
     * Returns the standard message context information attached to this call offer.
     *
     * @return an {@link Optional} containing the {@link ContextInfo}, or empty if not set
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the payload of the native flow button that triggered the call.
     *
     * @return an {@link Optional} containing the native flow payload, or empty if not set
     */
    public Optional<String> nativeFlowCallButtonPayload() {
        return Optional.ofNullable(nativeFlowCallButtonPayload);
    }

    /**
     * Returns the payload of the deeplink that triggered the call.
     *
     * @return an {@link Optional} containing the deeplink payload, or empty if not set
     */
    public Optional<String> deeplinkPayload() {
        return Optional.ofNullable(deeplinkPayload);
    }

    /**
     * Returns the chat specific context information attached to this call offer.
     *
     * @return an {@link Optional} containing the {@link ChatMessageContextInfo}, or empty if not set
     */
    public Optional<ChatMessageContextInfo> messageContextInfo() {
        return Optional.ofNullable(messageContextInfo);
    }

    /**
     * Sets the raw call key used during the call's key agreement.
     *
     * @param callKey the call key bytes, or {@code null} to clear the field
     */
    public void setCallKey(byte[] callKey) {
        this.callKey = callKey;
    }

    /**
     * Sets the identifier of the surface that converted into this call.
     *
     * @param conversionSource the conversion source identifier, or {@code null} to clear the field
     */
    public void setConversionSource(String conversionSource) {
        this.conversionSource = conversionSource;
    }

    /**
     * Sets the opaque payload associated with the conversion source.
     *
     * @param conversionData the conversion data bytes, or {@code null} to clear the field
     */
    public void setConversionData(byte[] conversionData) {
        this.conversionData = conversionData;
    }

    /**
     * Sets the delay in seconds between the conversion event and the call start.
     *
     * @param conversionDelaySeconds the delay in seconds, or {@code null} to clear the field
     */
    public void setConversionDelaySeconds(Integer conversionDelaySeconds) {
        this.conversionDelaySeconds = conversionDelaySeconds;
    }

    /**
     * Sets the Click To WhatsApp signals attached to the call.
     *
     * @param ctwaSignals the CTWA signals, or {@code null} to clear the field
     */
    public void setCtwaSignals(String ctwaSignals) {
        this.ctwaSignals = ctwaSignals;
    }

    /**
     * Sets the binary Click To WhatsApp payload.
     *
     * @param ctwaPayload the CTWA payload bytes, or {@code null} to clear the field
     */
    public void setCtwaPayload(byte[] ctwaPayload) {
        this.ctwaPayload = ctwaPayload;
    }

    /**
     * Sets the standard message context information.
     *
     * @param contextInfo the {@link ContextInfo}, or {@code null} to clear the field
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Sets the payload of the native flow button that triggered the call.
     *
     * @param nativeFlowCallButtonPayload the native flow payload, or {@code null} to clear the field
     */
    public void setNativeFlowCallButtonPayload(String nativeFlowCallButtonPayload) {
        this.nativeFlowCallButtonPayload = nativeFlowCallButtonPayload;
    }

    /**
     * Sets the payload of the deeplink that triggered the call.
     *
     * @param deeplinkPayload the deeplink payload, or {@code null} to clear the field
     */
    public void setDeeplinkPayload(String deeplinkPayload) {
        this.deeplinkPayload = deeplinkPayload;
    }

    /**
     * Sets the chat specific context information attached to this call offer.
     *
     * @param messageContextInfo the {@link ChatMessageContextInfo}, or {@code null} to clear the field
     */
    public void setMessageContextInfo(ChatMessageContextInfo messageContextInfo) {
        this.messageContextInfo = messageContextInfo;
    }
}
