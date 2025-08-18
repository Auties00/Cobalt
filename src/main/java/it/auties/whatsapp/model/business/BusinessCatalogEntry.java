package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.node.Node;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A model class that represents a business catalog entry.
 */
@ProtobufMessage
public final class BusinessCatalogEntry {
    private static final Map<String, BusinessItemAvailability> PRETTY_NAME_TO_AVAILABILITY = Arrays.stream(BusinessItemAvailability.values())
            .collect(Collectors.toMap(entry -> entry.name().toLowerCase().replaceAll("_", " "), Function.identity()));
     private static final Map<String, BusinessReviewStatus> PRETTY_NAME_TO_REVIEW_STATUS = Arrays.stream(BusinessReviewStatus.values())
            .collect(Collectors.toMap(entry -> entry.name().toLowerCase(), Function.identity()));

    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final URI encryptedImage;

    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    final BusinessReviewStatus reviewStatus;

    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    final BusinessItemAvailability availability;

    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String name;

    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String sellerId;

    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final URI uri;

    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String description;

    @ProtobufProperty(index = 9, type = ProtobufType.INT64)
    final long price;

    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    final String currency;

    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    final boolean hidden;

    BusinessCatalogEntry(String id, URI encryptedImage, BusinessReviewStatus reviewStatus, BusinessItemAvailability availability, String name, String sellerId, URI uri, String description, long price, String currency, boolean hidden) {
        this.id = Objects.requireNonNull(id, "id cannot be null in");
        this.encryptedImage = Objects.requireNonNull(encryptedImage, "encryptedImage cannot be null");
        this.reviewStatus = Objects.requireNonNull(reviewStatus, "reviewStatus cannot be null");
        this.availability = Objects.requireNonNull(availability, "availability cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.sellerId = Objects.requireNonNull(sellerId, "sellerId cannot be null");
        this.uri = Objects.requireNonNull(uri, "uri cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.price = price;
        this.currency = Objects.requireNonNull(currency, "currency cannot be null");
        this.hidden = hidden;
    }

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
                .map(prettyName -> PRETTY_NAME_TO_REVIEW_STATUS.get(prettyName.toLowerCase()))
                .orElse(BusinessReviewStatus.NO_REVIEW);
        var availability = node.findChild("availability")
                .flatMap(Node::contentAsString)
                .map(prettyName -> PRETTY_NAME_TO_AVAILABILITY.get(prettyName.toLowerCase()))
                .orElse(BusinessItemAvailability.UNKNOWN);
        var sellerId = node.findChild("retailer_id")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing seller id for catalog entry"));
        var uri = node.findChild("url")
                .flatMap(Node::contentAsString)
                .map(URI::create)
                .orElseThrow(() -> new NoSuchElementException("Missing uri for catalog entry"));
        var description = node.findChild("description")
                .flatMap(Node::contentAsString)
                .orElse("");
        var price = node.findChild("price")
                .flatMap(Node::contentAsString)
                .map(Long::parseUnsignedLong)
                .orElseThrow(() -> new NoSuchElementException("Missing price for catalog entry"));
        var currency = node.findChild("currency")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing currency for catalog entry"));
        return new BusinessCatalogEntry(id, encryptedImage, statusInfo, availability, name, sellerId, uri, description, price, currency, hidden);
    }

    public String id() {
        return id;
    }

    public URI encryptedImage() {
        return encryptedImage;
    }

    public BusinessReviewStatus reviewStatus() {
        return reviewStatus;
    }

    public BusinessItemAvailability availability() {
        return availability;
    }

    public String name() {
        return name;
    }

    public String sellerId() {
        return sellerId;
    }

    public URI uri() {
        return uri;
    }

    public String description() {
        return description;
    }

    public long price() {
        return price;
    }

    public String currency() {
        return currency;
    }

    public boolean hidden() {
        return hidden;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BusinessCatalogEntry that
                && price == that.price
                && hidden == that.hidden
                && Objects.equals(id, that.id)
                && Objects.equals(encryptedImage, that.encryptedImage)
                && reviewStatus == that.reviewStatus
                && availability == that.availability
                && Objects.equals(name, that.name)
                && Objects.equals(sellerId, that.sellerId)
                && Objects.equals(uri, that.uri)
                && Objects.equals(description, that.description)
                && Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, encryptedImage, reviewStatus, availability, name, sellerId, uri, description, price, currency, hidden);
    }

    @Override
    public String toString() {
        return "BusinessCatalogEntry[" +
                "id='" + id + '\'' +
                ", encryptedImage=" + encryptedImage +
                ", reviewStatus=" + reviewStatus +
                ", availability=" + availability +
                ", name='" + name + '\'' +
                ", sellerId='" + sellerId + '\'' +
                ", uri=" + uri +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", currency='" + currency + '\'' +
                ", hidden=" + hidden +
                ']';
    }
}
