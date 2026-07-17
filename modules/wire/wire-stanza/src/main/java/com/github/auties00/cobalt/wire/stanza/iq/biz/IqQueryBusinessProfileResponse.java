package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the sealed family of inbound reply variants the relay produces in response to an {@link IqQueryBusinessProfileRequest}.
 *
 * <p>The matched variant drives merchant-profile rendering: {@link Success} carries one
 * {@link Success.Profile} entry per echoed merchant (with the cart UI, contact details, business
 * hours, linked accounts, direct-connection block, bot prompts and commands already decoded),
 * {@link ClientError} surfaces a rejected request and {@link ServerError} surfaces a transient
 * internal failure.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryBusinessProfileJob")
public sealed interface IqQueryBusinessProfileResponse extends IqStanza.Response
        permits IqQueryBusinessProfileResponse.Success, IqQueryBusinessProfileResponse.ClientError, IqQueryBusinessProfileResponse.ServerError {

    /**
     * Tries each variant in priority order until one matches.
     *
     * <p>The order is {@link Success}, then {@link ClientError}, then {@link ServerError}.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    static Optional<? extends IqQueryBusinessProfileResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Carries one {@link Profile} entry per echoed {@code <profile/>} child for a successful fetch.
     *
     * <p>The entries from {@link #profiles()} refresh the business-profile collection in wire
     * order, which matches the caller-supplied entry order on the {@link IqQueryBusinessProfileRequest}.
     */
    final class Success implements IqQueryBusinessProfileResponse {
        /**
         * Models one business-profile entry fully decoded from a {@code <profile/>} child of the {@code <business_profile/>} payload.
         *
         * <p>The projection drives every merchant-rendering surface: the chat opener uses
         * {@link #description()} and {@link #email()}, the merchant directory uses
         * {@link #categories()} and {@link #coverPhoto()}, the cart UI uses {@link #profileOptions()}
         * and {@link #directConnection()}, and the bot interaction surface uses {@link #prompts()}
         * and {@link #commands()}.
         */
        public static final class Profile {
            /**
             * Holds the business JID this entry belongs to.
             */
            private final Jid businessJid;

            /**
             * Holds the version tag echoed by the relay.
             *
             * <p>Callers cache profile bodies and re-query with the same tag for conditional fetches.
             */
            private final String tag;

            /**
             * Holds the optional postal address (free text).
             */
            private final String address;

            /**
             * Holds the optional self-description body.
             */
            private final String description;

            /**
             * Holds the optional contact email address.
             */
            private final String email;

            /**
             * Holds the optional latitude of the merchant's pinned location.
             */
            private final Double latitude;

            /**
             * Holds the optional longitude of the merchant's pinned location.
             */
            private final Double longitude;

            /**
             * Holds the website URLs (zero, one, or two entries on the wire).
             */
            private final List<String> websites;

            /**
             * Holds the category entries (id plus display name).
             */
            private final List<Category> categories;

            /**
             * Holds the optional business-hours schedule.
             */
            private final BusinessHours businessHours;

            /**
             * Holds the optional catalog status (for example {@code "PENDING"} or {@code "APPROVED"}).
             */
            private final String catalogStatus;

            /**
             * Holds the optional profile-options block (commerce experience, cart-enabled flag, shop URL, and similar markers).
             */
            private final ProfileOptions profileOptions;

            /**
             * Holds the optional Facebook page link block.
             */
            private final FacebookPage facebookPage;

            /**
             * Holds the optional Instagram-professional block.
             */
            private final InstagramProfessional instagramProfessional;

            /**
             * Holds whether the merchant has any linked-account block at all (Facebook or Instagram).
             */
            private final boolean profileIsLinked;

            /**
             * Holds the optional direct-connection block (enabled flag and default postcode).
             */
            private final DirectConnection directConnection;

            /**
             * Holds the service-area entries (radius plus center).
             */
            private final List<ServiceArea> serviceAreas;

            /**
             * Holds the offering categories.
             */
            private final List<OfferingCategory> offerings;

            /**
             * Holds the optional cover photo (id plus URL).
             */
            private final CoverPhoto coverPhoto;

            /**
             * Holds the optional custom merchant URL.
             */
            private final String customUrl;

            /**
             * Holds the optional bot welcome prompts.
             */
            private final List<Prompt> prompts;

            /**
             * Holds the optional bot commands list.
             */
            private final List<Command> commands;

            /**
             * Holds the optional commands-section description text.
             */
            private final String commandsDescription;

            /**
             * Holds the optional automated-bot type marker.
             */
            private final String automatedType;

            /**
             * Holds the optional welcome-message protocol-mode marker.
             */
            private final String welcomeMessageProtocolMode;

            /**
             * Holds the optional "merchant since" display text.
             */
            private final String memberSinceText;

            /**
             * Holds the optional price-tier id.
             */
            private final String priceTierId;

            /**
             * Holds whether this profile is an authorised agent (only populated when the gating flag is on).
             */
            private final Boolean isAuthorizedAgent;

            /**
             * Holds the optional parent company name (authorised agent only).
             */
            private final String parentCompanyName;

            /**
             * Holds the optional parent company logo URL (authorised agent only).
             */
            private final String parentCompanyLogoUrl;

            /**
             * Holds the optional official-business-account phone number (authorised agent only).
             */
            private final String obaPhoneNumber;

            /**
             * Constructs a profile entry from the decoded {@code <profile/>} child.
             *
             * <p>The field-by-field optional shape matches the wire echo so the business-profile
             * collection can render the entry without further normalisation.
             *
             * @param businessJid                the business JID; never {@code null}
             * @param tag                        the version tag; may be {@code null}
             * @param address                    the postal address; may be {@code null}
             * @param description                the self-description body; may be {@code null}
             * @param email                      the contact email; may be {@code null}
             * @param latitude                   the pinned-location latitude; may be {@code null}
             * @param longitude                  the pinned-location longitude; may be {@code null}
             * @param websites                   the website URLs; never {@code null}
             * @param categories                 the category entries; never {@code null}
             * @param businessHours              the business-hours schedule; may be {@code null}
             * @param catalogStatus              the catalog status; may be {@code null}
             * @param profileOptions             the profile-options block; may be {@code null}
             * @param facebookPage               the Facebook page link block; may be {@code null}
             * @param instagramProfessional      the Instagram-professional block; may be {@code null}
             * @param profileIsLinked            whether any linked-account block is present
             * @param directConnection           the direct-connection block; may be {@code null}
             * @param serviceAreas               the service-area entries; never {@code null}
             * @param offerings                  the offering categories; never {@code null}
             * @param coverPhoto                 the cover photo; may be {@code null}
             * @param customUrl                  the custom merchant URL; may be {@code null}
             * @param prompts                    the bot welcome prompts; never {@code null}
             * @param commands                   the bot commands; never {@code null}
             * @param commandsDescription        the commands-section description; may be {@code null}
             * @param automatedType              the automated-bot type marker; may be {@code null}
             * @param welcomeMessageProtocolMode the welcome-message protocol mode; may be {@code null}
             * @param memberSinceText            the "merchant since" display text; may be {@code null}
             * @param priceTierId                the price-tier id; may be {@code null}
             * @param isAuthorizedAgent          the authorised-agent flag; may be {@code null}
             * @param parentCompanyName          the parent company name; may be {@code null}
             * @param parentCompanyLogoUrl       the parent company logo URL; may be {@code null}
             * @param obaPhoneNumber             the official-business-account phone number; may be {@code null}
             * @throws NullPointerException if {@code businessJid}, {@code websites}, {@code categories}, {@code serviceAreas}, {@code offerings}, {@code prompts}, or {@code commands} is {@code null}
             */
            public Profile(Jid businessJid,
                           String tag,
                           String address,
                           String description,
                           String email,
                           Double latitude,
                           Double longitude,
                           List<String> websites,
                           List<Category> categories,
                           BusinessHours businessHours,
                           String catalogStatus,
                           ProfileOptions profileOptions,
                           FacebookPage facebookPage,
                           InstagramProfessional instagramProfessional,
                           boolean profileIsLinked,
                           DirectConnection directConnection,
                           List<ServiceArea> serviceAreas,
                           List<OfferingCategory> offerings,
                           CoverPhoto coverPhoto,
                           String customUrl,
                           List<Prompt> prompts,
                           List<Command> commands,
                           String commandsDescription,
                           String automatedType,
                           String welcomeMessageProtocolMode,
                           String memberSinceText,
                           String priceTierId,
                           Boolean isAuthorizedAgent,
                           String parentCompanyName,
                           String parentCompanyLogoUrl,
                           String obaPhoneNumber) {
                this.businessJid = Objects.requireNonNull(businessJid, "businessJid cannot be null");
                this.tag = tag;
                this.address = address;
                this.description = description;
                this.email = email;
                this.latitude = latitude;
                this.longitude = longitude;
                Objects.requireNonNull(websites, "websites cannot be null");
                this.websites = List.copyOf(websites);
                Objects.requireNonNull(categories, "categories cannot be null");
                this.categories = List.copyOf(categories);
                this.businessHours = businessHours;
                this.catalogStatus = catalogStatus;
                this.profileOptions = profileOptions;
                this.facebookPage = facebookPage;
                this.instagramProfessional = instagramProfessional;
                this.profileIsLinked = profileIsLinked;
                this.directConnection = directConnection;
                Objects.requireNonNull(serviceAreas, "serviceAreas cannot be null");
                this.serviceAreas = List.copyOf(serviceAreas);
                Objects.requireNonNull(offerings, "offerings cannot be null");
                this.offerings = List.copyOf(offerings);
                this.coverPhoto = coverPhoto;
                this.customUrl = customUrl;
                Objects.requireNonNull(prompts, "prompts cannot be null");
                this.prompts = List.copyOf(prompts);
                Objects.requireNonNull(commands, "commands cannot be null");
                this.commands = List.copyOf(commands);
                this.commandsDescription = commandsDescription;
                this.automatedType = automatedType;
                this.welcomeMessageProtocolMode = welcomeMessageProtocolMode;
                this.memberSinceText = memberSinceText;
                this.priceTierId = priceTierId;
                this.isAuthorizedAgent = isAuthorizedAgent;
                this.parentCompanyName = parentCompanyName;
                this.parentCompanyLogoUrl = parentCompanyLogoUrl;
                this.obaPhoneNumber = obaPhoneNumber;
            }

            /**
             * Returns the merchant JID used as the business-profile collection key.
             *
             * <p>The value is taken verbatim from the {@code jid} attribute of the
             * {@code <profile/>} child.
             *
             * @return the JID; never {@code null}
             */
            public Jid businessJid() {
                return businessJid;
            }

            /**
             * Returns the version tag, when supplied.
             *
             * <p>Callers cache the profile body and replay the tag on a future conditional fetch;
             * an empty optional means the relay returned the full body unconditionally.
             *
             * @return an {@link Optional} carrying the tag
             */
            public Optional<String> tag() {
                return Optional.ofNullable(tag);
            }

            /**
             * Returns the postal address that drives the address line of the merchant profile sheet.
             *
             * <p>The relay echoes the merchant-supplied free-text address verbatim.
             *
             * @return an {@link Optional} carrying the address
             */
            public Optional<String> address() {
                return Optional.ofNullable(address);
            }

            /**
             * Returns the self-description body that drives the merchant description line of the chat opener and merchant directory.
             *
             * @return an {@link Optional} carrying the description
             */
            public Optional<String> description() {
                return Optional.ofNullable(description);
            }

            /**
             * Returns the contact email that drives the email line of the merchant profile sheet.
             *
             * @return an {@link Optional} carrying the email
             */
            public Optional<String> email() {
                return Optional.ofNullable(email);
            }

            /**
             * Returns the pinned-location latitude that, with {@link #longitude()}, drops a pin on the merchant location surface.
             *
             * @return an {@link Optional} carrying the latitude
             */
            public Optional<Double> latitude() {
                return Optional.ofNullable(latitude);
            }

            /**
             * Returns the pinned-location longitude that, with {@link #latitude()}, drops a pin on the merchant location surface.
             *
             * @return an {@link Optional} carrying the longitude
             */
            public Optional<Double> longitude() {
                return Optional.ofNullable(longitude);
            }

            /**
             * Returns the website URLs that drive the website links on the merchant profile sheet.
             *
             * <p>The relay echoes zero, one or two entries in wire order.
             *
             * @return an unmodifiable list; never {@code null}
             */
            public List<String> websites() {
                return websites;
            }

            /**
             * Returns the category entries that drive the category chips on the chat opener and merchant directory.
             *
             * <p>Each entry pairs an opaque id with a localised display name.
             *
             * @return an unmodifiable list; never {@code null}
             */
            public List<Category> categories() {
                return categories;
            }

            /**
             * Returns the business-hours schedule that drives the opening hours block on the merchant profile sheet.
             *
             * <p>An empty optional means the merchant has not configured hours.
             *
             * @return an {@link Optional} carrying the schedule
             */
            public Optional<BusinessHours> businessHours() {
                return Optional.ofNullable(businessHours);
            }

            /**
             * Returns the catalog status used to dispatch on the catalog approval state.
             *
             * <p>The value (for example {@code "PENDING"} or {@code "APPROVED"}) is read from the
             * {@code status} attribute of the {@code <catalog_status/>} grandchild.
             *
             * @return an {@link Optional} carrying the status
             */
            public Optional<String> catalogStatus() {
                return Optional.ofNullable(catalogStatus);
            }

            /**
             * Returns the profile-options block carrying the cart-enabled flag, shop URL and other catalog-related markers.
             *
             * <p>An empty optional means the relay omitted the {@code <profile_options/>}
             * grandchild.
             *
             * @return an {@link Optional} carrying the block
             */
            public Optional<ProfileOptions> profileOptions() {
                return Optional.ofNullable(profileOptions);
            }

            /**
             * Returns the Facebook page link block that drives the FB-page surface of the linked-accounts panel.
             *
             * @return an {@link Optional} carrying the block
             */
            public Optional<FacebookPage> facebookPage() {
                return Optional.ofNullable(facebookPage);
            }

            /**
             * Returns the Instagram-professional link block that drives the IG-professional surface of the linked-accounts panel.
             *
             * @return an {@link Optional} carrying the block
             */
            public Optional<InstagramProfessional> instagramProfessional() {
                return Optional.ofNullable(instagramProfessional);
            }

            /**
             * Returns whether the profile carries any linked-account block at all (Facebook or Instagram).
             *
             * <p>This flag gates the visibility of the linked-accounts panel before reading the
             * per-platform blocks; a {@code true} value means at least one of {@link #facebookPage()}
             * or {@link #instagramProfessional()} is populated.
             *
             * @return {@code true} when at least one linkage is present
             */
            public boolean profileIsLinked() {
                return profileIsLinked;
            }

            /**
             * Returns the direct-connection block that drives the cart-postcode entry surface.
             *
             * <p>An empty optional means the merchant has not enrolled in the buyer-side
             * direct-connection flow.
             *
             * @return an {@link Optional} carrying the block
             */
            public Optional<DirectConnection> directConnection() {
                return Optional.ofNullable(directConnection);
            }

            /**
             * Returns the service-area entries that drive the merchant's service-area map.
             *
             * <p>Each entry pairs a center latitude/longitude with a radius in meters and a
             * free-text description.
             *
             * @return an unmodifiable list; never {@code null}
             */
            public List<ServiceArea> serviceAreas() {
                return serviceAreas;
            }

            /**
             * Returns the offering categories that drive the merchant's offering grid.
             *
             * <p>Each category groups one or more typed {@link Offering} entries.
             *
             * @return an unmodifiable list; never {@code null}
             */
            public List<OfferingCategory> offerings() {
                return offerings;
            }

            /**
             * Returns the cover photo that drives the banner of the merchant profile sheet.
             *
             * <p>An empty optional means the merchant has not uploaded a cover photo.
             *
             * @return an {@link Optional} carrying the cover photo
             */
            public Optional<CoverPhoto> coverPhoto() {
                return Optional.ofNullable(coverPhoto);
            }

            /**
             * Returns the custom merchant URL that drives the wa.me-style shareable link on the merchant profile sheet.
             *
             * <p>An empty optional means the merchant has not configured a custom URL.
             *
             * @return an {@link Optional} carrying the URL
             */
            public Optional<String> customUrl() {
                return Optional.ofNullable(customUrl);
            }

            /**
             * Returns the bot welcome prompts that drive the welcome-prompts surface the bot-enabled merchant uses to seed the conversation.
             *
             * <p>Each entry pairs an emoji with a body text.
             *
             * @return an unmodifiable list; never {@code null}
             */
            public List<Prompt> prompts() {
                return prompts;
            }

            /**
             * Returns the bot commands that drive the slash-command palette the bot-enabled merchant exposes.
             *
             * <p>Each entry pairs a command name with a description.
             *
             * @return an unmodifiable list; never {@code null}
             */
            public List<Command> commands() {
                return commands;
            }

            /**
             * Returns the commands-section description that drives the header text above the slash-command palette.
             *
             * @return an {@link Optional} carrying the description
             */
            public Optional<String> commandsDescription() {
                return Optional.ofNullable(commandsDescription);
            }

            /**
             * Returns the automated-bot type marker used to dispatch on the bot's automation type.
             *
             * <p>The value is consumed by {@code WAWebBotTypes.BizBotAutomatedType.cast}; an empty
             * optional means the merchant is not a bot.
             *
             * @return an {@link Optional} carrying the type marker
             */
            public Optional<String> automatedType() {
                return Optional.ofNullable(automatedType);
            }

            /**
             * Returns the welcome-message protocol mode the merchant has enrolled in.
             *
             * <p>Bots use the mode to decide whether to surface the welcome message as a system
             * stanza or a normal chat message.
             *
             * @return an {@link Optional} carrying the mode marker
             */
            public Optional<String> welcomeMessageProtocolMode() {
                return Optional.ofNullable(welcomeMessageProtocolMode);
            }

            /**
             * Returns the "merchant since" display text that drives the merchant-tenure line on the merchant profile sheet.
             *
             * <p>The relay echoes a pre-localised string.
             *
             * @return an {@link Optional} carrying the text
             */
            public Optional<String> memberSinceText() {
                return Optional.ofNullable(memberSinceText);
            }

            /**
             * Returns the price-tier id that drives the price-tier symbol on the merchant profile sheet.
             *
             * <p>The id keys into {@code WAWebBizGetPriceTiersQuery.getCachedPriceTierById} to
             * resolve the symbol and description.
             *
             * @return an {@link Optional} carrying the id
             */
            public Optional<String> priceTierId() {
                return Optional.ofNullable(priceTierId);
            }

            /**
             * Returns the authorised-agent flag that gates the rendering of the parent-company block.
             *
             * <p>An empty optional means the relay did not include the {@code <authorized_agent/>}
             * grandchild.
             *
             * @return an {@link Optional} carrying the flag
             */
            public Optional<Boolean> isAuthorizedAgent() {
                return Optional.ofNullable(isAuthorizedAgent);
            }

            /**
             * Returns the parent company name that, with {@link #parentCompanyLogoUrl()}, drives the parent-company block.
             *
             * <p>Only populated when {@link #isAuthorizedAgent()} resolves to {@code true}.
             *
             * @return an {@link Optional} carrying the name
             */
            public Optional<String> parentCompanyName() {
                return Optional.ofNullable(parentCompanyName);
            }

            /**
             * Returns the parent company logo URL that, with {@link #parentCompanyName()}, drives the parent-company block.
             *
             * <p>Only populated when {@link #isAuthorizedAgent()} resolves to {@code true}.
             *
             * @return an {@link Optional} carrying the URL
             */
            public Optional<String> parentCompanyLogoUrl() {
                return Optional.ofNullable(parentCompanyLogoUrl);
            }

            /**
             * Returns the official-business-account phone number shown in the OBA disclosure line.
             *
             * <p>Surfaced when {@link #isAuthorizedAgent()} resolves to {@code true}; the value is
             * the underlying business's contact number.
             *
             * @return an {@link Optional} carrying the number
             */
            public Optional<String> obaPhoneNumber() {
                return Optional.ofNullable(obaPhoneNumber);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Profile) obj;
                return this.profileIsLinked == that.profileIsLinked
                        && Objects.equals(this.businessJid, that.businessJid)
                        && Objects.equals(this.tag, that.tag)
                        && Objects.equals(this.address, that.address)
                        && Objects.equals(this.description, that.description)
                        && Objects.equals(this.email, that.email)
                        && Objects.equals(this.latitude, that.latitude)
                        && Objects.equals(this.longitude, that.longitude)
                        && Objects.equals(this.websites, that.websites)
                        && Objects.equals(this.categories, that.categories)
                        && Objects.equals(this.businessHours, that.businessHours)
                        && Objects.equals(this.catalogStatus, that.catalogStatus)
                        && Objects.equals(this.profileOptions, that.profileOptions)
                        && Objects.equals(this.facebookPage, that.facebookPage)
                        && Objects.equals(this.instagramProfessional, that.instagramProfessional)
                        && Objects.equals(this.directConnection, that.directConnection)
                        && Objects.equals(this.serviceAreas, that.serviceAreas)
                        && Objects.equals(this.offerings, that.offerings)
                        && Objects.equals(this.coverPhoto, that.coverPhoto)
                        && Objects.equals(this.customUrl, that.customUrl)
                        && Objects.equals(this.prompts, that.prompts)
                        && Objects.equals(this.commands, that.commands)
                        && Objects.equals(this.commandsDescription, that.commandsDescription)
                        && Objects.equals(this.automatedType, that.automatedType)
                        && Objects.equals(this.welcomeMessageProtocolMode, that.welcomeMessageProtocolMode)
                        && Objects.equals(this.memberSinceText, that.memberSinceText)
                        && Objects.equals(this.priceTierId, that.priceTierId)
                        && Objects.equals(this.isAuthorizedAgent, that.isAuthorizedAgent)
                        && Objects.equals(this.parentCompanyName, that.parentCompanyName)
                        && Objects.equals(this.parentCompanyLogoUrl, that.parentCompanyLogoUrl)
                        && Objects.equals(this.obaPhoneNumber, that.obaPhoneNumber);
            }

            @Override
            public int hashCode() {
                var h = Objects.hash(businessJid, tag, address, description, email,
                        latitude, longitude, websites, categories, businessHours,
                        catalogStatus, profileOptions, facebookPage, instagramProfessional,
                        profileIsLinked, directConnection, serviceAreas, offerings,
                        coverPhoto, customUrl);
                return 31 * h + Objects.hash(prompts, commands, commandsDescription,
                        automatedType, welcomeMessageProtocolMode, memberSinceText,
                        priceTierId, isAuthorizedAgent, parentCompanyName,
                        parentCompanyLogoUrl, obaPhoneNumber);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.Profile[businessJid=" + businessJid
                        + ", tag=" + tag + ", address=" + address + ", description=" + description
                        + ", email=" + email + ", websites=" + websites
                        + ", categories=" + categories + ']';
            }
        }

        /**
         * Models one business category entry: opaque id plus localised display name.
         *
         * <p>The entry drives the category chips on the chat opener and merchant directory; the
         * same id is also used as the lookup key against the {@link IqQueryBusinessCategoriesResponse}
         * cache when the merchant directory needs the parent or sibling chain.
         */
        public static final class Category {
            /**
             * Holds the opaque category id.
             */
            private final String id;

            /**
             * Holds the localised display name.
             */
            private final String localizedDisplayName;

            /**
             * Constructs a category from the decoded {@code <category/>} child.
             *
             * @param id                   the id; never {@code null}
             * @param localizedDisplayName the display name; never {@code null}
             * @throws NullPointerException if either argument is {@code null}
             */
            public Category(String id, String localizedDisplayName) {
                this.id = Objects.requireNonNull(id, "id cannot be null");
                this.localizedDisplayName = Objects.requireNonNull(
                        localizedDisplayName, "localizedDisplayName cannot be null");
            }

            /**
             * Returns the opaque category id, which also keys into the business-categories cache.
             *
             * @return the id; never {@code null}
             */
            public String id() {
                return id;
            }

            /**
             * Returns the localised display name that drives the category chip label.
             *
             * <p>The relay echoes a pre-localised string.
             *
             * @return the display name; never {@code null}
             */
            public String localizedDisplayName() {
                return localizedDisplayName;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Category) obj;
                return Objects.equals(this.id, that.id)
                        && Objects.equals(this.localizedDisplayName, that.localizedDisplayName);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id, localizedDisplayName);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.Category[id=" + id
                        + ", localizedDisplayName=" + localizedDisplayName + ']';
            }
        }

        /**
         * Models the merchant's business-hours schedule.
         *
         * <p>The schedule drives the opening-hours surface on the merchant profile sheet; the
         * timezone is the merchant's configured IANA zone and the config list carries one window
         * per (day of week, mode) tuple.
         */
        public static final class BusinessHours {
            /**
             * Holds the optional IANA timezone identifier.
             */
            private final String timezone;

            /**
             * Holds the per-(day-of-week) opening windows.
             */
            private final List<BusinessHoursConfig> config;

            /**
             * Constructs a schedule from the decoded {@code <business_hours/>} grandchild.
             *
             * @param timezone the optional timezone; may be {@code null}
             * @param config   the per-day windows; never {@code null}
             * @throws NullPointerException if {@code config} is {@code null}
             */
            public BusinessHours(String timezone, List<BusinessHoursConfig> config) {
                this.timezone = timezone;
                Objects.requireNonNull(config, "config cannot be null");
                this.config = List.copyOf(config);
            }

            /**
             * Returns the IANA timezone identifier used to render the opening-hours surface in the merchant's local time.
             *
             * <p>An empty optional means the relay omitted the timezone attribute.
             *
             * @return an {@link Optional} carrying the timezone
             */
            public Optional<String> timezone() {
                return Optional.ofNullable(timezone);
            }

            /**
             * Returns the per-day opening windows that drive the daily opening-hours rows.
             *
             * <p>The list may contain multiple entries for the same day when the merchant configures
             * multiple windows (for example a lunch break).
             *
             * @return an unmodifiable list; never {@code null}
             */
            public List<BusinessHoursConfig> config() {
                return config;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (BusinessHours) obj;
                return Objects.equals(this.timezone, that.timezone)
                        && Objects.equals(this.config, that.config);
            }

            @Override
            public int hashCode() {
                return Objects.hash(timezone, config);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.BusinessHours[timezone="
                        + timezone + ", config=" + config + ']';
            }
        }

        /**
         * Models one business-hours window on a {@link BusinessHours}: day of week, mode, optional open and close times.
         *
         * <p>The row drives one line of the opening-hours surface; the mode dispatches the
         * rendering (specific hours, appointment only, closed).
         */
        public static final class BusinessHoursConfig {
            /**
             * Holds the day of week (for example {@code "mon"}).
             */
            private final String dayOfWeek;

            /**
             * Holds the mode (for example {@code "open_specific_hours"}, {@code "appointment_only"}, or {@code "closed"}).
             */
            private final String mode;

            /**
             * Holds the opening time in minutes since midnight.
             */
            private final int openTime;

            /**
             * Holds the closing time in minutes since midnight.
             */
            private final int closeTime;

            /**
             * Constructs a window from the decoded {@code <business_hours_config/>} child.
             *
             * @param dayOfWeek the day of week; never {@code null}
             * @param mode      the mode; never {@code null}
             * @param openTime  the opening time in minutes since midnight
             * @param closeTime the closing time in minutes since midnight
             * @throws NullPointerException if either string is {@code null}
             */
            public BusinessHoursConfig(String dayOfWeek, String mode, int openTime, int closeTime) {
                this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek cannot be null");
                this.mode = Objects.requireNonNull(mode, "mode cannot be null");
                this.openTime = openTime;
                this.closeTime = closeTime;
            }

            /**
             * Returns the day of week used as the row label.
             *
             * <p>The relay echoes a short day name (for example {@code "mon"} or {@code "tue"}).
             *
             * @return the day of week; never {@code null}
             */
            public String dayOfWeek() {
                return dayOfWeek;
            }

            /**
             * Returns the rendering mode used to dispatch on the row.
             *
             * <p>{@code "open_specific_hours"} gates rendering of the open and close times;
             * {@code "appointment_only"} and {@code "closed"} short-circuit the row.
             *
             * @return the mode; never {@code null}
             */
            public String mode() {
                return mode;
            }

            /**
             * Returns the opening time in minutes since midnight.
             *
             * <p>Meaningful only when {@link #mode()} is {@code "open_specific_hours"}; renders the
             * open window together with {@link #closeTime()}.
             *
             * @return the opening time
             */
            public int openTime() {
                return openTime;
            }

            /**
             * Returns the closing time in minutes since midnight.
             *
             * <p>Meaningful only when {@link #mode()} is {@code "open_specific_hours"}; renders the
             * open window together with {@link #openTime()}.
             *
             * @return the closing time
             */
            public int closeTime() {
                return closeTime;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (BusinessHoursConfig) obj;
                return this.openTime == that.openTime
                        && this.closeTime == that.closeTime
                        && Objects.equals(this.dayOfWeek, that.dayOfWeek)
                        && Objects.equals(this.mode, that.mode);
            }

            @Override
            public int hashCode() {
                return Objects.hash(dayOfWeek, mode, openTime, closeTime);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.BusinessHoursConfig[dayOfWeek="
                        + dayOfWeek + ", mode=" + mode + ", openTime=" + openTime
                        + ", closeTime=" + closeTime + ']';
            }
        }

        /**
         * Models the catalog and cart options block on a {@link Profile}.
         *
         * <p>The block carries the commerce experience marker, cart-enabled flag, shop URL,
         * commerce-manager URL, banned and direct-connection markers and the profile-edit lock; it
         * drives the catalog-grid affordance, commerce-manager deep link and ban-state badge on the
         * merchant profile sheet. Every field is optional, and an empty optional means the relay
         * omitted the corresponding grandchild.
         */
        public static final class ProfileOptions {
            /**
             * Holds the commerce experience marker (for example {@code "NONE"} or {@code "PURPLE_MOON"}).
             */
            private final String commerceExperience;

            /**
             * Holds whether the cart is enabled.
             */
            private final Boolean cartEnabled;

            /**
             * Holds the optional shop URL.
             */
            private final String shopUrl;

            /**
             * Holds the optional commerce-manager URL.
             */
            private final String commerceManagerUrl;

            /**
             * Holds whether the merchant has been banned.
             */
            private final Boolean banned;

            /**
             * Holds whether direct-connection is enabled.
             */
            private final Boolean directConnection;

            /**
             * Holds whether profile editing is disabled.
             */
            private final Boolean profileEditDisabled;

            /**
             * Constructs an options block from the decoded {@code <profile_options/>} grandchild.
             *
             * <p>Each field is independently optional on the wire.
             *
             * @param commerceExperience  the commerce experience marker; may be {@code null}
             * @param cartEnabled         the cart-enabled flag; may be {@code null}
             * @param shopUrl             the shop URL; may be {@code null}
             * @param commerceManagerUrl  the commerce-manager URL; may be {@code null}
             * @param banned              the banned flag; may be {@code null}
             * @param directConnection    the direct-connection flag; may be {@code null}
             * @param profileEditDisabled the profile-edit-disabled flag; may be {@code null}
             */
            public ProfileOptions(String commerceExperience,
                                  Boolean cartEnabled,
                                  String shopUrl,
                                  String commerceManagerUrl,
                                  Boolean banned,
                                  Boolean directConnection,
                                  Boolean profileEditDisabled) {
                this.commerceExperience = commerceExperience;
                this.cartEnabled = cartEnabled;
                this.shopUrl = shopUrl;
                this.commerceManagerUrl = commerceManagerUrl;
                this.banned = banned;
                this.directConnection = directConnection;
                this.profileEditDisabled = profileEditDisabled;
            }

            /**
             * Returns the commerce experience marker used to dispatch on the commerce-experience track.
             *
             * <p>The value (for example {@code "NONE"} or {@code "PURPLE_MOON"}) gates which catalog
             * surface the cart UI presents.
             *
             * @return an {@link Optional} carrying the marker
             */
            public Optional<String> commerceExperience() {
                return Optional.ofNullable(commerceExperience);
            }

            /**
             * Returns the cart-enabled flag that gates the "add to cart" affordance on the catalog card.
             *
             * <p>An empty optional means the relay omitted the flag and the caller should default to
             * disabled.
             *
             * @return an {@link Optional} carrying the flag
             */
            public Optional<Boolean> cartEnabled() {
                return Optional.ofNullable(cartEnabled);
            }

            /**
             * Returns the shop URL that drives the "open shop" CTA on the merchant profile sheet.
             *
             * @return an {@link Optional} carrying the URL
             */
            public Optional<String> shopUrl() {
                return Optional.ofNullable(shopUrl);
            }

            /**
             * Returns the commerce-manager URL that drives the deep link to Meta's commerce-manager surface when the calling user owns the merchant.
             *
             * @return an {@link Optional} carrying the URL
             */
            public Optional<String> commerceManagerUrl() {
                return Optional.ofNullable(commerceManagerUrl);
            }

            /**
             * Returns the banned flag that drives the ban-state badge on the merchant profile sheet.
             *
             * <p>An empty optional means the relay omitted the flag.
             *
             * @return an {@link Optional} carrying the flag
             */
            public Optional<Boolean> banned() {
                return Optional.ofNullable(banned);
            }

            /**
             * Returns the direct-connection flag that gates the cart-postcode entry surface.
             *
             * <p>An empty optional means the relay omitted the flag and the caller should default to
             * disabled.
             *
             * @return an {@link Optional} carrying the flag
             */
            public Optional<Boolean> directConnection() {
                return Optional.ofNullable(directConnection);
            }

            /**
             * Returns the profile-edit-disabled flag that gates write affordances on the merchant-profile edit surface.
             *
             * <p>The relay sets the flag when the merchant has lost edit capability.
             *
             * @return an {@link Optional} carrying the flag
             */
            public Optional<Boolean> profileEditDisabled() {
                return Optional.ofNullable(profileEditDisabled);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (ProfileOptions) obj;
                return Objects.equals(this.commerceExperience, that.commerceExperience)
                        && Objects.equals(this.cartEnabled, that.cartEnabled)
                        && Objects.equals(this.shopUrl, that.shopUrl)
                        && Objects.equals(this.commerceManagerUrl, that.commerceManagerUrl)
                        && Objects.equals(this.banned, that.banned)
                        && Objects.equals(this.directConnection, that.directConnection)
                        && Objects.equals(this.profileEditDisabled, that.profileEditDisabled);
            }

            @Override
            public int hashCode() {
                return Objects.hash(commerceExperience, cartEnabled, shopUrl,
                        commerceManagerUrl, banned, directConnection, profileEditDisabled);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.ProfileOptions[commerceExperience="
                        + commerceExperience + ", cartEnabled=" + cartEnabled
                        + ", shopUrl=" + shopUrl + ']';
            }
        }

        /**
         * Models the Facebook page link block on a {@link Profile}.
         *
         * <p>The block drives the FB-page row of the linked-accounts panel; every field is optional,
         * and an empty optional means the relay omitted the corresponding grandchild.
         */
        public static final class FacebookPage {
            /**
             * Holds the optional FB page id.
             */
            private final String id;

            /**
             * Holds the optional display name.
             */
            private final String displayName;

            /**
             * Holds the optional page-likes count.
             */
            private final Integer likes;

            /**
             * Constructs a block from the decoded {@code <fb_page/>} child.
             *
             * @param id          the id; may be {@code null}
             * @param displayName the display name; may be {@code null}
             * @param likes       the likes count; may be {@code null}
             */
            public FacebookPage(String id, String displayName, Integer likes) {
                this.id = id;
                this.displayName = displayName;
                this.likes = likes;
            }

            /**
             * Returns the FB page id that drives the deep link from the linked-accounts panel into the Facebook app.
             *
             * @return an {@link Optional} carrying the id
             */
            public Optional<String> id() {
                return Optional.ofNullable(id);
            }

            /**
             * Returns the display name that drives the FB-page label of the linked-accounts panel.
             *
             * @return an {@link Optional} carrying the name
             */
            public Optional<String> displayName() {
                return Optional.ofNullable(displayName);
            }

            /**
             * Returns the page-likes count that drives the like-count badge of the linked-accounts panel.
             *
             * @return an {@link Optional} carrying the count
             */
            public Optional<Integer> likes() {
                return Optional.ofNullable(likes);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (FacebookPage) obj;
                return Objects.equals(this.id, that.id)
                        && Objects.equals(this.displayName, that.displayName)
                        && Objects.equals(this.likes, that.likes);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id, displayName, likes);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.FacebookPage[id=" + id
                        + ", displayName=" + displayName + ", likes=" + likes + ']';
            }
        }

        /**
         * Models the Instagram-professional link block on a {@link Profile}.
         *
         * <p>The block drives the IG-professional row of the linked-accounts panel.
         */
        public static final class InstagramProfessional {
            /**
             * Holds the optional IG handle.
             */
            private final String igHandle;

            /**
             * Holds the optional follower count.
             */
            private final Integer followers;

            /**
             * Constructs a block from the decoded {@code <ig_professional/>} child.
             *
             * @param igHandle  the IG handle; may be {@code null}
             * @param followers the follower count; may be {@code null}
             */
            public InstagramProfessional(String igHandle, Integer followers) {
                this.igHandle = igHandle;
                this.followers = followers;
            }

            /**
             * Returns the IG handle that drives the IG-professional label of the linked-accounts panel and the deep link into the Instagram app.
             *
             * @return an {@link Optional} carrying the handle
             */
            public Optional<String> igHandle() {
                return Optional.ofNullable(igHandle);
            }

            /**
             * Returns the follower count that drives the follower-count badge of the linked-accounts panel.
             *
             * @return an {@link Optional} carrying the count
             */
            public Optional<Integer> followers() {
                return Optional.ofNullable(followers);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (InstagramProfessional) obj;
                return Objects.equals(this.igHandle, that.igHandle)
                        && Objects.equals(this.followers, that.followers);
            }

            @Override
            public int hashCode() {
                return Objects.hash(igHandle, followers);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.InstagramProfessional[igHandle="
                        + igHandle + ", followers=" + followers + ']';
            }
        }

        /**
         * Models the direct-connection block on a {@link Profile}: enabled flag plus the optional default-postcode echo.
         *
         * <p>The block drives the cart-postcode entry surface; the default postcode is the address
         * the buyer previously verified against the merchant, so the surface can pre-populate the
         * chip.
         */
        public static final class DirectConnection {
            /**
             * Holds whether direct-connection is enabled.
             */
            private final boolean enabled;

            /**
             * Holds the optional default-postcode echo.
             */
            private final DefaultPostcode defaultPostcode;

            /**
             * Constructs a block from the decoded {@code <direct_connection/>} child.
             *
             * @param enabled         the enabled flag
             * @param defaultPostcode the default postcode; may be {@code null}
             */
            public DirectConnection(boolean enabled, DefaultPostcode defaultPostcode) {
                this.enabled = enabled;
                this.defaultPostcode = defaultPostcode;
            }

            /**
             * Returns the enabled flag that gates the cart-postcode entry surface.
             *
             * <p>A {@code true} value unlocks the surface for the merchant.
             *
             * @return {@code true} when direct-connection is enabled
             */
            public boolean enabled() {
                return enabled;
            }

            /**
             * Returns the default-postcode echo used to pre-populate the postcode chip on the cart-postcode entry surface.
             *
             * <p>An empty optional means the buyer has not yet verified an address with the merchant.
             *
             * @return an {@link Optional} carrying the block
             */
            public Optional<DefaultPostcode> defaultPostcode() {
                return Optional.ofNullable(defaultPostcode);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (DirectConnection) obj;
                return this.enabled == that.enabled
                        && Objects.equals(this.defaultPostcode, that.defaultPostcode);
            }

            @Override
            public int hashCode() {
                return Objects.hash(enabled, defaultPostcode);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.DirectConnection[enabled="
                        + enabled + ", defaultPostcode=" + defaultPostcode + ']';
            }
        }

        /**
         * Models the default-postcode block on a {@link DirectConnection}: postcode value plus the location-name label.
         *
         * <p>The block pre-populates the postcode chip on the cart-postcode entry surface; the
         * location-name label is the resolved address the UI surfaces above the chip.
         */
        public static final class DefaultPostcode {
            /**
             * Holds the postcode value.
             */
            private final String code;

            /**
             * Holds the location-name label resolved from the postcode.
             */
            private final String locationName;

            /**
             * Constructs a block from the decoded {@code <default_postcode/>} child.
             *
             * @param code         the postcode; never {@code null}
             * @param locationName the location label; never {@code null}
             * @throws NullPointerException if either argument is {@code null}
             */
            public DefaultPostcode(String code, String locationName) {
                this.code = Objects.requireNonNull(code, "code cannot be null");
                this.locationName = Objects.requireNonNull(locationName, "locationName cannot be null");
            }

            /**
             * Returns the postcode value used to pre-populate the postcode chip.
             *
             * @return the postcode; never {@code null}
             */
            public String code() {
                return code;
            }

            /**
             * Returns the location-name label rendered as the resolved address line above the postcode chip.
             *
             * @return the label; never {@code null}
             */
            public String locationName() {
                return locationName;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (DefaultPostcode) obj;
                return Objects.equals(this.code, that.code)
                        && Objects.equals(this.locationName, that.locationName);
            }

            @Override
            public int hashCode() {
                return Objects.hash(code, locationName);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.DefaultPostcode[code=" + code
                        + ", locationName=" + locationName + ']';
            }
        }

        /**
         * Models one service-area entry on a {@link Profile}: radius in meters around a fixed latitude/longitude center plus a free-text description.
         *
         * <p>The entry renders one circle on the merchant's service-area map.
         */
        public static final class ServiceArea {
            /**
             * Holds the service radius in meters.
             */
            private final double radius;

            /**
             * Holds the center latitude.
             */
            private final double latitude;

            /**
             * Holds the center longitude.
             */
            private final double longitude;

            /**
             * Holds the free-text description.
             */
            private final String areaDescription;

            /**
             * Constructs an entry from the decoded {@code <service_area/>} child.
             *
             * @param radius          the radius in meters
             * @param latitude        the center latitude
             * @param longitude       the center longitude
             * @param areaDescription the free-text description; never {@code null}
             * @throws NullPointerException if {@code areaDescription} is {@code null}
             */
            public ServiceArea(double radius, double latitude, double longitude, String areaDescription) {
                this.radius = radius;
                this.latitude = latitude;
                this.longitude = longitude;
                this.areaDescription = Objects.requireNonNull(
                        areaDescription, "areaDescription cannot be null");
            }

            /**
             * Returns the service radius in meters used to size the rendered circle on the service-area map.
             *
             * @return the radius
             */
            public double radius() {
                return radius;
            }

            /**
             * Returns the center latitude that, with {@link #longitude()}, places the rendered circle on the service-area map.
             *
             * @return the latitude
             */
            public double latitude() {
                return latitude;
            }

            /**
             * Returns the center longitude that, with {@link #latitude()}, places the rendered circle on the service-area map.
             *
             * @return the longitude
             */
            public double longitude() {
                return longitude;
            }

            /**
             * Returns the free-text description rendered as the merchant-supplied label next to the rendered circle (for example {@code "Greater Mumbai"}).
             *
             * @return the description; never {@code null}
             */
            public String areaDescription() {
                return areaDescription;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (ServiceArea) obj;
                return Double.compare(this.radius, that.radius) == 0
                        && Double.compare(this.latitude, that.latitude) == 0
                        && Double.compare(this.longitude, that.longitude) == 0
                        && Objects.equals(this.areaDescription, that.areaDescription);
            }

            @Override
            public int hashCode() {
                return Objects.hash(radius, latitude, longitude, areaDescription);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.ServiceArea[radius=" + radius
                        + ", latitude=" + latitude + ", longitude=" + longitude
                        + ", areaDescription=" + areaDescription + ']';
            }
        }

        /**
         * Models one offering category on a {@link Profile}: id, name and list of typed offering entries.
         *
         * <p>The category renders one section of the merchant's offering grid; each section groups
         * one or more {@link Offering} entries.
         */
        public static final class OfferingCategory {
            /**
             * Holds the category id.
             */
            private final String id;

            /**
             * Holds the category name.
             */
            private final String name;

            /**
             * Holds the list of offerings.
             */
            private final List<Offering> offerings;

            /**
             * Constructs a category from the decoded {@code <category/>} child of {@code <offerings/>}.
             *
             * @param id        the id; never {@code null}
             * @param name      the name; never {@code null}
             * @param offerings the offerings; never {@code null}
             * @throws NullPointerException if any argument is {@code null}
             */
            public OfferingCategory(String id, String name, List<Offering> offerings) {
                this.id = Objects.requireNonNull(id, "id cannot be null");
                this.name = Objects.requireNonNull(name, "name cannot be null");
                Objects.requireNonNull(offerings, "offerings cannot be null");
                this.offerings = List.copyOf(offerings);
            }

            /**
             * Returns the category id used as a stable key for the offering-grid section.
             *
             * @return the id; never {@code null}
             */
            public String id() {
                return id;
            }

            /**
             * Returns the category name used as the offering-grid section header.
             *
             * @return the name; never {@code null}
             */
            public String name() {
                return name;
            }

            /**
             * Returns the typed offerings rendered as the offering-grid rows inside the section.
             *
             * @return an unmodifiable list; never {@code null}
             */
            public List<Offering> offerings() {
                return offerings;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (OfferingCategory) obj;
                return Objects.equals(this.id, that.id)
                        && Objects.equals(this.name, that.name)
                        && Objects.equals(this.offerings, that.offerings);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id, name, offerings);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.OfferingCategory[id=" + id
                        + ", name=" + name + ", offerings=" + offerings + ']';
            }
        }

        /**
         * Models one typed offering entry on an {@link OfferingCategory}.
         *
         * <p>The entry renders one row of the offering grid; the offered flag dispatches between an
         * active row and a dimmed "not currently offered" row.
         */
        public static final class Offering {
            /**
             * Holds the offering id.
             */
            private final String id;

            /**
             * Holds the localised display name.
             */
            private final String localizedDisplayName;

            /**
             * Holds whether the offering is currently offered.
             */
            private final boolean offered;

            /**
             * Constructs an offering from the decoded {@code <offering/>} child.
             *
             * @param id                   the id; never {@code null}
             * @param localizedDisplayName the name; never {@code null}
             * @param offered              the offered flag
             * @throws NullPointerException if either string is {@code null}
             */
            public Offering(String id, String localizedDisplayName, boolean offered) {
                this.id = Objects.requireNonNull(id, "id cannot be null");
                this.localizedDisplayName = Objects.requireNonNull(
                        localizedDisplayName, "localizedDisplayName cannot be null");
                this.offered = offered;
            }

            /**
             * Returns the offering id used as a stable key for the offering-grid row.
             *
             * @return the id; never {@code null}
             */
            public String id() {
                return id;
            }

            /**
             * Returns the localised display name rendered as the row label.
             *
             * <p>The relay echoes a pre-localised string.
             *
             * @return the name; never {@code null}
             */
            public String localizedDisplayName() {
                return localizedDisplayName;
            }

            /**
             * Returns the offered flag that dispatches between an active row and a dimmed "not currently offered" row.
             *
             * @return {@code true} when currently offered
             */
            public boolean offered() {
                return offered;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Offering) obj;
                return this.offered == that.offered
                        && Objects.equals(this.id, that.id)
                        && Objects.equals(this.localizedDisplayName, that.localizedDisplayName);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id, localizedDisplayName, offered);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.Offering[id=" + id
                        + ", localizedDisplayName=" + localizedDisplayName
                        + ", offered=" + offered + ']';
            }
        }

        /**
         * Models the cover photo block on a {@link Profile}: id plus URL.
         *
         * <p>The block renders the banner of the merchant profile sheet; the URL is signed and
         * short-lived.
         */
        public static final class CoverPhoto {
            /**
             * Holds the cover photo id.
             */
            private final String id;

            /**
             * Holds the signed cover photo URL.
             */
            private final String url;

            /**
             * Constructs a block from the decoded {@code <cover_photo/>} child.
             *
             * @param id  the id; never {@code null}
             * @param url the URL; never {@code null}
             * @throws NullPointerException if either argument is {@code null}
             */
            public CoverPhoto(String id, String url) {
                this.id = Objects.requireNonNull(id, "id cannot be null");
                this.url = Objects.requireNonNull(url, "url cannot be null");
            }

            /**
             * Returns the cover photo id used as the upload-artefact id when deleting or refreshing the cover photo.
             *
             * <p>The id keys the cover-photo lifecycle via {@link IqDeleteCoverPhotoRequest} and
             * {@link IqSendCoverPhotoRequest}.
             *
             * @return the id; never {@code null}
             */
            public String id() {
                return id;
            }

            /**
             * Returns the signed cover photo URL rendered as the banner of the merchant profile sheet.
             *
             * @return the URL; never {@code null}
             */
            public String url() {
                return url;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (CoverPhoto) obj;
                return Objects.equals(this.id, that.id)
                        && Objects.equals(this.url, that.url);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id, url);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.CoverPhoto[id=" + id
                        + ", url=" + url + ']';
            }
        }

        /**
         * Models one bot welcome prompt on a {@link Profile}: emoji plus body text.
         *
         * <p>The entry renders one tappable prompt on the welcome-prompts surface; the emoji is
         * rendered as a small badge next to the body text.
         */
        public static final class Prompt {
            /**
             * Holds the emoji glyph (empty when the merchant omitted it).
             */
            private final String emoji;

            /**
             * Holds the body text.
             */
            private final String text;

            /**
             * Constructs a prompt from the decoded {@code <prompt/>} child.
             *
             * <p>The emoji is an empty string when the merchant did not supply one.
             *
             * @param emoji the emoji; never {@code null}, empty when omitted
             * @param text  the body; never {@code null}
             * @throws NullPointerException if either argument is {@code null}
             */
            public Prompt(String emoji, String text) {
                this.emoji = Objects.requireNonNull(emoji, "emoji cannot be null");
                this.text = Objects.requireNonNull(text, "text cannot be null");
            }

            /**
             * Returns the emoji glyph rendered as the small badge next to the prompt body.
             *
             * <p>An empty string means the merchant omitted the emoji.
             *
             * @return the emoji; never {@code null}
             */
            public String emoji() {
                return emoji;
            }

            /**
             * Returns the body text rendered as the prompt label on the welcome-prompts surface.
             *
             * @return the body; never {@code null}
             */
            public String text() {
                return text;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Prompt) obj;
                return Objects.equals(this.emoji, that.emoji)
                        && Objects.equals(this.text, that.text);
            }

            @Override
            public int hashCode() {
                return Objects.hash(emoji, text);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.Prompt[emoji=" + emoji
                        + ", text=" + text + ']';
            }
        }

        /**
         * Models one bot command on a {@link Profile}: name plus description.
         *
         * <p>The entry renders one row of the slash-command palette the bot-enabled merchant
         * exposes.
         */
        public static final class Command {
            /**
             * Holds the command name.
             */
            private final String name;

            /**
             * Holds the command description.
             */
            private final String description;

            /**
             * Constructs a command from the decoded {@code <command/>} child.
             *
             * @param name        the name; never {@code null}
             * @param description the description; never {@code null}
             * @throws NullPointerException if either argument is {@code null}
             */
            public Command(String name, String description) {
                this.name = Objects.requireNonNull(name, "name cannot be null");
                this.description = Objects.requireNonNull(description, "description cannot be null");
            }

            /**
             * Returns the command name rendered as the slash-command label (for example {@code "menu"} or {@code "order"}).
             *
             * @return the name; never {@code null}
             */
            public String name() {
                return name;
            }

            /**
             * Returns the command description rendered as the slash-command help text next to the label.
             *
             * @return the description; never {@code null}
             */
            public String description() {
                return description;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Command) obj;
                return Objects.equals(this.name, that.name)
                        && Objects.equals(this.description, that.description);
            }

            @Override
            public int hashCode() {
                return Objects.hash(name, description);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessProfileResponse.Success.Command[name=" + name
                        + ", description=" + description + ']';
            }
        }

        /**
         * Holds the typed profile entries in wire order.
         */
        private final List<Profile> profiles;

        /**
         * Constructs a successful reply from the decoded profile entries.
         *
         * <p>The supplied list is defensively copied so the caller may mutate the source freely
         * after construction.
         *
         * @param profiles the profile entries; never {@code null}
         * @throws NullPointerException if {@code profiles} is {@code null}
         */
        public Success(List<Profile> profiles) {
            Objects.requireNonNull(profiles, "profiles cannot be null");
            this.profiles = List.copyOf(profiles);
        }

        /**
         * Returns the typed profile entries used to refresh the business-profile collection.
         *
         * <p>The order matches the wire order, which matches the caller-supplied entry order on the
         * {@link IqQueryBusinessProfileRequest}.
         *
         * @return an unmodifiable list; never {@code null}
         */
        public List<Profile> profiles() {
            return profiles;
        }

        /**
         * Tries to parse a {@link Success} variant from the inbound stanza.
         *
         * <p>The method validates the {@code <iq type="result">} envelope and iterates over every
         * {@code <profile/>} child of the {@code <business_profile/>} payload, dispatching to
         * {@link #parseProfile(Stanza, Jid, String)} for each entry whose {@code jid} attribute
         * resolves.
         *
         * @implNote
         * This implementation matches the {@code WAWebQueryBusinessProfileJob} default export and
         * its parser dispatch through {@code WAWebCommonParsersParseBusinessProfile.default};
         * entries with missing or unparseable {@code jid} attributes are silently skipped, matching
         * the WA Web behaviour.
         *
         * @param stanza    the inbound IQ stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebQueryBusinessProfileJob",
                exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var businessProfileNode = stanza.getChild("business_profile").orElse(null);
            if (businessProfileNode == null) {
                return Optional.of(new Success(Collections.emptyList()));
            }
            var profiles = new ArrayList<Profile>();
            for (var profileNode : businessProfileNode.getChildren("profile")) {
                var jid = profileNode.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    continue;
                }
                var tag = profileNode.getAttributeAsString("tag").orElse(null);
                profiles.add(parseProfile(profileNode, jid, tag));
            }
            return Optional.of(new Success(profiles));
        }

        /**
         * Decodes a single {@code <profile/>} child into the typed {@link Profile} projection.
         *
         * <p>Reads every supported grandchild and attribute of the profile stanza and assembles a
         * fully populated entry.
         *
         * @implNote
         * This implementation mirrors {@code WAWebCommonParsersParseBusinessProfile.default};
         * fields the relay omits resolve to {@link Optional#empty()} or an empty list rather than
         * throwing, matching the WA Web behaviour.
         *
         * @param profileStanza the {@code <profile/>} child
         * @param jid         the parsed JID
         * @param tag         the parsed tag; may be {@code null}
         * @return the decoded profile entry; never {@code null}
         */
        private static Profile parseProfile(Stanza profileStanza, Jid jid, String tag) {
            var address = profileStanza.getChild("address")
                    .flatMap(Stanza::toContentString).orElse(null);
            var description = profileStanza.getChild("description")
                    .flatMap(Stanza::toContentString).orElse(null);
            var email = profileStanza.getChild("email")
                    .flatMap(Stanza::toContentString).orElse(null);
            var priceTierId = profileStanza.getChild("price_tier")
                    .flatMap(p -> p.getAttributeAsString("id")).orElse(null);
            var latitude = profileStanza.getChild("latitude")
                    .flatMap(Stanza::toContentString)
                    .map(Double::parseDouble).orElse(null);
            var longitude = profileStanza.getChild("longitude")
                    .flatMap(Stanza::toContentString)
                    .map(Double::parseDouble).orElse(null);
            var websites = new ArrayList<String>();
            for (var w : profileStanza.getChildren("website")) {
                var url = w.toContentString().orElse(null);
                if (url != null) {
                    websites.add(url);
                }
            }
            var memberSinceText = profileStanza.getChild("member_since_text")
                    .flatMap(Stanza::toContentString).orElse(null);
            Boolean isAuthorizedAgent = null;
            String parentCompanyName = null;
            String parentCompanyLogoUrl = null;
            String obaPhoneNumber = null;
            var authorizedAgentNode = profileStanza.getChild("authorized_agent").orElse(null);
            if (authorizedAgentNode != null) {
                isAuthorizedAgent = authorizedAgentNode
                        .getAttributeAsString("is_authorized_agent")
                        .map("true"::equals).orElse(null);
                parentCompanyName = authorizedAgentNode.getChild("parent_company_name")
                        .flatMap(Stanza::toContentString).orElse(null);
                parentCompanyLogoUrl = authorizedAgentNode.getChild("parent_company_logo_url")
                        .flatMap(Stanza::toContentString).orElse(null);
                obaPhoneNumber = authorizedAgentNode.getChild("oba_phone_number")
                        .flatMap(Stanza::toContentString).orElse(null);
            }
            var categories = new ArrayList<Success.Category>();
            profileStanza.getChild("categories").ifPresent(cats -> {
                for (var cat : cats.getChildren("category")) {
                    var id = cat.getAttributeAsString("id").orElse("");
                    var name = cat.toContentString().orElse("");
                    categories.add(new Success.Category(id, name));
                }
            });
            BusinessHours businessHours = null;
            var businessHoursNode = profileStanza.getChild("business_hours").orElse(null);
            if (businessHoursNode != null) {
                var timezone = businessHoursNode.getAttributeAsString("timezone").orElse(null);
                var configs = new ArrayList<BusinessHoursConfig>();
                for (var c : businessHoursNode.getChildren("business_hours_config")) {
                    var dow = c.getAttributeAsString("day_of_week").orElse("");
                    var mode = c.getAttributeAsString("mode").orElse("");
                    var openTime = c.getAttributeAsInt("open_time").orElse(0);
                    var closeTime = c.getAttributeAsInt("close_time").orElse(0);
                    configs.add(new BusinessHoursConfig(dow, mode, openTime, closeTime));
                }
                businessHours = new BusinessHours(timezone, configs);
            }
            ProfileOptions profileOptions = null;
            var profileOptionsNode = profileStanza.getChild("profile_options").orElse(null);
            if (profileOptionsNode != null) {
                var commerceExperience = profileOptionsNode.getChild("commerce_experience")
                        .flatMap(Stanza::toContentString).orElse(null);
                var cartEnabled = profileOptionsNode.getChild("cart_enabled")
                        .flatMap(Stanza::toContentString)
                        .map("true"::equals).orElse(null);
                var shopUrl = profileOptionsNode.getChild("shop_url")
                        .flatMap(Stanza::toContentString).orElse(null);
                var commerceManagerUrl = profileOptionsNode.getChild("commerce_manager_url")
                        .flatMap(Stanza::toContentString).orElse(null);
                var banned = profileOptionsNode.getChild("is_banned")
                        .flatMap(Stanza::toContentString)
                        .map("true"::equals).orElse(null);
                var dc = profileOptionsNode.getChild("direct_connection")
                        .flatMap(Stanza::toContentString)
                        .map("true"::equals).orElse(null);
                var profileEditDisabled = profileOptionsNode.getChild("is_profile_edit_disabled")
                        .flatMap(Stanza::toContentString)
                        .map("true"::equals).orElse(null);
                profileOptions = new ProfileOptions(commerceExperience, cartEnabled, shopUrl,
                        commerceManagerUrl, banned, dc, profileEditDisabled);
            }
            DirectConnection directConnection = null;
            var directConnectionNode = profileStanza.getChild("direct_connection").orElse(null);
            if (directConnectionNode != null) {
                var enabled = directConnectionNode.getAttributeAsString("enabled")
                        .map("true"::equals).orElse(false);
                DefaultPostcode defaultPostcode = null;
                var defaultPostcodeNode = directConnectionNode.getChild("default_postcode").orElse(null);
                if (defaultPostcodeNode != null) {
                    var code = defaultPostcodeNode.getAttributeAsString("code").orElse("");
                    var locName = defaultPostcodeNode.getAttributeAsString("location_name").orElse("");
                    defaultPostcode = new DefaultPostcode(code, locName);
                }
                directConnection = new DirectConnection(enabled, defaultPostcode);
            }
            var serviceAreas = new ArrayList<ServiceArea>();
            profileStanza.getChild("service_areas").ifPresent(sas -> {
                for (var sa : sas.getChildren("service_area")) {
                    var radiusNode = sa.getChild("area_radius_meters").orElse(null);
                    var centerNode = sa.getChild("area_center").orElse(null);
                    if (radiusNode == null || centerNode == null) {
                        continue;
                    }
                    var lat = centerNode.getChild("latitude").flatMap(Stanza::toContentString).orElse(null);
                    var lon = centerNode.getChild("longitude").flatMap(Stanza::toContentString).orElse(null);
                    if (lat == null || lon == null) {
                        continue;
                    }
                    var radiusValue = radiusNode.toContentString().map(Double::parseDouble).orElse(0.0);
                    var areaDescription = sa.getChild("area_description")
                            .flatMap(Stanza::toContentString).orElse("");
                    serviceAreas.add(new ServiceArea(radiusValue, Double.parseDouble(lat),
                            Double.parseDouble(lon), areaDescription));
                }
            });
            var catalogStatus = profileStanza.getChild("catalog_status")
                    .flatMap(c -> c.getAttributeAsString("status")).orElse(null);
            var offerings = new ArrayList<OfferingCategory>();
            profileStanza.getChild("offerings").ifPresent(o -> {
                for (var category : o.getChildren("category")) {
                    var id = category.getAttributeAsString("id").orElse("");
                    var name = category.getAttributeAsString("name").orElse("");
                    var entries = new ArrayList<Offering>();
                    for (var entry : category.getChildren("offering")) {
                        var entryId = entry.getAttributeAsString("id").orElse("");
                        var entryName = entry.toContentString().orElse("");
                        var offered = entry.getAttributeAsString("is_offered")
                                .map("true"::equals).orElse(false);
                        entries.add(new Offering(entryId, entryName, offered));
                    }
                    offerings.add(new OfferingCategory(id, name, entries));
                }
            });
            FacebookPage facebookPage = null;
            InstagramProfessional instagramProfessional = null;
            var profileIsLinked = false;
            var linkedAccountsNode = profileStanza.getChild("linked_accounts").orElse(null);
            if (linkedAccountsNode != null) {
                profileIsLinked = true;
                var fbNode = linkedAccountsNode.getChild("fb_page").orElse(null);
                if (fbNode != null) {
                    var id = fbNode.getAttributeAsString("id").orElse(null);
                    var displayName = fbNode.getChild("display_name")
                            .flatMap(Stanza::toContentString).orElse(null);
                    var likes = fbNode.getChild("likes")
                            .flatMap(Stanza::toContentInt).orElse(null);
                    facebookPage = new FacebookPage(id, displayName, likes);
                }
                var igNode = linkedAccountsNode.getChild("ig_professional").orElse(null);
                if (igNode != null) {
                    var igHandle = igNode.getChild("ig_handle")
                            .flatMap(Stanza::toContentString).orElse(null);
                    var followers = igNode.getChild("followers")
                            .flatMap(Stanza::toContentInt).orElse(null);
                    instagramProfessional = new InstagramProfessional(igHandle, followers);
                }
            }
            CoverPhoto coverPhoto = null;
            var coverPhotoNode = profileStanza.getChild("cover_photo").orElse(null);
            if (coverPhotoNode != null) {
                var id = coverPhotoNode.getAttributeAsString("id").orElse("");
                var url = coverPhotoNode.toContentString().orElse("");
                coverPhoto = new CoverPhoto(id, url);
            }
            var customUrl = profileStanza.getChild("custom_url")
                    .flatMap(Stanza::toContentString).orElse(null);
            var automatedType = profileStanza.getChild("automated_type")
                    .flatMap(Stanza::toContentString).orElse(null);
            var welcomeMessageProtocolMode = profileStanza.getChild("welcome_message_protocol_mode")
                    .flatMap(Stanza::toContentString).orElse(null);
            var prompts = new ArrayList<Prompt>();
            profileStanza.getChild("prompts").ifPresent(ps -> {
                for (var p : ps.getChildren("prompt")) {
                    var emoji = p.getChild("emoji")
                            .flatMap(Stanza::toContentString).orElse("");
                    var text = p.getChild("text")
                            .flatMap(Stanza::toContentString).orElse("");
                    prompts.add(new Prompt(emoji, text));
                }
            });
            var commands = new ArrayList<Command>();
            String commandsDescription = null;
            var commandsNode = profileStanza.getChild("commands").orElse(null);
            if (commandsNode != null) {
                commandsDescription = commandsNode.getChild("description")
                        .flatMap(Stanza::toContentString).orElse(null);
                for (var c : commandsNode.getChildren("command")) {
                    var name = c.getChild("name")
                            .flatMap(Stanza::toContentString).orElse("");
                    var desc = c.getChild("description")
                            .flatMap(Stanza::toContentString).orElse("");
                    commands.add(new Command(name, desc));
                }
            }
            return new Profile(jid, tag, address, description, email, latitude, longitude,
                    websites, categories, businessHours, catalogStatus, profileOptions,
                    facebookPage, instagramProfessional, profileIsLinked, directConnection,
                    serviceAreas, offerings, coverPhoto, customUrl, prompts, commands,
                    commandsDescription, automatedType, welcomeMessageProtocolMode,
                    memberSinceText, priceTierId, isAuthorizedAgent, parentCompanyName,
                    parentCompanyLogoUrl, obaPhoneNumber);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return Objects.equals(this.profiles, that.profiles);
        }

        @Override
        public int hashCode() {
            return Objects.hash(profiles);
        }

        @Override
        public String toString() {
            return "IqQueryBusinessProfileResponse.Success[profiles=" + profiles + ']';
        }
    }

    /**
     * Surfaces a relay rejection of the request as malformed or referencing an unknown merchant.
     *
     * <p>This variant carries a user-facing 4xx-class error for the merchant-profile rendering
     * surface.
     */
    final class ClientError implements IqQueryBusinessProfileResponse {
        /**
         * Holds the numeric error code echoed by the {@code <error/>} child.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the {@code <error/>} child.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from the relay's {@code <error/>} envelope.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code used to dispatch a localised message to the merchant-profile rendering surface.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text, when supplied.
         *
         * <p>The text is server-localised and not stable across snapshots.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the inbound stanza.
         *
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} to extract
         * the (code, text) envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the client-error schema
         */
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ClientError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        @Override
        public String toString() {
            return "IqQueryBusinessProfileResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Surfaces a transient internal-failure status the relay returns while processing the request.
     *
     * <p>This variant drives a backoff-and-retry path in the merchant-profile rendering surface;
     * the relay returns this shape when the business-profile backend is temporarily unavailable.
     */
    final class ServerError implements IqQueryBusinessProfileResponse {
        /**
         * Holds the numeric error code echoed by the {@code <error/>} child.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the {@code <error/>} child.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from the relay's {@code <error/>} envelope.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code; a 5xx-class value is the canonical retry trigger.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text, when supplied.
         *
         * <p>The text is server-localised and not stable across snapshots.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the inbound stanza.
         *
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} to extract
         * the (code, text) envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the server-error schema
         */
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ServerError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        @Override
        public String toString() {
            return "IqQueryBusinessProfileResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
