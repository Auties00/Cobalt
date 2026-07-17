package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Outcome of a status-only change to a WhatsApp Business advertisement.
 *
 * <p>WhatsApp Business lets a merchant turn an ordinary message, status, or
 * product into a paid advertisement that, when tapped, opens a chat with the
 * business; these are the "Click-to-WhatsApp" ads. Creating and running such
 * an ad goes through a small lifecycle: the merchant assembles a draft, the
 * draft becomes a live boosted ad, and the live ad can later be paused,
 * resumed, or deleted. A few preparatory steps (confirming a contact email,
 * requesting an email verification code, notifying the payment hub, and
 * attesting the advertiser's identity) sit alongside that lifecycle.
 *
 * <p>Many of these actions report only whether the change took effect, without
 * echoing the entity that changed: deleting a draft, deleting a live ad,
 * pausing or resuming a live ad, notifying the payment hub, confirming email
 * onboarding, requesting an email verification code, and certifying the
 * advertiser all fall into this group. This model collapses those outcomes
 * into one shape so a caller checks {@link #success()} regardless of which
 * action it ran.
 *
 * <p>When the server reports which entities the action touched (for example
 * the identifier of the ad that was paused, or the certified user's name),
 * they are surfaced through {@link #affectedIds()}; otherwise that list is
 * empty. A human-readable failure reason, when the server provides one, is
 * carried by {@link #errorMessage()}.
 */
@ProtobufMessage(name = "BusinessAdMutationResult")
public final class BusinessAdMutationResult {
    /**
     * Whether the action took effect. The server reports this as the sole
     * outcome of a status-only advertisement change; {@code false} both when
     * the server explicitly reported failure and when it omitted the success
     * marker entirely.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean success;

    /**
     * Identifiers of the advertisement entities the action affected, when the
     * server reports them (for example the identifier of the ad that was
     * paused, or the name recorded against an advertiser certification). Never
     * {@code null}, possibly empty when the server reports no ids.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final List<String> affectedIds;

    /**
     * Human-readable reason the action failed. Empty when the action succeeded
     * or the server did not attach a reason.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String errorMessage;

    /**
     * Constructs a new {@code BusinessAdMutationResult}. A {@code null}
     * {@code affectedIds} is coerced to an empty list, and {@code errorMessage}
     * may be {@code null} when the server attached no failure reason.
     *
     * @param success      whether the action took effect
     * @param affectedIds  the affected entity ids; {@code null} treated as empty
     * @param errorMessage the failure reason, or {@code null}
     */
    BusinessAdMutationResult(boolean success, List<String> affectedIds, String errorMessage) {
        this.success = success;
        this.affectedIds = affectedIds == null ? List.of() : affectedIds;
        this.errorMessage = errorMessage;
    }

    /**
     * Returns whether the action took effect.
     *
     * @return {@code true} when the server reported the change applied
     */
    public boolean success() {
        return success;
    }

    /**
     * Returns the identifiers of the advertisement entities the action
     * affected.
     *
     * @return an unmodifiable view of the affected ids; never {@code null},
     *         possibly empty
     */
    public List<String> affectedIds() {
        return Collections.unmodifiableList(affectedIds);
    }

    /**
     * Returns the human-readable reason the action failed.
     *
     * @return an {@code Optional} carrying the failure reason, or empty when
     *         the action succeeded or carried no reason
     */
    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }
}
