package it.auties.whatsapp.model.business;

import it.auties.whatsapp.model.request.Node;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.NoSuchElementException;
import lombok.NonNull;

/**
 * A record class that represents a business catalog entry.
 *
 * @param id the unique identifier of the catalog entry
 * @param encryptedImage the encrypted URL of the original image of the catalog entry
 * @param reviewStatus the review status of the catalog entry
 * @param availability the availability status of the catalog entry
 * @param name the name of the catalog entry
 * @param sellerId the unique identifier of the seller of the catalog entry
 * @param uri the URI of the catalog entry
 * @param description the description of the catalog entry
 * @param price the price of the catalog entry
 * @param currency the currency of the price of the catalog entry
 * @param hidden whether the catalog entry is hidden or not
 */
public record BusinessCatalogEntry(@NonNull String id, @NonNull URI encryptedImage,
                                   @NonNull ReviewStatus reviewStatus,
                                   @NonNull Availability availability, @NonNull String name,
                                   @NonNull String sellerId,
                                   @NonNull URI uri, @NonNull String description, long price,
                                   @NonNull String currency,
                                   boolean hidden) {
  /**
   * A factory method that creates a BusinessCatalogEntry object from a given Node.
   *
   * @param node the node to get the data from
   * @return a BusinessCatalogEntry object
   * @throws NoSuchElementException if some required data is missing
   */
  public static BusinessCatalogEntry of(@NonNull Node node) {
    var id = node.attributes()
        .getRequiredString("id");
    var hidden = node.attributes()
        .getBoolean("is_hidden");
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
    return new BusinessCatalogEntry(id, encryptedImage, statusInfo, availability, name, sellerId,
        uri, description,
        price, currency, hidden);
  }

  /**
   * An enumeration of possible ReviewStatuses.
   */
  public enum ReviewStatus {
    /**
     * Indicates that no review has been performed.
     */
    NO_REVIEW,
    /**
     * Indicates that the review is pending.
     */
    PENDING,
    /**
     * Indicates that the review was rejected.
     */
    REJECTED,
    /**
     * Indicates that the review was approved.
     */
    APPROVED,
    /**
     * Indicates that the review is outdated.
     */
    OUTDATED;

    /**
     * Returns a ReviewStatus based on the given name.
     *
     * @param name the name of the ReviewStatus
     * @return a ReviewStatus
     */
    public static ReviewStatus of(String name) {
      return valueOf(name.toUpperCase(Locale.ROOT));
    }
  }

  /**
   * An enumeration of possible Availabilities.
   */
  public enum Availability {
    /**
     * Indicates an unknown availability.
     */
    UNKNOWN,
    /**
     * Indicates that the item is in stock.
     */
    IN_STOCK,
    /**
     * Indicates that the item is out of stock.
     */
    OUT_OF_STOCK;

    /**
     * Returns an Availability based on the given name.
     *
     * @param name the name of the Availability
     * @return an Availability
     */
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
