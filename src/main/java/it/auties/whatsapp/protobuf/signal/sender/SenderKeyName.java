package it.auties.whatsapp.protobuf.signal.sender;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.whatsapp.protobuf.signal.session.SessionAddress;

import java.util.Objects;

public record SenderKeyName(String groupId, SessionAddress sender) {
  @JsonCreator
  public static SenderKeyName of(String serialized){
    var split = serialized.split("::", 2);
    return new SenderKeyName(split[0], SessionAddress.of(split[1]));
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof SenderKeyName keyName
            && Objects.equals(keyName.groupId(), groupId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId);
  }

  @JsonValue
  @Override
  public String toString() {
    return "%s::%s".formatted(groupId(), sender);
  }
}
