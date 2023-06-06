package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.whatsapp.util.Validate;

import java.util.Objects;

public record SessionAddress(String name, int id) {
    @JsonCreator
    public static SessionAddress of(String serialized) {
        var split = serialized.split(":", 2);
        Validate.isTrue(split.length == 2, "Too few parts");
        return new SessionAddress(split[0], Integer.parseInt(split[1]));
    }

    @JsonValue
    @Override
    public String toString() {
        return "%s:%s".formatted(name(), id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }
}
