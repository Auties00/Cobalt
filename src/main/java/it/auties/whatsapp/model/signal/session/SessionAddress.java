package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record SessionAddress(String name, int id) {
    @JsonCreator
    public static SessionAddress of(String serialized){
        var split = serialized.split("\\.", 2);
        return new SessionAddress(split[0], Integer.parseInt(split[1]));
    }

    @JsonValue
    @Override
    public String toString() {
        return "%s.%s".formatted(name(), id());
    }
}
