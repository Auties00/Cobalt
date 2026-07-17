package com.github.auties00.cobalt.wire.linked.message.bot;

import com.github.auties00.cobalt.wire.linked.bot.response.AIRichResponseMessageType;
import com.github.auties00.cobalt.wire.linked.bot.response.AIRichResponseSubMessage;
import com.github.auties00.cobalt.wire.linked.bot.response.AIRichResponseUnifiedResponse;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A structured response produced by the WhatsApp AI assistant.
 *
 * <p>A rich response is composed of an ordered sequence of typed
 * {@link AIRichResponseSubMessage} fragments. Each fragment may carry
 * plain text, formatted code, a tabular dataset, inline or grid
 * images, a map view, a LaTeX expression, a dynamic media payload,
 * or a reel carousel. Clients render the fragments in order to
 * produce the final visual response shown to the user.
 *
 * <p>In addition to the typed fragments, the server may attach a
 * {@linkplain #unifiedResponse() unified response} that carries the
 * entire answer as a single UTF-8 JSON document. The unified payload
 * is provided as an alternative representation for clients that
 * prefer to render the response from a consolidated source rather
 * than from the fragment list.
 *
 * <p>Because {@code AIRichResponseMessage} implements
 * {@link ContextualMessage}, it can carry quoted-message metadata
 * and forwarding information through its
 * {@linkplain #contextInfo() context info}.
 */
@ProtobufMessage(name = "AIRichResponseMessage")
public final class AIRichResponseMessage implements ContextualMessage {
    /**
     * The overall classification of this rich response.
     *
     * <p>Identifies the high-level response category (for example a
     * search answer, a conversational reply, or a generated media
     * response) which clients may use to pick an appropriate
     * presentation style.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    AIRichResponseMessageType messageType;

    /**
     * The ordered content fragments that compose this rich response.
     *
     * <p>Each fragment represents a single visual element of the
     * response. Clients render the fragments sequentially to produce
     * the full answer.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<AIRichResponseSubMessage> submessages;

    /**
     * An alternative, consolidated representation of the response.
     *
     * <p>When present, carries a single UTF-8 JSON document that
     * encodes the full answer in a unified format. Clients may
     * render from this payload instead of iterating the fragment
     * list.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    AIRichResponseUnifiedResponse unifiedResponse;

    /**
     * Optional contextual metadata attached to this message.
     *
     * <p>May carry a quoted message reference, forwarding counters,
     * mention lists, or other metadata shared by all
     * {@link ContextualMessage} types.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;


    /**
     * Constructs a new rich AI response message with the given
     * components.
     *
     * <p>This constructor is package-private and is intended to be
     * invoked by the generated protobuf builder. Application code
     * should use {@code AIRichResponseMessageBuilder} to construct
     * instances.
     *
     * @param messageType     the overall response classification, or {@code null} if unspecified
     * @param submessages     the ordered content fragments, or {@code null} if none are provided
     * @param unifiedResponse the consolidated JSON representation, or {@code null} if absent
     * @param contextInfo     the contextual metadata, or {@code null} if none is attached
     */
    AIRichResponseMessage(AIRichResponseMessageType messageType, List<AIRichResponseSubMessage> submessages, AIRichResponseUnifiedResponse unifiedResponse, ContextInfo contextInfo) {
        this.messageType = messageType;
        this.submessages = submessages;
        this.unifiedResponse = unifiedResponse;
        this.contextInfo = contextInfo;
    }

    /**
     * Returns the overall classification of this rich response.
     *
     * @return an {@link Optional} containing the response type, or
     *         {@link Optional#empty()} if no type was set by the server
     */
    public Optional<AIRichResponseMessageType> messageType() {
        return Optional.ofNullable(messageType);
    }

    /**
     * Returns the ordered content fragments that compose this rich
     * response.
     *
     * <p>The returned list is unmodifiable and never {@code null}.
     * When no fragments are present an empty list is returned, which
     * typically indicates that the response is encoded entirely in
     * the {@linkplain #unifiedResponse() unified response} payload.
     *
     * @return an unmodifiable list of response fragments, possibly
     *         empty but never {@code null}
     */
    public List<AIRichResponseSubMessage> submessages() {
        return submessages == null ? List.of() : Collections.unmodifiableList(submessages);
    }

    /**
     * Returns the consolidated JSON representation of this response,
     * if the server provided one.
     *
     * @return an {@link Optional} containing the unified response,
     *         or {@link Optional#empty()} if the server did not
     *         provide a consolidated payload
     */
    public Optional<AIRichResponseUnifiedResponse> unifiedResponse() {
        return Optional.ofNullable(unifiedResponse);
    }

    /**
     * Returns the contextual metadata attached to this message.
     *
     * @return an {@link Optional} containing the context information,
     *         or {@link Optional#empty()} if no context metadata is
     *         attached
     */
    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Updates the overall classification of this rich response.
     *
     * @param messageType the new response classification, or
     *                    {@code null} to clear the current value
     */
    public void setMessageType(AIRichResponseMessageType messageType) {
        this.messageType = messageType;
    }

    /**
     * Replaces the ordered content fragments that compose this rich
     * response.
     *
     * <p>The provided list is stored directly; callers should ensure
     * that the list ordering matches the intended rendering order,
     * since fragments are displayed sequentially.
     *
     * @param submessages the new list of fragments, or {@code null}
     *                    to clear the current fragments
     */
    public void setSubmessages(List<AIRichResponseSubMessage> submessages) {
        this.submessages = submessages;
    }

    /**
     * Updates the consolidated JSON representation of this response.
     *
     * @param unifiedResponse the new unified response payload, or
     *                        {@code null} to clear the current value
     */
    public void setUnifiedResponse(AIRichResponseUnifiedResponse unifiedResponse) {
        this.unifiedResponse = unifiedResponse;
    }

    /**
     * Updates the contextual metadata attached to this message.
     *
     * @param contextInfo the new context information, or
     *                    {@code null} to clear the current value
     */
    @Override
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }
}
