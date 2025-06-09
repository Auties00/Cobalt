package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.node.Node;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This model class represents the metadata of a business profile
 */
@ProtobufMessage
public final class BusinessProfile {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid jid;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String description;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String address;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String email;

    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final BusinessHours hours;

    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    final boolean cartEnabled;

    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final List<URI> websites;

    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    final List<BusinessCategory> categories;

    BusinessProfile(Jid jid, String description, String address, String email, BusinessHours hours, boolean cartEnabled, List<URI> websites, List<BusinessCategory> categories) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.description = description;
        this.address = address;
        this.email = email;
        this.hours = hours;
        this.cartEnabled = cartEnabled;
        this.websites = Objects.requireNonNullElse(websites, List.of());
        this.categories = Objects.requireNonNullElse(categories, List.of());
    }

    public static BusinessProfile of(Node node) {
        var jid = node.attributes()
                .getRequiredJid("jid");
        var address = node.findChild("address")
                .flatMap(Node::contentAsString)
                .orElse(null);
        var description = node.findChild("description")
                .flatMap(Node::contentAsString)
                .orElse(null);
        var websites = node.listChildren("website")
                .stream()
                .map(Node::contentAsString)
                .flatMap(Optional::stream)
                .map(URI::create)
                .toList();
        var email = node.findChild("email")
                .flatMap(Node::contentAsString)
                .orElse(null);
        var categories = node.listChildren("categories")
                .stream()
                .map(entry -> entry.findChild("category"))
                .flatMap(Optional::stream)
                .map(BusinessCategory::of)
                .toList();
        var commerceExperience = node.findChild("profile_options");
        var cartEnabled = commerceExperience.flatMap(entry -> entry.findChild("cart_enabled"))
                .flatMap(Node::contentAsBoolean)
                .orElse(commerceExperience.isEmpty());
        var hours = node.findChild("business_hours")
                .map(Node::attributes)
                .map(attributes -> attributes.getNullableString("timezone"))
                .map(timezone -> {
                    var entries = node.findChild("business_hours")
                            .stream()
                            .map(entry -> entry.listChildren("business_hours_config"))
                            .flatMap(Collection::stream)
                            .map(BusinessHoursEntry::of)
                            .toList();
                    return new BusinessHours(timezone, entries);
                })
                .orElse(null);
        return new BusinessProfile(jid, description, address, email, hours, cartEnabled, websites, categories);
    }

    public Jid jid() {
        return jid;
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public Optional<String> address() {
        return Optional.ofNullable(address);
    }

    public Optional<String> email() {
        return Optional.ofNullable(email);
    }

    public Optional<BusinessHours> hours() {
        return Optional.ofNullable(hours);
    }

    public boolean cartEnabled() {
        return cartEnabled;
    }

    public List<URI> websites() {
        return websites;
    }

    public List<BusinessCategory> categories() {
        return categories;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BusinessProfile that
                && cartEnabled == that.cartEnabled
                && Objects.equals(jid, that.jid)
                && Objects.equals(description, that.description)
                && Objects.equals(address, that.address)
                && Objects.equals(email, that.email)
                && Objects.equals(hours, that.hours)
                && Objects.equals(websites, that.websites)
                && Objects.equals(categories, that.categories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, description, address, email, hours, cartEnabled, websites, categories);
    }

    @Override
    public String toString() {
        return "BusinessProfile[" +
                "jid=" + jid + ", " +
                "description=" + description + ", " +
                "address=" + address + ", " +
                "email=" + email + ", " +
                "hours=" + hours + ", " +
                "cartEnabled=" + cartEnabled + ", " +
                "websites=" + websites + ", " +
                "categories=" + categories + ']';
    }
}
