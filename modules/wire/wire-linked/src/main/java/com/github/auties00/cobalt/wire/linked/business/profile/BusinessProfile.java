package com.github.auties00.cobalt.wire.linked.business.profile;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents the public profile metadata of a WhatsApp Business account.
 *
 * <p>A business profile contains the information that a WhatsApp Business account owner has
 * configured to be publicly visible to users, including the business description, physical
 * address, contact email, website URLs, operating hours, and the categories under which
 * the business is classified.
 *
 * <p>The profile also carries information about the shopping cart feature and the bot
 * automation type for accounts that use automated messaging. The cart feature allows
 * customers to add items from a business catalog to a cart and submit an order as a
 * WhatsApp message. The automation type indicates whether the account is operated by a
 * first-party partially automated bot ({@link BusinessAutomatedType#PARTIAL_1P}), a
 * third-party fully automated bot ({@link BusinessAutomatedType#FULL_3P}), or is not
 * automated at all.
 *
 * <p>This profile is queried from the WhatsApp server and is associated with a specific
 * business account identified by its {@link Jid}.
 */
@ProtobufMessage
public final class BusinessProfile {
    /**
     * The JID of the business account that owns this profile.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid jid;

    /**
     * The business description text, or {@code null} if not set.
     *
     * <p>A free-form text field set by the business owner. The server enforces a maximum
     * length of 512 characters.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String description;

    /**
     * The business physical address, or {@code null} if not set.
     *
     * <p>A free-form text field representing the business's location. The server enforces
     * a maximum length of 256 characters.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String address;

    /**
     * The business contact email address, or {@code null} if not set.
     *
     * <p>Displayed on the business profile as a contact point. The server enforces a
     * maximum length of 128 characters.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String email;

    /**
     * The business operating hours schedule, or {@code null} if not configured.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    BusinessHours hours;

    /**
     * Whether the shopping cart feature is enabled for this business.
     *
     * <p>When {@code true}, customers can add items from the business catalog to a cart
     * and submit an order as a WhatsApp message.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    boolean cartEnabled;

    /**
     * The list of website URLs associated with this business.
     *
     * <p>A business profile supports up to two website URLs. Each URL is displayed as a
     * clickable link on the profile, with individual URLs limited to 256 characters.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    List<URI> websites;

    /**
     * The list of business categories classifying this business.
     *
     * <p>A business profile supports up to three categories, selected by the business owner
     * during profile setup.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    List<BusinessCategory> categories;

    /**
     * The bot automation type for this business account, or {@code null} if not set.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    BusinessAutomatedType automatedType;

    /**
     * The single cover photo (banner) attached to this profile, or {@code null} if none is set.
     *
     * <p>A business profile carries at most one cover photo; {@link BusinessCoverPhoto#id()} keys
     * its replace and delete lifecycle.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    BusinessCoverPhoto coverPhoto;

    /**
     * Constructs a new business profile with the specified properties.
     *
     * @param jid           the JID of the business account
     * @param description   the business description, or {@code null} if not set
     * @param address       the business physical address, or {@code null} if not set
     * @param email         the business contact email, or {@code null} if not set
     * @param hours         the operating hours schedule, or {@code null} if not configured
     * @param cartEnabled   whether the shopping cart feature is enabled
     * @param websites      the list of website URLs, or {@code null} for an empty list
     * @param categories    the list of business categories, or {@code null} for an empty list
     * @param automatedType the bot automation type, or {@code null} if not set
     * @param coverPhoto    the cover photo, or {@code null} if none is set
     */
    BusinessProfile(Jid jid, String description, String address, String email, BusinessHours hours, boolean cartEnabled, List<URI> websites, List<BusinessCategory> categories, BusinessAutomatedType automatedType, BusinessCoverPhoto coverPhoto) {
        this.jid = jid;
        this.description = description;
        this.address = address;
        this.email = email;
        this.hours = hours;
        this.cartEnabled = cartEnabled;
        this.websites = websites;
        this.categories = categories;
        this.automatedType = automatedType;
        this.coverPhoto = coverPhoto;
    }

    /**
     * Returns the JID of the business account that owns this profile.
     *
     * @return the business account JID
     */
    public Jid jid() {
        return jid;
    }

    /**
     * Sets the JID of the business account that owns this profile.
     *
     * @param jid the business account JID
     */
    public void setJid(Jid jid) {
        this.jid = jid;
    }

    /**
     * Returns the business description, if available.
     *
     * <p>The description is a free-form text field set by the business owner that describes
     * the nature and services of the business. The maximum length enforced by the server
     * is 512 characters.
     *
     * @return an {@link Optional} containing the description, or an empty {@code Optional}
     *         if not set
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Sets the business description.
     *
     * @param description the description text, or {@code null} to clear
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the business physical address, if available.
     *
     * <p>The address is a free-form text field representing the physical location of the
     * business. The maximum length enforced by the server is 256 characters.
     *
     * @return an {@link Optional} containing the address, or an empty {@code Optional}
     *         if not set
     */
    public Optional<String> address() {
        return Optional.ofNullable(address);
    }

    /**
     * Sets the business physical address.
     *
     * @param address the physical address, or {@code null} to clear
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Returns the business contact email address, if available.
     *
     * <p>The email address is a contact point displayed on the business profile. The
     * maximum length enforced by the server is 128 characters.
     *
     * @return an {@link Optional} containing the email address, or an empty {@code Optional}
     *         if not set
     */
    public Optional<String> email() {
        return Optional.ofNullable(email);
    }

    /**
     * Sets the business contact email address.
     *
     * @param email the email address, or {@code null} to clear
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the business operating hours schedule, if configured.
     *
     * @return an {@link Optional} containing the {@link BusinessHours}, or an empty
     *         {@code Optional} if not configured
     */
    public Optional<BusinessHours> hours() {
        return Optional.ofNullable(hours);
    }

    /**
     * Sets the business operating hours schedule.
     *
     * @param hours the operating hours schedule, or {@code null} to clear
     */
    public void setHours(BusinessHours hours) {
        this.hours = hours;
    }

    /**
     * Returns whether the shopping cart feature is enabled for this business.
     *
     * <p>When enabled, customers can add items from the business catalog to a cart and
     * submit an order as a WhatsApp message.
     *
     * @return {@code true} if the cart feature is enabled, otherwise {@code false}
     */
    public boolean cartEnabled() {
        return cartEnabled;
    }

    /**
     * Sets whether the shopping cart feature is enabled for this business.
     *
     * @param cartEnabled {@code true} to enable the cart feature, {@code false} to disable it
     */
    public void setCartEnabled(boolean cartEnabled) {
        this.cartEnabled = cartEnabled;
    }

    /**
     * Returns an unmodifiable view of the website URLs associated with this business.
     *
     * <p>A business profile supports up to two website URLs. Each URL is displayed as a
     * clickable link on the profile, with individual URLs limited to 256 characters.
     *
     * @return a non-{@code null}, unmodifiable list of website URLs
     */
    public List<URI> websites() {
        return websites == null ? List.of() : Collections.unmodifiableList(websites);
    }

    /**
     * Sets the list of website URLs associated with this business.
     *
     * @param websites the list of website URLs, or {@code null} for an empty list
     */
    public void setWebsites(List<URI> websites) {
        this.websites = websites;
    }

    /**
     * Returns an unmodifiable view of the business categories classifying this business.
     *
     * <p>Categories are assigned by the business owner during profile setup and help users
     * understand the nature and industry of the business. A profile supports up to three
     * categories.
     *
     * @return a non-{@code null}, unmodifiable list of business categories
     */
    public List<BusinessCategory> categories() {
        return categories == null ? List.of() : Collections.unmodifiableList(categories);
    }

    /**
     * Sets the list of business categories classifying this business.
     *
     * @param categories the list of categories, or {@code null} for an empty list
     */
    public void setCategories(List<BusinessCategory> categories) {
        this.categories = categories;
    }

    /**
     * Returns the bot automation type for this business account, if available.
     *
     * <p>The automation type indicates the level and source of automated messaging
     * configured for this business.
     *
     * @return an {@link Optional} containing the {@link BusinessAutomatedType}, or an
     *         empty {@code Optional} if not set
     */
    public Optional<BusinessAutomatedType> automatedType() {
        return Optional.ofNullable(automatedType);
    }

    /**
     * Sets the bot automation type for this business account.
     *
     * @param automatedType the automation type, or {@code null} to clear
     */
    public void setAutomatedType(BusinessAutomatedType automatedType) {
        this.automatedType = automatedType;
    }

    /**
     * Returns the cover photo attached to this profile, if set.
     *
     * <p>A business profile carries at most one cover photo.
     *
     * @return an {@link Optional} containing the {@link BusinessCoverPhoto}, or an empty
     *         {@code Optional} if none is set
     */
    public Optional<BusinessCoverPhoto> coverPhoto() {
        return Optional.ofNullable(coverPhoto);
    }

    /**
     * Sets the cover photo attached to this profile.
     *
     * @param coverPhoto the cover photo, or {@code null} to clear
     */
    public void setCoverPhoto(BusinessCoverPhoto coverPhoto) {
        this.coverPhoto = coverPhoto;
    }
}
