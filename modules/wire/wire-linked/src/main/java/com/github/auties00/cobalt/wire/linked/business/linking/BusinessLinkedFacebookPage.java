package com.github.auties00.cobalt.wire.linked.business.linking;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Linked Facebook Page projection within a
 * {@link BusinessLinkedAccounts} bundle.
 *
 * <p>When a WhatsApp Business account links a Facebook Page, the relay
 * exposes the page's identifier and display name plus a handful of
 * server-driven sub-states that drive the Linked Accounts UI: whether
 * the page has ever run a CTWA ad, whether one is currently active,
 * whether the user has opted into picture/profile import, and whether
 * the WhatsApp Business app must render the "Message on WhatsApp"
 * button on the page.
 */
@ProtobufMessage(name = "BusinessLinkedFacebookPage")
public final class BusinessLinkedFacebookPage {
    /**
     * The Facebook Page identifier (numeric Graph API id).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * The page's display name (the user-facing brand name shown in the
     * Linked Accounts panel).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String displayName;

    /**
     * Whether the page has ever published a Click-to-WhatsApp ad
     * creative.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    boolean hasCreatedAd;

    /**
     * Whether the page currently has at least one Click-to-WhatsApp ad
     * actively running.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    boolean hasActiveCtwaAd;

    /**
     * Whether the user has explicitly disabled syncing the Facebook
     * Page profile picture into the WhatsApp Business profile. Absent
     * (empty {@link Optional}) when the relay omitted the toggle.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    Boolean profileSyncDisabled;

    /**
     * Whether the WhatsApp Business app must render the "Message on
     * WhatsApp" button on this page's surfaces.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    boolean whatsAppAsPageButtonEnabled;

    /**
     * Optional URL of the page's profile picture, served from the
     * Facebook CDN.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String profilePictureUrl;

    /**
     * Optional inline profile-picture bytes the relay attached when the
     * client has not yet downloaded the picture from the URL.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BYTES)
    byte[] profilePictureBytes;

    /**
     * Optional show-on-profile flag. {@code true} when the user has
     * opted into surfacing the link on their public WhatsApp profile,
     * {@code false} when they have opted out, absent (empty
     * {@link Optional}) when the relay omitted the toggle.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.BOOL)
    Boolean showOnProfile;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param id                          the Facebook Page identifier
     * @param displayName                 the page display name
     * @param hasCreatedAd                whether the page has ever
     *                                    published a CTWA ad
     * @param hasActiveCtwaAd             whether a CTWA ad is currently
     *                                    active
     * @param profileSyncDisabled         the optional profile-sync
     *                                    disabled toggle
     * @param whatsAppAsPageButtonEnabled whether the page button is
     *                                    enabled
     * @param profilePictureUrl           the optional profile-picture
     *                                    URL
     * @param profilePictureBytes         the optional inline picture
     *                                    bytes
     * @param showOnProfile               the optional show-on-profile
     *                                    toggle
     */
    BusinessLinkedFacebookPage(String id, String displayName,
                               boolean hasCreatedAd, boolean hasActiveCtwaAd,
                               Boolean profileSyncDisabled,
                               boolean whatsAppAsPageButtonEnabled,
                               String profilePictureUrl, byte[] profilePictureBytes,
                               Boolean showOnProfile) {
        this.id = id;
        this.displayName = displayName;
        this.hasCreatedAd = hasCreatedAd;
        this.hasActiveCtwaAd = hasActiveCtwaAd;
        this.profileSyncDisabled = profileSyncDisabled;
        this.whatsAppAsPageButtonEnabled = whatsAppAsPageButtonEnabled;
        this.profilePictureUrl = profilePictureUrl;
        this.profilePictureBytes = profilePictureBytes;
        this.showOnProfile = showOnProfile;
    }

    /**
     * Returns the Facebook Page identifier.
     *
     * @return the identifier; never {@code null} for a parsed
     *         projection
     */
    public String id() {
        return id;
    }

    /**
     * Returns the page display name.
     *
     * @return the name; never {@code null} for a parsed projection
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns whether the page has ever published a Click-to-WhatsApp
     * ad.
     *
     * @return {@code true} when the page has ever published a CTWA ad
     */
    public boolean hasCreatedAd() {
        return hasCreatedAd;
    }

    /**
     * Returns whether the page currently has an active
     * Click-to-WhatsApp ad.
     *
     * @return {@code true} when at least one CTWA ad is currently
     *         running
     */
    public boolean hasActiveCtwaAd() {
        return hasActiveCtwaAd;
    }

    /**
     * Returns the optional profile-sync disabled toggle.
     *
     * @return an {@link Optional} carrying the toggle, or empty when
     *         the relay omitted it
     */
    public Optional<Boolean> profileSyncDisabled() {
        return Optional.ofNullable(profileSyncDisabled);
    }

    /**
     * Returns whether the "Message on WhatsApp" page button is enabled.
     *
     * @return {@code true} when the button must be rendered
     */
    public boolean whatsAppAsPageButtonEnabled() {
        return whatsAppAsPageButtonEnabled;
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
     * Returns the optional show-on-profile toggle.
     *
     * @return an {@link Optional} carrying the toggle, or empty when
     *         the relay omitted it
     */
    public Optional<Boolean> showOnProfile() {
        return Optional.ofNullable(showOnProfile);
    }
}
