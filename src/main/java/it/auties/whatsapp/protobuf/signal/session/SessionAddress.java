package it.auties.whatsapp.protobuf.signal.session;

import java.util.Objects;

public record SessionAddress(String name, int deviceId) {
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
