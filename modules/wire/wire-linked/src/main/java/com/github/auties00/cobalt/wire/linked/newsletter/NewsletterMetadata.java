package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.*;

/**
 * Aggregates the administrative metadata of a newsletter.
 *
 * <p>This class brings together every directory-level field that describes
 * a newsletter to subscribers and admins: its display name and
 * description, its full and preview profile pictures, its handle and
 * invite code, its privacy and verification state, its creation time and
 * subscriber count, linkage information, admin count, geo-suspension
 * status, opted-in feature capabilities, and any associated WAMO
 * subscription plan.
 *
 * <p>Every field is optional: the server populates only the fields that
 * are applicable to the newsletter and visible to the current viewer.
 */
@ProtobufMessage
public final class NewsletterMetadata {
    /**
     * The display name of the newsletter.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    NewsletterName name;

    /**
     * The textual description shown on the newsletter profile screen.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    NewsletterDescription description;

    /**
     * The full-resolution profile picture.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    NewsletterPicture picture;

    /**
     * The unique handle used to deep-link or share the newsletter.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String handle;

    /**
     * The server-configurable settings (reaction policy and related).
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    NewsletterSettings settings;

    /**
     * The opaque invite code that can be used to join this newsletter.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String invite;

    /**
     * The verification state of this newsletter.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    NewsletterVerification verification;

    /**
     * The moment at which this newsletter was created.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant creationTimestamp;

    /**
     * The number of subscribers. {@code null} when not reported yet.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.UINT64)
    Long subscribersCount;

    /**
     * The discoverability policy of this newsletter.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
    NewsletterPrivacy privacy;

    /**
     * Whether this newsletter is linked to other accounts, for example
     * business or WAMO accounts.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    boolean hasLinkedAccounts;

    /**
     * The number of admins. {@code null} when not reported yet.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.UINT64)
    Long adminCount;

    /**
     * Whether this newsletter has been administratively terminated.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.BOOL)
    boolean terminated;

    /**
     * The ISO country codes in which this newsletter is currently
     * geo-suspended.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    List<String> geosuspendedCountries;

    /**
     * The optional features that this newsletter has opted into.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.ENUM)
    List<NewsletterCapability> capabilities;

    /**
     * The WAMO subscription plan identifier tied to this newsletter.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.STRING)
    String wamoSubPlanId;

    /**
     * The preview-resolution profile picture used for thumbnails.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    NewsletterPicture previewPicture;

    /**
     * Constructs a new {@code NewsletterMetadata}. Invoked by the
     * generated protobuf deserializer.
     *
     * @param name                  the display name, may be {@code null}
     * @param description           the description, may be {@code null}
     * @param picture               the full-resolution picture, may be {@code null}
     * @param handle                the unique handle, may be {@code null}
     * @param settings              the server-configurable settings, may be {@code null}
     * @param invite                the invite code, may be {@code null}
     * @param verification          the verification state, may be {@code null}
     * @param creationTimestamp     the creation timestamp, may be {@code null}
     * @param subscribersCount      the subscriber count, may be {@code null}
     * @param privacy               the discoverability policy, may be {@code null}
     * @param hasLinkedAccounts     whether the newsletter has linked accounts
     * @param adminCount            the admin count, may be {@code null}
     * @param terminated            whether the newsletter has been terminated
     * @param geosuspendedCountries the geo-suspended countries, defaulted to an empty mutable list when {@code null}
     * @param capabilities          the opted-in capabilities, defaulted to an empty mutable list when {@code null}
     * @param wamoSubPlanId         the WAMO subscription plan identifier, may be {@code null}
     * @param previewPicture        the preview-resolution picture, may be {@code null}
     */
    NewsletterMetadata(NewsletterName name, NewsletterDescription description, NewsletterPicture picture, String handle, NewsletterSettings settings, String invite, NewsletterVerification verification, Instant creationTimestamp, Long subscribersCount, NewsletterPrivacy privacy, boolean hasLinkedAccounts, Long adminCount, boolean terminated, List<String> geosuspendedCountries, List<NewsletterCapability> capabilities, String wamoSubPlanId, NewsletterPicture previewPicture) {
        this.name = name;
        this.description = description;
        this.picture = picture;
        this.handle = handle;
        this.settings = settings;
        this.invite = invite;
        this.verification = verification;
        this.creationTimestamp = creationTimestamp;
        this.subscribersCount = subscribersCount;
        this.privacy = privacy;
        this.hasLinkedAccounts = hasLinkedAccounts;
        this.adminCount = adminCount;
        this.terminated = terminated;
        this.geosuspendedCountries = Objects.requireNonNullElseGet(geosuspendedCountries, ArrayList::new);
        this.capabilities = Objects.requireNonNullElseGet(capabilities, ArrayList::new);
        this.wamoSubPlanId = wamoSubPlanId;
        this.previewPicture = previewPicture;
    }

    /**
     * Returns the moment at which this newsletter was created.
     *
     * @return an {@link Optional} holding the creation timestamp, or
     *         empty when not reported
     */
    public Optional<Instant> creationTimestamp() {
        return Optional.ofNullable(creationTimestamp);
    }

    /**
     * Returns the display name of this newsletter.
     *
     * @return an {@link Optional} holding the display name, or empty
     *         when not reported
     */
    public Optional<NewsletterName> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the textual description of this newsletter.
     *
     * @return an {@link Optional} holding the description, or empty when
     *         not reported
     */
    public Optional<NewsletterDescription> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the full-resolution profile picture.
     *
     * @return an {@link Optional} holding the picture, or empty when not
     *         reported
     */
    public Optional<NewsletterPicture> picture() {
        return Optional.ofNullable(picture);
    }

    /**
     * Returns the preview-resolution profile picture.
     *
     * @return an {@link Optional} holding the preview picture, or empty
     *         when not reported
     */
    public Optional<NewsletterPicture> previewPicture() {
        return Optional.ofNullable(previewPicture);
    }

    /**
     * Returns the unique handle used to deep-link to this newsletter.
     *
     * @return an {@link Optional} holding the handle, or empty when not
     *         reported
     */
    public Optional<String> handle() {
        return Optional.ofNullable(handle);
    }

    /**
     * Returns the server-configurable settings such as the reaction
     * policy.
     *
     * @return an {@link Optional} holding the settings, or empty when
     *         not reported
     */
    public Optional<NewsletterSettings> settings() {
        return Optional.ofNullable(settings);
    }

    /**
     * Returns the opaque invite code that can be used to join this
     * newsletter.
     *
     * @return an {@link Optional} holding the invite code, or empty when
     *         the viewer is not entitled to see it
     */
    public Optional<String> invite() {
        return Optional.ofNullable(invite);
    }

    /**
     * Returns the verification state of this newsletter.
     *
     * @return an {@link Optional} holding the verification state, or
     *         empty when not reported
     */
    public Optional<NewsletterVerification> verification() {
        return Optional.ofNullable(verification);
    }

    /**
     * Returns the number of subscribers to this newsletter.
     *
     * @return an {@link OptionalLong} holding the subscriber count, or
     *         empty when not reported
     */
    public OptionalLong subscribersCount() {
        return subscribersCount == null ? OptionalLong.empty() : OptionalLong.of(subscribersCount);
    }

    /**
     * Returns the discoverability policy of this newsletter.
     *
     * @return an {@link Optional} holding the privacy setting, or empty
     *         when not reported
     */
    public Optional<NewsletterPrivacy> privacy() {
        return Optional.ofNullable(privacy);
    }

    /**
     * Returns whether this newsletter is linked to other accounts.
     *
     * @return {@code true} when any external account is linked
     */
    public boolean hasLinkedAccounts() {
        return hasLinkedAccounts;
    }

    /**
     * Returns the number of admins for this newsletter.
     *
     * @return an {@link OptionalLong} holding the admin count, or empty
     *         when not reported
     */
    public OptionalLong adminCount() {
        return adminCount == null ? OptionalLong.empty() : OptionalLong.of(adminCount);
    }

    /**
     * Returns whether this newsletter has been administratively
     * terminated.
     *
     * @return {@code true} when the newsletter is terminated
     */
    public boolean terminated() {
        return terminated;
    }

    /**
     * Returns the ISO country codes in which this newsletter is currently
     * geo-suspended.
     *
     * @return an unmodifiable list of country codes, never {@code null}
     */
    public List<String> geosuspendedCountries() {
        return Collections.unmodifiableList(geosuspendedCountries);
    }

    /**
     * Returns the optional features this newsletter has opted into.
     *
     * @return an unmodifiable list of capabilities, never {@code null}
     */
    public List<NewsletterCapability> capabilities() {
        return Collections.unmodifiableList(capabilities);
    }

    /**
     * Returns the WAMO subscription plan identifier tied to this
     * newsletter.
     *
     * @return an {@link Optional} holding the plan identifier, or empty
     *         when no WAMO plan is associated
     */
    public Optional<String> wamoSubPlanId() {
        return Optional.ofNullable(wamoSubPlanId);
    }

    /**
     * Sets the display name.
     *
     * @param name the new display name, or {@code null}
     */
    public void setName(NewsletterName name) {
        this.name = name;
    }

    /**
     * Sets the textual description.
     *
     * @param description the new description, or {@code null}
     */
    public void setDescription(NewsletterDescription description) {
        this.description = description;
    }

    /**
     * Sets the full-resolution profile picture.
     *
     * @param picture the new picture, or {@code null}
     */
    public void setPicture(NewsletterPicture picture) {
        this.picture = picture;
    }

    /**
     * Sets the preview-resolution profile picture.
     *
     * @param previewPicture the new preview picture, or {@code null}
     */
    public void setPreviewPicture(NewsletterPicture previewPicture) {
        this.previewPicture = previewPicture;
    }

    /**
     * Sets the unique handle.
     *
     * @param handle the new handle, or {@code null}
     */
    public void setHandle(String handle) {
        this.handle = handle;
    }

    /**
     * Sets the server-configurable settings.
     *
     * @param settings the new settings, or {@code null}
     */
    public void setSettings(NewsletterSettings settings) {
        this.settings = settings;
    }

    /**
     * Sets the invite code.
     *
     * @param invite the new invite code, or {@code null}
     */
    public void setInvite(String invite) {
        this.invite = invite;
    }

    /**
     * Sets the verification state.
     *
     * @param verification the new verification state, or {@code null}
     */
    public void setVerification(NewsletterVerification verification) {
        this.verification = verification;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param creationTimestamp the new creation timestamp, or
     *                          {@code null}
     */
    public void setCreationTimestamp(Instant creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    /**
     * Sets the subscriber count.
     *
     * @param subscribersCount the new subscriber count, or {@code null}
     */
    public void setSubscribersCount(Long subscribersCount) {
        this.subscribersCount = subscribersCount;
    }

    /**
     * Sets the discoverability policy.
     *
     * @param privacy the new privacy setting, or {@code null}
     */
    public void setPrivacy(NewsletterPrivacy privacy) {
        this.privacy = privacy;
    }

    /**
     * Sets whether this newsletter is linked to other accounts.
     *
     * @param hasLinkedAccounts the new linked-accounts flag
     */
    public void setHasLinkedAccounts(boolean hasLinkedAccounts) {
        this.hasLinkedAccounts = hasLinkedAccounts;
    }

    /**
     * Sets the admin count.
     *
     * @param adminCount the new admin count, or {@code null}
     */
    public void setAdminCount(Long adminCount) {
        this.adminCount = adminCount;
    }

    /**
     * Sets whether this newsletter has been administratively terminated.
     *
     * @param terminated the new terminated flag
     */
    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    /**
     * Sets the list of ISO country codes in which this newsletter is
     * geo-suspended.
     *
     * @param geosuspendedCountries the new country codes, defaulted to an
     *                              empty mutable list when {@code null}
     */
    public void setGeosuspendedCountries(List<String> geosuspendedCountries) {
        this.geosuspendedCountries = Objects.requireNonNullElseGet(geosuspendedCountries, ArrayList::new);
    }

    /**
     * Sets the list of optional features this newsletter has opted into.
     *
     * @param capabilities the new capabilities, defaulted to an empty
     *                     mutable list when {@code null}
     */
    public void setCapabilities(List<NewsletterCapability> capabilities) {
        this.capabilities = Objects.requireNonNullElseGet(capabilities, ArrayList::new);
    }

    /**
     * Sets the WAMO subscription plan identifier.
     *
     * @param wamoSubPlanId the new plan identifier, or {@code null}
     */
    public void setWamoSubPlanId(String wamoSubPlanId) {
        this.wamoSubPlanId = wamoSubPlanId;
    }

    /**
     * Returns whether this metadata equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterMetadata}
     *         whose fields are all equal to this one's
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterMetadata that
               && hasLinkedAccounts == that.hasLinkedAccounts
               && terminated == that.terminated
               && Objects.equals(name, that.name)
               && Objects.equals(description, that.description)
               && Objects.equals(picture, that.picture)
               && Objects.equals(previewPicture, that.previewPicture)
               && Objects.equals(handle, that.handle)
               && Objects.equals(settings, that.settings)
               && Objects.equals(invite, that.invite)
               && Objects.equals(verification, that.verification)
               && Objects.equals(creationTimestamp, that.creationTimestamp)
               && Objects.equals(subscribersCount, that.subscribersCount)
               && privacy == that.privacy
               && Objects.equals(adminCount, that.adminCount)
               && Objects.equals(geosuspendedCountries, that.geosuspendedCountries)
               && Objects.equals(capabilities, that.capabilities)
               && Objects.equals(wamoSubPlanId, that.wamoSubPlanId);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this metadata
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, description, picture, previewPicture, handle, settings, invite, verification, creationTimestamp, subscribersCount, privacy, hasLinkedAccounts, adminCount, terminated, geosuspendedCountries, capabilities, wamoSubPlanId);
    }
}
