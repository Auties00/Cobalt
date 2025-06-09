package it.auties.whatsapp.model.newsletter;

import io.avaje.jsonb.Json;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

@ProtobufMessage
@Json
public final class NewsletterState {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    @Json.Property("type")
    String type;

    public NewsletterState(String type) {
        this.type = type;
    }

    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    public NewsletterState setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterState that
                && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public String toString() {
        return "NewsletterState{" +
                "type='" + type + '\'' +
                '}';
    }
}
