package it.auties.whatsapp.model.business;

import it.auties.whatsapp.model.request.Node;
import lombok.NonNull;

import java.util.List;
import java.util.NoSuchElementException;

public record BusinessCollectionEntry(@NonNull String id, @NonNull String name,
                                      @NonNull List<BusinessCatalogEntry> products) {
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
