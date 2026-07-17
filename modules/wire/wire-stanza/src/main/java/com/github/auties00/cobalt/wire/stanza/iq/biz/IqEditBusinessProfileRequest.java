package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The typed outbound {@code <iq xmlns="w:biz" type="set">} stanza that mutates the current merchant's business profile.
 *
 * <p>The SMB profile editor sends this request to patch any subset of the merchant's profile fields in a single
 * round-trip. Each non-{@code null} field becomes one child of the {@code <business_profile/>} delta envelope, so a
 * caller can patch the address, the geo coordinates, the description, the email, the website slots, the category list,
 * the business hours, the price tier or the service-area list without touching the other fields. {@link #builder()}
 * offers a fluent alternative when only a small subset of fields applies.
 *
 * @implNote
 * This implementation sends a {@code mutation_type="delta"} envelope: passing an empty website list clears all website
 * slots, passing a {@code null} value for any field leaves the relay's cached value untouched, and the per-row payload
 * mirrors the canonical wire shape produced by the WA Web profile editor.
 */
@WhatsAppWebModule(moduleName = "WAWebBusinessProfileJob")
public final class IqEditBusinessProfileRequest implements IqStanza.Request {
    /**
     * The optional address line emitted as the {@code <address/>} child content.
     */
    private final String address;

    /**
     * The optional latitude emitted as the {@code <latitude/>} child content.
     */
    private final Double latitude;

    /**
     * The optional longitude emitted as the {@code <longitude/>} child content.
     */
    private final Double longitude;

    /**
     * The optional self-description emitted as the {@code <description/>} child content.
     */
    private final String description;

    /**
     * The optional contact email emitted as the {@code <email/>} child content.
     */
    private final String email;

    /**
     * The optional list of website slots; an empty list clears all slots and a non-{@code null} value overrides
     * whatever the relay had cached.
     */
    private final List<IqEditBusinessProfileWebsite> websites;

    /**
     * The optional list of business-category identifiers emitted as {@code <categories><category id/></categories>}
     * children.
     */
    private final List<String> categories;

    /**
     * The optional business-hours payload emitted as the {@code <business_hours/>} child.
     */
    private final IqEditBusinessProfileBusinessHours businessHours;

    /**
     * The optional price-tier identifier; a non-{@code null} value triggers the {@code <price_tier/>} child.
     */
    private final String priceTierId;

    /**
     * The optional list of service-area entries emitted as the {@code <service_areas/>} child.
     */
    private final List<IqEditBusinessProfileServiceArea> serviceAreas;

    /**
     * Constructs a typed request directly from every field.
     *
     * <p>Pass {@code null} for any field that should keep the relay's cached value unchanged. {@link #builder()} offers
     * a fluent alternative when only a small subset of fields applies.
     *
     * @param address       see {@link #address()}
     * @param latitude      see {@link #latitude()}
     * @param longitude     see {@link #longitude()}
     * @param description   see {@link #description()}
     * @param email         see {@link #email()}
     * @param websites      see {@link #websites()}
     * @param categories    see {@link #categories()}
     * @param businessHours see {@link #businessHours()}
     * @param priceTierId   see {@link #priceTierId()}
     * @param serviceAreas  see {@link #serviceAreas()}
     */
    public IqEditBusinessProfileRequest(String address,
                   Double latitude,
                   Double longitude,
                   String description,
                   String email,
                   List<IqEditBusinessProfileWebsite> websites,
                   List<String> categories,
                   IqEditBusinessProfileBusinessHours businessHours,
                   String priceTierId,
                   List<IqEditBusinessProfileServiceArea> serviceAreas) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.email = email;
        this.websites = websites == null ? null : List.copyOf(websites);
        this.categories = categories == null ? null : List.copyOf(categories);
        this.businessHours = businessHours;
        this.priceTierId = priceTierId;
        this.serviceAreas = serviceAreas == null ? null : List.copyOf(serviceAreas);
    }

    /**
     * Returns the address line that the edit-profile mutation will stamp.
     *
     * <p>The value is absent when the caller did not patch the address field.
     *
     * @return an {@link Optional} carrying the address
     */
    public Optional<String> address() {
        return Optional.ofNullable(address);
    }

    /**
     * Returns the geo latitude that the edit-profile mutation will stamp.
     *
     * <p>The value is absent when the caller did not patch the geo coordinates.
     *
     * @return an {@link Optional} carrying the latitude
     */
    public Optional<Double> latitude() {
        return Optional.ofNullable(latitude);
    }

    /**
     * Returns the geo longitude that the edit-profile mutation will stamp.
     *
     * <p>The value is absent when the caller did not patch the geo coordinates.
     *
     * @return an {@link Optional} carrying the longitude
     */
    public Optional<Double> longitude() {
        return Optional.ofNullable(longitude);
    }

    /**
     * Returns the self-description that the edit-profile mutation will stamp.
     *
     * <p>The value is absent when the caller did not patch the description.
     *
     * @return an {@link Optional} carrying the description
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the contact email that the edit-profile mutation will stamp.
     *
     * <p>The value is absent when the caller did not patch the email.
     *
     * @return an {@link Optional} carrying the email
     */
    public Optional<String> email() {
        return Optional.ofNullable(email);
    }

    /**
     * Returns the website slot list that the edit-profile mutation will stamp.
     *
     * <p>The value is absent when the caller did not patch the websites field. An empty list clears all slots; a
     * non-empty list with one or two entries overrides the cached slots.
     *
     * @return an {@link Optional} carrying the list
     */
    public Optional<List<IqEditBusinessProfileWebsite>> websites() {
        return Optional.ofNullable(websites);
    }

    /**
     * Returns the business-category identifier list that the edit-profile mutation will stamp.
     *
     * <p>Each entry refers to a category from the business-category typeahead catalog.
     *
     * @return an {@link Optional} carrying the list
     */
    public Optional<List<String>> categories() {
        return Optional.ofNullable(categories);
    }

    /**
     * Returns the schedule payload that the edit-profile mutation will stamp.
     *
     * <p>The value is absent when the caller did not patch the schedule.
     *
     * @return an {@link Optional} carrying the payload
     */
    public Optional<IqEditBusinessProfileBusinessHours> businessHours() {
        return Optional.ofNullable(businessHours);
    }

    /**
     * Returns the price-tier identifier that the edit-profile mutation will stamp.
     *
     * <p>The value is absent when the caller did not patch the price tier.
     *
     * @return an {@link Optional} carrying the identifier
     */
    public Optional<String> priceTierId() {
        return Optional.ofNullable(priceTierId);
    }

    /**
     * Returns the service-area list that the edit-profile mutation will stamp.
     *
     * <p>The value is absent when the caller did not patch the service areas.
     *
     * @return an {@link Optional} carrying the list
     */
    public Optional<List<IqEditBusinessProfileServiceArea>> serviceAreas() {
        return Optional.ofNullable(serviceAreas);
    }

    /**
     * Returns a fresh {@link Builder} for assembling a request fluently.
     *
     * <p>The builder starts empty and every setter accepts {@code null} to leave the cached value untouched.
     *
     * @return a new {@link Builder}; never {@code null}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits a {@code <business_profile v="3" mutation_type="delta"/>} wrapper carrying one child
     * per non-{@code null} field: the website slots collapse into one or two {@code <website/>} children (an empty list
     * emits a single empty {@code <website/>} sentinel that clears all slots), and the price tier emits a
     * {@code <price_tier id symbol/>} with empty symbol because the description is not rendered through this stanza.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBusinessProfileJob",
            exports = "editBusinessProfile", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var children = new ArrayList<Stanza>();
        if (address != null) {
            children.add(new StanzaBuilder().description("address").content(address).build());
        }
        if (latitude != null) {
            children.add(new StanzaBuilder().description("latitude")
                    .content(String.valueOf(latitude.doubleValue())).build());
        }
        if (longitude != null) {
            children.add(new StanzaBuilder().description("longitude")
                    .content(String.valueOf(longitude.doubleValue())).build());
        }
        if (description != null) {
            children.add(new StanzaBuilder().description("description").content(description).build());
        }
        if (email != null) {
            children.add(new StanzaBuilder().description("email").content(email).build());
        }
        if (websites != null) {
            if (websites.isEmpty()) {
                children.add(new StanzaBuilder().description("website").build());
            } else {
                if (websites.size() >= 1) {
                    children.add(new StanzaBuilder().description("website")
                            .content(websites.get(0).url()).build());
                }
                if (websites.size() >= 2) {
                    children.add(new StanzaBuilder().description("website")
                            .content(websites.get(1).url()).build());
                }
            }
        }
        if (categories != null) {
            var categoryNodes = new ArrayList<Stanza>();
            for (var id : categories) {
                categoryNodes.add(new StanzaBuilder()
                        .description("category")
                        .attribute("id", id)
                        .build());
            }
            children.add(new StanzaBuilder()
                    .description("categories")
                    .content(categoryNodes)
                    .build());
        }
        if (businessHours != null) {
            children.add(buildBusinessHoursNode(businessHours));
        }
        if (priceTierId != null) {
            children.add(new StanzaBuilder()
                    .description("price_tier")
                    .attribute("id", priceTierId)
                    .attribute("symbol", "")
                    .content("")
                    .build());
        }
        if (serviceAreas != null) {
            var serviceAreaNodes = new ArrayList<Stanza>();
            for (var sa : serviceAreas) {
                var areaDescription = new StanzaBuilder()
                        .description("area_description")
                        .content(sa.areaDescription())
                        .build();
                var areaRadius = new StanzaBuilder()
                        .description("area_radius_meters")
                        .content(String.valueOf(sa.radius()))
                        .build();
                var lat = new StanzaBuilder()
                        .description("latitude")
                        .content(String.valueOf(sa.latitude()))
                        .build();
                var lon = new StanzaBuilder()
                        .description("longitude")
                        .content(String.valueOf(sa.longitude()))
                        .build();
                var areaCenter = new StanzaBuilder()
                        .description("area_center")
                        .content(List.of(lat, lon))
                        .build();
                serviceAreaNodes.add(new StanzaBuilder()
                        .description("service_area")
                        .content(List.of(areaDescription, areaRadius, areaCenter))
                        .build());
            }
            children.add(new StanzaBuilder()
                    .description("service_areas")
                    .content(serviceAreaNodes)
                    .build());
        }
        var businessProfileNode = new StanzaBuilder()
                .description("business_profile")
                .attribute("v", "3")
                .attribute("mutation_type", "delta")
                .content(children)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(businessProfileNode);
    }

    /**
     * Builds the {@code <business_hours/>} child from the supplied payload.
     *
     * @implNote
     * This implementation emits one {@code <business_hours_config/>} child per schedule row and drops the
     * {@code timezone} attribute when the payload does not stamp one; the optional note is emitted as the first child
     * of the envelope only when present and non-empty.
     *
     * @param hours the typed payload; never {@code null}
     * @return the built stanza; never {@code null}
     */
    private static Stanza buildBusinessHoursNode(IqEditBusinessProfileBusinessHours hours) {
        var hoursChildren = new ArrayList<Stanza>();
        if (hours.note().isPresent() && !hours.note().get().isEmpty()) {
            hoursChildren.add(new StanzaBuilder()
                    .description("business_hours_note")
                    .content(hours.note().get())
                    .build());
        }
        for (var c : hours.config()) {
            var configBuilder = new StanzaBuilder()
                    .description("business_hours_config")
                    .attribute("day_of_week", c.dayOfWeek())
                    .attribute("mode", c.mode());
            if (c.openTime().isPresent()) {
                configBuilder.attribute("open_time", String.valueOf(c.openTime().get()));
            }
            if (c.closeTime().isPresent()) {
                configBuilder.attribute("close_time", String.valueOf(c.closeTime().get()));
            }
            hoursChildren.add(configBuilder.build());
        }
        var businessHoursBuilder = new StanzaBuilder()
                .description("business_hours");
        if (hours.timezone().isPresent()) {
            businessHoursBuilder.attribute("timezone", hours.timezone().get());
        }
        return businessHoursBuilder
                .content(hoursChildren)
                .build();
    }

    /**
     * The fluent builder for {@link IqEditBusinessProfileRequest}.
     *
     * <p>The builder assembles a typed request when only a small subset of the profile fields applies; every setter
     * accepts {@code null} to leave the cached relay value untouched, and {@link #build()} produces the immutable request.
     */
    public static final class Builder {
        /**
         * The optional address staged for the next {@link #build()}.
         */
        private String address;

        /**
         * The optional latitude staged for the next {@link #build()}.
         */
        private Double latitude;

        /**
         * The optional longitude staged for the next {@link #build()}.
         */
        private Double longitude;

        /**
         * The optional description staged for the next {@link #build()}.
         */
        private String description;

        /**
         * The optional contact email staged for the next {@link #build()}.
         */
        private String email;

        /**
         * The optional website slot list staged for the next {@link #build()}.
         */
        private List<IqEditBusinessProfileWebsite> websites;

        /**
         * The optional business-category identifier list staged for the next {@link #build()}.
         */
        private List<String> categories;

        /**
         * The optional business-hours payload staged for the next {@link #build()}.
         */
        private IqEditBusinessProfileBusinessHours businessHours;

        /**
         * The optional price-tier identifier staged for the next {@link #build()}.
         */
        private String priceTierId;

        /**
         * The optional service-area list staged for the next {@link #build()}.
         */
        private List<IqEditBusinessProfileServiceArea> serviceAreas;

        /**
         * Constructs an empty builder.
         *
         * <p>This constructor is package-private; obtain a builder through {@link IqEditBusinessProfileRequest#builder()}.
         */
        Builder() {
        }

        /**
         * Stages the address field of the profile.
         *
         * <p>Pass {@code null} to leave the cached value untouched.
         *
         * @param address the address; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder address(String address) {
            this.address = address;
            return this;
        }

        /**
         * Stages the geo latitude of the profile.
         *
         * <p>Pass {@code null} to leave the cached value untouched.
         *
         * @param latitude the latitude; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        /**
         * Stages the geo longitude of the profile.
         *
         * <p>Pass {@code null} to leave the cached value untouched.
         *
         * @param longitude the longitude; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        /**
         * Stages the self-description of the profile.
         *
         * <p>Pass {@code null} to leave the cached value untouched.
         *
         * @param description the description; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Stages the contact email of the profile.
         *
         * <p>Pass {@code null} to leave the cached value untouched.
         *
         * @param email the email; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Stages the website slot list of the profile.
         *
         * <p>Pass an empty list to clear all slots, a list of one or two entries to override the slots, or {@code null}
         * to leave the cached value untouched.
         *
         * @param websites the websites; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder websites(List<IqEditBusinessProfileWebsite> websites) {
            this.websites = websites;
            return this;
        }

        /**
         * Stages the business-category identifier list of the profile.
         *
         * <p>Pass {@code null} to leave the cached value untouched.
         *
         * @param categories the categories; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder categories(List<String> categories) {
            this.categories = categories;
            return this;
        }

        /**
         * Stages the business-hours schedule of the profile.
         *
         * <p>Pass {@code null} to leave the cached value untouched.
         *
         * @param businessHours the payload; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder businessHours(IqEditBusinessProfileBusinessHours businessHours) {
            this.businessHours = businessHours;
            return this;
        }

        /**
         * Stages the price tier of the profile.
         *
         * <p>Pass {@code null} to leave the cached value untouched.
         *
         * @param priceTierId the identifier; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder priceTierId(String priceTierId) {
            this.priceTierId = priceTierId;
            return this;
        }

        /**
         * Stages the service areas of the profile.
         *
         * <p>Pass {@code null} to leave the cached value untouched.
         *
         * @param serviceAreas the areas; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder serviceAreas(List<IqEditBusinessProfileServiceArea> serviceAreas) {
            this.serviceAreas = serviceAreas;
            return this;
        }

        /**
         * Builds the immutable typed request from the staged fields.
         *
         * @return the built request; never {@code null}
         */
        public IqEditBusinessProfileRequest build() {
            return new IqEditBusinessProfileRequest(address, latitude, longitude, description, email,
                    websites, categories, businessHours, priceTierId, serviceAreas);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqEditBusinessProfileRequest) obj;
        return Objects.equals(this.address, that.address)
                && Objects.equals(this.latitude, that.latitude)
                && Objects.equals(this.longitude, that.longitude)
                && Objects.equals(this.description, that.description)
                && Objects.equals(this.email, that.email)
                && Objects.equals(this.websites, that.websites)
                && Objects.equals(this.categories, that.categories)
                && Objects.equals(this.businessHours, that.businessHours)
                && Objects.equals(this.priceTierId, that.priceTierId)
                && Objects.equals(this.serviceAreas, that.serviceAreas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, latitude, longitude, description, email,
                websites, categories, businessHours, priceTierId, serviceAreas);
    }

    @Override
    public String toString() {
        return "IqEditBusinessProfileRequest[address=" + address
                + ", description=" + description + ", email=" + email
                + ", websites=" + websites + ']';
    }
}
