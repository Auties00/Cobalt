package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

@ProtobufMessage
public final class NewsletterState {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private String type;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public NewsletterState(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }

    public NewsletterState setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String toString() {
        return "NewsletterState{" +
                "type='" + type + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterState that && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}
