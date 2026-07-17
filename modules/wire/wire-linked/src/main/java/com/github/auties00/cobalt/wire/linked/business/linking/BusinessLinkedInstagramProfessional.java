package com.github.auties00.cobalt.wire.linked.business.linking;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Linked Instagram Professional projection within a
 * {@link BusinessLinkedAccounts} bundle.
 *
 * <p>An Instagram Professional account is a creator or business
 * Instagram surface tied to the same Meta Business Manager. The relay
 * exposes the public handle plus, when populated, the display name and
 * profile picture so the WhatsApp Business client can render the
 * surface in the Linked Accounts panel without an extra Graph API
 * round-trip.
 */
@ProtobufMessage(name = "BusinessLinkedInstagramProfessional")
public final class BusinessLinkedInstagramProfessional {
    /**
     * The Instagram public handle (without the {@code @} prefix).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String igHandle;

    /**
     * Optional URL of the Instagram profile picture, served from the
     * Facebook CDN.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String profilePictureUrl;

    /**
     * Optional inline profile-picture bytes the relay attached when
     * the client has not yet downloaded the picture from the URL.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] profilePictureBytes;

    /**
     * Optional display name (the human-readable brand name shown in
     * the Instagram bio).
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String displayName;

    /**
     * Optional show-on-profile flag. {@code true} when the user has
     * opted into surfacing the link on their public WhatsApp profile,
     * {@code false} when they have opted out, absent (empty
     * {@link Optional}) when the relay omitted the toggle.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    Boolean showOnProfile;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param igHandle            the Instagram handle
     * @param profilePictureUrl   the optional profile-picture URL
     * @param profilePictureBytes the optional inline picture bytes
     * @param displayName         the optional display name
     * @param showOnProfile       the optional show-on-profile toggle
     */
    BusinessLinkedInstagramProfessional(String igHandle,
                                        String profilePictureUrl,
                                        byte[] profilePictureBytes,
                                        String displayName,
                                        Boolean showOnProfile) {
        this.igHandle = igHandle;
        this.profilePictureUrl = profilePictureUrl;
        this.profilePictureBytes = profilePictureBytes;
        this.displayName = displayName;
        this.showOnProfile = showOnProfile;
    }

    /**
     * Returns the Instagram public handle.
     *
     * @return the handle; never {@code null} for a parsed projection
     */
    public String igHandle() {
        return igHandle;
    }

    /**
     * Returns the optional profile-picture URL.
     *
     * @return an {@link Optional} carrying the URL, or empty when the
     *         relay omitted it
     */
    public Optional<String> profilePictureUrl() {
        return Optional.ofNullable(profilePictureUrl);
    }

    /**
     * Returns the optional inline profile-picture bytes.
     *
     * @return an {@link Optional} carrying the bytes, or empty when
     *         the relay omitted them
     */
    public Optional<byte[]> profilePictureBytes() {
        return Optional.ofNullable(profilePictureBytes);
    }

    /**
     * Returns the optional display name.
     *
     * @return an {@link Optional} carrying the name, or empty when
     *         the relay omitted it
     */
    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    /**
     * Returns the optional show-on-profile toggle.
     *
     * @return an {@link Optional} carrying the toggle, or empty when
     *         the relay omitted it
     */
    public Optional<Boolean> showOnProfile() {
        return Optional.ofNullable(showOnProfile);
    }
}
