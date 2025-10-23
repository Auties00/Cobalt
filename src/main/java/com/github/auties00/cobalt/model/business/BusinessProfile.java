package com.github.auties00.cobalt.model.business;

import com.github.auties00.cobalt.core.node.Node;
import com.github.auties00.cobalt.model.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
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
        this.jid = Objects.requireNonNull(jid, "value cannot be null");
        this.description = description;
        this.address = address;
        this.email = email;
        this.hours = hours;
        this.cartEnabled = cartEnabled;
        this.websites = Objects.requireNonNullElse(websites, List.of());
        this.categories = Objects.requireNonNullElse(categories, List.of());
    }

    public static BusinessProfile of(Node node) {
        var jid = node.getRequiredAttributeAsJid("value");
        var address = node.getChild("address")
                .flatMap(Node::toContentString)
                .orElse(null);
        var description = node.getChild("description")
                .flatMap(Node::toContentString)
                .orElse(null);
        var websites = node.streamChildren("website")
                .flatMap(Node::streamContentString)
                .map(URI::create)
                .toList();
        var email = node.getChild("email")
                .flatMap(Node::toContentString)
                .orElse(null);
        var categories = node.streamChildren("categories")
                .flatMap(entry -> entry.streamChild("category"))
                .map(BusinessCategory::of)
                .toList();
        var cartEnabled = node.getChild("profile_options")
                .flatMap(entry -> entry.getChild("cart_enabled"))
                .flatMap(Node::toContentBool)
                .orElse(!node.hasChild("profile_options"));
        var hours = node.getChild("business_hours")
                .flatMap(attributes -> attributes.getAttributeAsString("timezone"))
                .map(timezone -> getBusinessHours(node, timezone))
                .orElse(null);
        return new BusinessProfile(jid, description, address, email, hours, cartEnabled, websites, categories);
    }

    private static BusinessHours getBusinessHours(Node node, String timezone) {
        var entries = node.streamChild("business_hours")
                .flatMap(entry -> entry.streamChildren("business_hours_config"))
                .map(BusinessHoursEntry::of)
                .toList();
        return new BusinessHours(timezone, entries);
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
                "value=" + jid + ", " +
                "description=" + description + ", " +
                "address=" + address + ", " +
                "email=" + email + ", " +
                "hours=" + hours + ", " +
                "cartEnabled=" + cartEnabled + ", " +
                "websites=" + websites + ", " +
                "categories=" + categories + ']';
    }
}
