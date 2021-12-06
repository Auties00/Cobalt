package it.auties.whatsapp.protobuf.group;

import java.util.Objects;

public record ProtocolAddress(String name, int deviceId) {
    @Override
    public boolean equals(Object other) {
        return other instanceof ProtocolAddress address
                && Objects.equals(address.name(), name())
                && Objects.equals(address.deviceId(), deviceId());
    }
}
