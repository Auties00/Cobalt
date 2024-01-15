package it.auties.whatsapp.model.business;

import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.node.Node;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * This model class represents the metadata of a business profile
 */
public record BusinessProfile(
        Jid jid,
        Optional<String> description,
        Optional<String> address,
        Optional<String> email,
        Optional<BusinessHours> hours,
        boolean cartEnabled,
        List<URI> websites,
        List<BusinessCategory> categories
) {
    /**
     * Constructs a new profile from a node
     *
     * @param node a non-null node
     * @return a non-null profile
     */
    public static BusinessProfile of(Node node) {
        var jid = node.attributes()
                .getRequiredJid("jid");
        var address = node.findNode("address")
                .flatMap(Node::contentAsString);
        var description = node.findNode("description")
                .flatMap(Node::contentAsString);
        var websites = node.findNodes("website")
                .stream()
                .map(Node::contentAsString)
                .flatMap(Optional::stream)
                .map(URI::create)
                .toList();
        var email = node.findNode("email")
                .flatMap(Node::contentAsString);
        var categories = node.findNodes("categories")
                .stream()
                .map(entry -> entry.findNode("category"))
                .flatMap(Optional::stream)
                .map(BusinessCategory::of)
                .toList();
        var commerceExperience = node.findNode("profile_options");
        var cartEnabled = commerceExperience.flatMap(entry -> entry.findNode("cart_enabled"))
                .flatMap(Node::contentAsBoolean)
                .orElse(commerceExperience.isEmpty());
        var hours = createHours(node);
        return new BusinessProfile(jid, description, address, email, hours, cartEnabled, websites, categories);
    }

    private static Optional<BusinessHours> createHours(Node node) {
        var timezone = node.findNode("business_hours")
                .map(Node::attributes)
                .map(attributes -> attributes.getNullableString("timezone"));
        if (timezone.isEmpty()) {
            return Optional.empty();
        }

        var entries = node.findNode("business_hours")
                .stream()
                .map(entry -> entry.findNodes("business_hours_config"))
                .flatMap(Collection::stream)
                .map(BusinessHoursEntry::of)
                .toList();
        return Optional.of(new BusinessHours(timezone.get(), entries));
    }
}
