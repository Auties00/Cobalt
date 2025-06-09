package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.node.Node;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Record class representing a business collection entry.
 */
@ProtobufMessage
public final class BusinessCollectionEntry {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String name;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final List<BusinessCatalogEntry> products;

    BusinessCollectionEntry(String id, String name, List<BusinessCatalogEntry> products) {
        this.id = id;
        this.name = name;
        this.products = Objects.requireNonNullElse(products, List.of());
    }

    public static BusinessCollectionEntry of(Node node) {
        var id = node.findChild("id")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing id from business collections"));
        var name = node.findChild("name")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing name from business collections"));
        var products = node.listChildren("product")
                .stream()
                .map(BusinessCatalogEntry::of)
                .toList();
        return new BusinessCollectionEntry(id, name, products);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public List<BusinessCatalogEntry> products() {
        return products;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCollectionEntry) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.products, that.products);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, products);
    }

    @Override
    public String toString() {
        return "BusinessCollectionEntry[" +
                "id=" + id + ", " +
                "name=" + name + ", " +
                "products=" + products + ']';
    }

}