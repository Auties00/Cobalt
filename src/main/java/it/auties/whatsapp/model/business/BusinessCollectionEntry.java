package it.auties.whatsapp.model.business;

import it.auties.whatsapp.model.node.Node;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Record class representing a business collection entry.
 *
 * @param id       the id of the business collection
 * @param name     the name of the business collection
 * @param products the list of products in the business collection
 */
public record BusinessCollectionEntry(String id, String name,
                                      List<BusinessCatalogEntry> products) {
    /**
     * Creates a {@code BusinessCollectionEntry} object from a {@code Node} object.
     *
     * @param node the node representing the business collection entry
     * @return the created {@code BusinessCollectionEntry} object
     * @throws NoSuchElementException if the id or name of the business collection is missing from the
     *                                node
     */
    public static BusinessCollectionEntry of(Node node) {
        var id = node.findChild("id")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing id from business collections"));
        var name = node.findChild("name")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing name from business collections"));
        var products = node.listChildren("product").stream().map(BusinessCatalogEntry::of).toList();
        return new BusinessCollectionEntry(id, name, products);
    }
}