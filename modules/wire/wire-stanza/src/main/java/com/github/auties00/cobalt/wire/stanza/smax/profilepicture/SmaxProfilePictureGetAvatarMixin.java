package com.github.auties00.cobalt.wire.stanza.smax.profilepicture;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.List;
import java.util.Objects;

/**
 * Carries the {@code 0..4} {@code <avatar pose_id/>} children that turn a
 * {@link SmaxProfilePictureGetRequest} into an avatar-pose-set fetch.
 *
 * <p>An instance is passed to {@link SmaxProfilePictureGetRequest} when fetching
 * an entity's multi-pose avatar rather than a still picture; the relay then
 * returns one {@link SmaxProfilePictureGetResponse.SuccessAvatarURLs.AvatarUrl}
 * per requested pose-id.
 *
 * @implNote
 * This implementation enforces the {@code 0..4} bound at construction time so
 * {@link SmaxProfilePictureGetRequest#toStanza()} can iterate without
 * re-checking.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutProfilePictureAvatarMixin")
public final class SmaxProfilePictureGetAvatarMixin {
    /**
     * The list of {@code <avatar pose_id/>} children; bounded to {@code 0..4}
     * entries.
     */
    private final List<AvatarPose> avatarArgs;

    /**
     * Constructs an avatar payload from the given pose entries.
     *
     * @implNote
     * This implementation defensively copies the input list via
     * {@link List#copyOf(java.util.Collection)} so later caller mutations do
     * not affect the payload.
     *
     * @param avatarArgs the avatar entries; must contain at most {@code 4}
     *                   entries
     * @throws NullPointerException     if {@code avatarArgs} is {@code null}
     * @throws IllegalArgumentException if {@code avatarArgs} exceeds {@code 4}
     *                                  entries
     */
    public SmaxProfilePictureGetAvatarMixin(List<AvatarPose> avatarArgs) {
        Objects.requireNonNull(avatarArgs, "avatarArgs cannot be null");
        if (avatarArgs.size() > 4) {
            throw new IllegalArgumentException(
                    "avatarArgs must carry at most 4 entries");
        }
        this.avatarArgs = List.copyOf(avatarArgs);
    }

    /**
     * Returns the avatar entries.
     *
     * <p>{@link SmaxProfilePictureGetRequest#toStanza()} reads this list when
     * fanning the entries into {@code <avatar pose_id=...>} children.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<AvatarPose> avatarArgs() {
        return avatarArgs;
    }

    /**
     * Compares this payload to another for value equality on the avatar list.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a
     *         {@link SmaxProfilePictureGetAvatarMixin} with an equal entry list
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxProfilePictureGetAvatarMixin) obj;
        return Objects.equals(this.avatarArgs, that.avatarArgs);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(avatarArgs);
    }

    /**
     * Returns a debug-friendly representation of this payload.
     *
     * <p>The format is intended for logging and is not part of the contract.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "SmaxProfilePictureGetAvatarMixin[avatarArgs=" + avatarArgs + ']';
    }

    /**
     * A single {@code <avatar pose_id/>} entry within an avatar payload.
     */
    public static final class AvatarPose {
        /**
         * The {@code pose_id} attribute.
         */
        private final String avatarPoseId;

        /**
         * Constructs a pose entry from the given pose id.
         *
         * @param avatarPoseId the pose id; never {@code null}
         * @throws NullPointerException if {@code avatarPoseId} is {@code null}
         */
        public AvatarPose(String avatarPoseId) {
            this.avatarPoseId = Objects.requireNonNull(avatarPoseId, "avatarPoseId cannot be null");
        }

        /**
         * Returns the pose id.
         *
         * @return the id; never {@code null}
         */
        public String avatarPoseId() {
            return avatarPoseId;
        }

        /**
         * Builds the {@code <avatar pose_id/>} stanza consumed by
         * {@link SmaxProfilePictureGetRequest#toStanza()}.
         *
         * @return the {@link Stanza}
         */
        @WhatsAppWebExport(moduleName = "WASmaxOutProfilePictureAvatarMixin",
                exports = "makeAvatarAvatar",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Stanza toStanza() {
            return new StanzaBuilder()
                    .description("avatar")
                    .attribute("pose_id", avatarPoseId)
                    .build();
        }

        /**
         * Compares this entry to another for value equality on the pose id.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an {@link AvatarPose} with an
         *         equal pose id
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (AvatarPose) obj;
            return Objects.equals(this.avatarPoseId, that.avatarPoseId);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(avatarPoseId);
        }

        /**
         * Returns a debug-friendly representation of this entry.
         *
         * <p>The format is intended for logging and is not part of the
         * contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxProfilePictureGetAvatarMixin.AvatarPose[avatarPoseId=" + avatarPoseId + ']';
        }
    }
}
