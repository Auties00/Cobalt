package it.auties.whatsapp.model.business;

import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.request.Node;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * This model class represents the metadata of a business profile
 */
@AllArgsConstructor
@Value
@Accessors(fluent = true)
public class BusinessProfile {
    /**
     * The jid of the profile
     */
    @NonNull ContactJid jid;

    /**
     * The description of the business
     */
    String description;

    /**
     * The address of the business
     */
    String address;

    /**
     * The email of the business
     */
    String email;

    /**
     * The open hours of the business
     */
    BusinessHours hours;

    /**
     * The websites of the business
     */
    @NonNull List<URI> websites;

    /**
     * The category of the business
     */
    @NonNull List<BusinessCategory> categories;

    /**
     * Constructs a new profile from a node
     *
     * @param node a non-null node
     * @return a non-null profile
     */
    public static BusinessProfile of(@NonNull Node node) {
        var jid = node.attributes()
                .getJid("jid")
                .orElseThrow(() -> new NoSuchElementException("Missing jid from business profile"));
        var address = node.findNode("address").flatMap(Node::contentAsString).orElse(null);
        var description = node.findNode("description").flatMap(Node::contentAsString).orElse(null);
        var websites = node.findNodes("website")
                .stream()
                .map(Node::contentAsString)
                .flatMap(Optional::stream)
                .map(URI::create)
                .toList();
        var email = node.findNode("email").flatMap(Node::contentAsString).orElse(null);
        var categories = node.findNodes("categories")
                .stream()
                .map(entry -> entry.findNode("category"))
                .flatMap(Optional::stream)
                .map(BusinessCategory::of)
                .toList();
        var hours = createHours(node);
        return new BusinessProfile(jid, description, address, email, hours, websites, categories);
    }

    private static BusinessHours createHours(Node node) {
        var timezone = node.findNode("business_hours")
                .map(Node::attributes)
                .map(attributes -> attributes.getNullableString("timezone"))
                .orElse(null);
        if (timezone == null) {
            return null;
        }
        var entries = node.findNode("business_hours")
                .stream()
                .map(entry -> entry.findNodes("business_hours_config"))
                .flatMap(Collection::stream)
                .map(BusinessHoursEntry::of)
                .toList();
        return new BusinessHours(timezone, entries);
    }

    /**
     * Returns the description, if available
     *
     * @return an optional
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the address, if available
     *
     * @return an optional
     */
    public Optional<String> address() {
        return Optional.ofNullable(address);
    }

    /**
     * Returns the email, if available
     *
     * @return an optional
     */
    public Optional<String> email() {
        return Optional.ofNullable(email);
    }

    /**
     * Returns the business hours, if available
     *
     * @return an optional
     */
    public Optional<BusinessHours> hours() {
        return Optional.ofNullable(hours);
    }
}
