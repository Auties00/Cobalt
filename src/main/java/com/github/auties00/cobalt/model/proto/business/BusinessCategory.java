package com.github.auties00.cobalt.model.proto.business;

import com.github.auties00.cobalt.model.node.Node;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a business category
 */
@ProtobufMessage
public final class BusinessCategory {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;
    
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String name;

    BusinessCategory(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static BusinessCategory of(Node node) {
        var id = node.getRequiredAttributeAsString("id");
        var name = URLDecoder.decode(node.toContentString().orElseThrow(), StandardCharsets.UTF_8);
        return new BusinessCategory(id, name);
    }

    public String id() {
        return id;
    }

    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BusinessCategory that
                && Objects.equals(id, that.id)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "BusinessCategory[" +
                "id=" + id +
                ", name=" + name + ']';
    }
}
