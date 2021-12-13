package it.auties.whatsapp.protobuf.signal.sender;

import it.auties.whatsapp.protobuf.signal.session.ProtocolAddress;

import java.util.Objects;

public record SenderKeyName(String groupId, ProtocolAddress sender) {
  public String serialize() {
    return "%s::%s::%s".formatted(groupId(), sender.name(), sender.deviceId());
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof SenderKeyName keyName
            && Objects.equals(keyName.groupId(), groupId())
            && Objects.equals(keyName.sender(), sender());
  }
}
