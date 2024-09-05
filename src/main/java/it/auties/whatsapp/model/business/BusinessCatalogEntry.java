package it.auties.whatsapp.model.business;

import it.auties.whatsapp.model.node.Node;

import java.net.URI;
import java.util.NoSuchElementException;

/**
 * A record class that represents a business catalog entry.
 *
 * @param id             the unique identifier of the catalog entry
 * @param encryptedImage the encrypted URL of the original image of the catalog entry
 * @param reviewStatus   the review status of the catalog entry
 * @param availability   the availability status of the catalog entry
 * @param name           the name of the catalog entry
 * @param sellerId       the unique identifier of the seller of the catalog entry
 * @param uri            the URI of the catalog entry
 * @param description    the description of the catalog entry
 * @param price          the price of the catalog entry
 * @param currency       the currency of the price of the catalog entry
 * @param hidden         whether the catalog entry is hidden or not
 */
public record BusinessCatalogEntry(String id, URI encryptedImage, BusinessReviewStatus reviewStatus,
                                   BusinessItemAvailability availability, String name, String sellerId,
                                   URI uri, String description, long price, String currency,
                                   boolean hidden) {
    /**
     * A factory method that creates a BusinessCatalogEntry object from a given Node.
     *
     * @param node the node to get the data from
     * @return a BusinessCatalogEntry object
     * @throws NoSuchElementException if some required data is missing
     */
    public static BusinessCatalogEntry of(Node node) {
        var id = node.attributes().getRequiredString("id");
        var hidden = node.attributes().getBoolean("is_hidden");
        var name = node.findChild("name")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing name for catalog entry"));
        var encryptedImage = node.findChild("media")
                .flatMap(entry -> entry.findChild("original_image_url"))
                .flatMap(Node::contentAsString)
                .map(URI::create)
                .orElseThrow(() -> new NoSuchElementException("Missing image for catalog entry"));
        var statusInfo = node.findChild("status_info")
                .flatMap(entry -> entry.findChild("status"))
                .flatMap(Node::contentAsString)
                .map(BusinessReviewStatus::of)
                .orElse(BusinessReviewStatus.NO_REVIEW);
        var availability = node.findChild("availability")
                .flatMap(Node::contentAsString)
                .map(BusinessItemAvailability::of)
                .orElse(BusinessItemAvailability.UNKNOWN);
        var sellerId = node.findChild("retailer_id")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing seller id for catalog entry"));
        var uri = node.findChild("url")
                .flatMap(Node::contentAsString)
                .map(URI::create)
                .orElseThrow(() -> new NoSuchElementException("Missing uri for catalog entry"));
        var description = node.findChild("description").flatMap(Node::contentAsString).orElse("");
        var price = node.findChild("price")
                .flatMap(Node::contentAsString)
                .map(Long::parseUnsignedLong)
                .orElseThrow(() -> new NoSuchElementException("Missing price for catalog entry"));
        var currency = node.findChild("currency")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing currency for catalog entry"));
        return new BusinessCatalogEntry(id, encryptedImage, statusInfo, availability, name, sellerId, uri, description, price, currency, hidden);
    }
}
