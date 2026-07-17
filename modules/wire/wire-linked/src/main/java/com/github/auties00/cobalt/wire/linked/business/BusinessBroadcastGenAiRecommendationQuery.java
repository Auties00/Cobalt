package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for the generative-AI message recommendation of the WhatsApp
 * Business broadcast composer.
 *
 * <p>When a merchant composes a broadcast to many recipients, the composer
 * can suggest tone-tagged message variants by asking a generative-AI
 * backend to refine the draft against the recipient's recent message
 * history. This input bundles every parameter the suggestion engine
 * consumes.
 *
 * <p>{@link #actorId()} names the WhatsApp Business identity acting on
 * behalf of the merchant. {@link #modelId()} selects the generative-AI
 * backend model the server should run the request against. The
 * {@link #userInfo()} blob is an opaque user-context payload (typically a
 * recent message-history JSON object) that the server forwards verbatim to
 * the backend. {@link #userMessageDraft()} carries the draft the operator
 * wrote and {@link #userPrompt()} carries the additional steering prompt
 * the operator supplied to bias the suggestion.
 */
@ProtobufMessage(name = "BusinessBroadcastGenAiRecommendationQuery")
public final class BusinessBroadcastGenAiRecommendationQuery {
    /**
     * WhatsApp Business identity acting on behalf of the merchant. Unset
     * omits the variable.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String actorId;

    /**
     * Generative-AI backend model identifier the server should run the
     * request against. Unset omits the variable.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String modelId;

    /**
     * Opaque user-context blob the server forwards verbatim to the
     * generative-AI backend, typically a JSON object carrying the
     * recipient's recent message history. Unset omits the variable.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String userInfo;

    /**
     * Draft message the operator wrote and wants the backend to refine.
     * Unset omits the variable.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String userMessageDraft;

    /**
     * Additional steering prompt the operator supplied to bias the
     * suggestion. Unset omits the variable.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String userPrompt;

    /**
     * Constructs a new {@code BusinessBroadcastGenAiRecommendationQuery}.
     * Every argument may be {@code null} to omit the corresponding variable
     * from the request.
     *
     * @param actorId          the WhatsApp Business identity acting on
     *                         behalf of the merchant, or {@code null}
     * @param modelId          the generative-AI backend model identifier,
     *                         or {@code null}
     * @param userInfo         the opaque user-context blob, or {@code null}
     * @param userMessageDraft the draft message the operator wrote, or
     *                         {@code null}
     * @param userPrompt       the additional steering prompt, or
     *                         {@code null}
     */
    public BusinessBroadcastGenAiRecommendationQuery(String actorId, String modelId, String userInfo,
                                                     String userMessageDraft, String userPrompt) {
        this.actorId = actorId;
        this.modelId = modelId;
        this.userInfo = userInfo;
        this.userMessageDraft = userMessageDraft;
        this.userPrompt = userPrompt;
    }

    /**
     * Returns the WhatsApp Business identity acting on behalf of the
     * merchant.
     *
     * @return an {@link Optional} carrying the identity, or empty when
     *         unset
     */
    public Optional<String> actorId() {
        return Optional.ofNullable(actorId);
    }

    /**
     * Returns the generative-AI backend model identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> modelId() {
        return Optional.ofNullable(modelId);
    }

    /**
     * Returns the opaque user-context blob.
     *
     * @return an {@link Optional} carrying the blob, or empty when unset
     */
    public Optional<String> userInfo() {
        return Optional.ofNullable(userInfo);
    }

    /**
     * Returns the draft message the operator wrote.
     *
     * @return an {@link Optional} carrying the draft, or empty when unset
     */
    public Optional<String> userMessageDraft() {
        return Optional.ofNullable(userMessageDraft);
    }

    /**
     * Returns the additional steering prompt the operator supplied.
     *
     * @return an {@link Optional} carrying the prompt, or empty when unset
     */
    public Optional<String> userPrompt() {
        return Optional.ofNullable(userPrompt);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessBroadcastGenAiRecommendationQuery) obj;
        return Objects.equals(actorId, that.actorId)
                && Objects.equals(modelId, that.modelId)
                && Objects.equals(userInfo, that.userInfo)
                && Objects.equals(userMessageDraft, that.userMessageDraft)
                && Objects.equals(userPrompt, that.userPrompt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actorId, modelId, userInfo, userMessageDraft, userPrompt);
    }

    @Override
    public String toString() {
        return "BusinessBroadcastGenAiRecommendationQuery[" +
                "actorId=" + actorId + ", " +
                "modelId=" + modelId + ", " +
                "userInfo=" + userInfo + ", " +
                "userMessageDraft=" + userMessageDraft + ", " +
                "userPrompt=" + userPrompt + ']';
    }
}
