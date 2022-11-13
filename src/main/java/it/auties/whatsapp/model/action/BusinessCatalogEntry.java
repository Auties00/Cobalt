package it.auties.whatsapp.model.action;

import it.auties.whatsapp.model.request.Node;
import lombok.NonNull;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.NoSuchElementException;

public record BusinessCatalogEntry(@NonNull String id, @NonNull URI encryptedImage, @NonNull ReviewStatus reviewStatus,
                                   @NonNull Availability availability, @NonNull String name, @NonNull String sellerId,
                                   @NonNull URI uri, @NonNull String description, long price, @NonNull String currency,
                                   boolean hidden) {
    public static BusinessCatalogEntry of(@NonNull Node node) {
        var id = node.attributes()
                .getRequiredString("id");
        var hidden = node.attributes()
                .getBool("is_hidden");
        var name = node.findNode("name")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing name for catalog entry"));
        var encryptedImage = node.findNode("media")
                .flatMap(entry -> entry.findNode("original_image_url"))
                .flatMap(Node::contentAsString)
                .map(URI::create)
                .orElseThrow(() -> new NoSuchElementException("Missing image for catalog entry"));
        var statusInfo = node.findNode("status_info")
                .flatMap(entry -> entry.findNode("status"))
                .flatMap(Node::contentAsString)
                .map(ReviewStatus::of)
                .orElse(ReviewStatus.NO_REVIEW);
        var availability = node.findNode("availability")
                .flatMap(Node::contentAsString)
                .map(Availability::of)
                .orElse(Availability.UNKNOWN);
        var sellerId = node.findNode("retailer_id")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing seller id for catalog entry"));
        var uri = node.findNode("url")
                .flatMap(Node::contentAsString)
                .map(URI::create)
                .orElseThrow(() -> new NoSuchElementException("Missing uri for catalog entry"));
        var description = node.findNode("description")
                .flatMap(Node::contentAsString)
                .orElse("");
        var price = node.findNode("price")
                .flatMap(Node::contentAsString)
                .map(Long::parseUnsignedLong)
                .orElseThrow(() -> new NoSuchElementException("Missing price for catalog entry"));
        var currency = node.findNode("currency")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing currency for catalog entry"));
        return new BusinessCatalogEntry(id, encryptedImage, statusInfo, availability, name, sellerId, uri, description,
                price, currency, hidden);
    }

    public enum ReviewStatus {
        NO_REVIEW,
        PENDING,
        REJECTED,
        APPROVED,
        OUTDATED;

        public static ReviewStatus of(String name) {
            return valueOf(name);
        }
    }

    public enum Availability {
        UNKNOWN,
        IN_STOCK,
        OUT_OF_STOCK;

        public static Availability of(String name) {
            return Arrays.stream(values())
                    .filter(entry -> entry.name()
                            .toLowerCase(Locale.ROOT)
                            .replaceAll("_", " ")
                            .equals(name))
                    .findFirst()
                    .orElse(UNKNOWN);
        }
    }
}
