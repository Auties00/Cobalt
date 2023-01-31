package it.auties.whatsapp.model.business;

import it.auties.whatsapp.model.request.Node;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.NonNull;

/**
 * Record class representing a business collection entry.
 *
 * @param id       the id of the business collection
 * @param name     the name of the business collection
 * @param products the list of products in the business collection
 */
public record BusinessCollectionEntry(@NonNull String id, @NonNull String name,
                                      @NonNull List<BusinessCatalogEntry> products) {
  /**
   * Creates a {@code BusinessCollectionEntry} object from a {@code Node} object.
   *
   * @param node the node representing the business collection entry
   * @return the created {@code BusinessCollectionEntry} object
   * @throws NoSuchElementException if the id or name of the business collection is missing from the
   *                                node
   */
  public static BusinessCollectionEntry of(@NonNull Node node) {
    var id = node.findNode("id")
        .flatMap(Node::contentAsString)
        .orElseThrow(() -> new NoSuchElementException("Missing id from business collections"));
    var name = node.findNode("name")
        .flatMap(Node::contentAsString)
        .orElseThrow(() -> new NoSuchElementException("Missing name from business collections"));
    var products = node.findNodes("product")
        .stream()
        .map(BusinessCatalogEntry::of)
        .toList();
    return new BusinessCollectionEntry(id, name, products);
  }
}