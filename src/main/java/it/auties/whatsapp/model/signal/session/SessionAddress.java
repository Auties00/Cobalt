package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public record SessionAddress(String name, int deviceId) {
    @JsonCreator
    public static SessionAddress of(String serialized){
        var split = serialized.split("\\.", 2);
        return new SessionAddress(split[0], Integer.parseInt(split[1]));
    }

    @JsonValue
    @Override
    public String toString() {
        return "%s.%s".formatted(name, deviceId);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SessionAddress address
                && Objects.equals(address.name(), name())
                && Objects.equals(address.deviceId(), deviceId());
    }
}
