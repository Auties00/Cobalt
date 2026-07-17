package com.github.auties00.cobalt.wire.linked.preference;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model representing the dismissed/seen state of a single WhatsApp
 * onboarding hint.
 *
 * <p>WhatsApp shows one-shot tooltips and banners the first time a user
 * encounters a new feature (the underlying WA Web codename is "NUX",
 * short for "New User Experience"). Each hint is identified by a stable
 * {@linkplain #hintId() identifier}; the {@linkplain #dismissed()
 * dismissed flag} records whether the user has already acknowledged the
 * hint and therefore whether it should still be shown.
 *
 * <p>Cobalt persists each hint independently so callers can resolve the
 * status of a single hint without iterating the whole catalog. The matching
 * sync action updates the record whenever the hint is shown or dismissed.
 *
 * <p>This class is a local model only. Modifying its fields does not send any
 * request to the WhatsApp servers; it simply reflects the locally cached
 * state.
 */
@ProtobufMessage
public final class OnboardingHintState {
    /**
     * The non-{@code null} stable identifier of the onboarding hint. Used as
     * the primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String hintId;

    /**
     * Whether the user has already dismissed (or otherwise acknowledged)
     * this hint and it should not be shown again.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    boolean dismissed;

    /**
     * Constructs a new onboarding-hint state with the given identifier and
     * dismissed flag.
     *
     * @param hintId    the non-{@code null} hint identifier
     * @param dismissed whether the hint has been dismissed
     */
    OnboardingHintState(String hintId, boolean dismissed) {
        this.hintId = Objects.requireNonNull(hintId, "hintId cannot be null");
        this.dismissed = dismissed;
    }

    /**
     * Returns the non-{@code null} hint identifier.
     *
     * @return the hint identifier
     */
    public String hintId() {
        return hintId;
    }

    /**
     * Returns whether the hint has already been dismissed by the user.
     *
     * @return {@code true} if the hint has been dismissed
     */
    public boolean dismissed() {
        return dismissed;
    }

    /**
     * Updates the dismissed flag of this hint.
     *
     * @param dismissed the new dismissed flag
     * @return this hint state instance for method chaining
     */
    public OnboardingHintState setDismissed(boolean dismissed) {
        this.dismissed = dismissed;
        return this;
    }

    /**
     * Returns a hash code derived from this hint's
     * {@linkplain #hintId() identifier}.
     *
     * @return the hash code of the hint identifier
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(hintId);
    }

    /**
     * Returns whether this hint state is equal to the given object.
     *
     * <p>Two hint states are considered equal when they share the same
     * {@linkplain #hintId() identifier}, regardless of the dismissed flag.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is an {@code OnboardingHintState}
     *         with the same identifier
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof OnboardingHintState that && Objects.equals(this.hintId, that.hintId);
    }
}
