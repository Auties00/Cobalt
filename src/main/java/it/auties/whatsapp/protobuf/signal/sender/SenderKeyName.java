package it.auties.whatsapp.protobuf.signal.sender;

import it.auties.whatsapp.protobuf.signal.session.SessionAddress;

import java.util.Objects;

public record SenderKeyName(String groupId, SessionAddress sender) {
  @Override
  public boolean equals(Object other) {
    return other instanceof SenderKeyName keyName
            && Objects.equals(keyName.groupId(), groupId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId);
  }

  @Override
  public String toString() {
    return "%s::%s::%s".formatted(groupId(), sender.name(), sender.deviceId());
  }
}
