package it.auties.whatsapp.model.business;

import it.auties.whatsapp.model.node.Node;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * A model class that represents a business category
 *
 * @param id   the non-null id
 * @param name the non-null display name
 */
public record BusinessCategory(String id, String name) {
    /**
     * Constructs a category from a node
     *
     * @param node a non-null node
     * @return a non-null category
     */
    public static BusinessCategory of(Node node) {
        var id = node.attributes().getRequiredString("id");
        var name = URLDecoder.decode(node.contentAsString().orElseThrow(), StandardCharsets.UTF_8);
        return new BusinessCategory(id, name);
    }
}
