package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.binary.BinaryArray;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.swing.plaf.nimbus.State;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;

@AllArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Session {
  private static final int ARCHIVED_STATES_MAX_LENGTH = 40;

  @JsonProperty("1")
  @JsonPropertyDescription("SessionStructure")
  private SessionState state;
  
  @JsonProperty("2")
  @JsonPropertyDescription("SessionStructure")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private LinkedList<SessionState> previousStates;
  
  private boolean fresh;

  public Session() {
    this.state = new SessionState();
    this.previousStates = new LinkedList<>();
    this.fresh = true;
  }

  public Session(SessionState state) {
    this.state = state;
    this.previousStates = new LinkedList<>();
  }

  public static Session ofEncoded(byte[] serialized) {
    try {
      return ProtobufDecoder.forType(Session.class)
              .decode(serialized);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Cannot decode %s".formatted(BinaryArray.of(serialized).toBase64()), exception);
    }
  }

  public boolean hasState(int version, byte[] baseKey) {
    return (state != null && state.contentEquals(version, baseKey))
            || previousStates.stream().anyMatch(state -> state != null && state.contentEquals(version, baseKey));
  }

  public Optional<SessionState> findState(int version, byte[] baseKey) {
    return Optional.ofNullable(state)
            .filter(state -> state.contentEquals(version, baseKey))
            .or(() -> findStateFallback(version, baseKey));
  }

  private Optional<SessionState> findStateFallback(int version, byte[] baseKey) {
    return previousStates.stream()
            .filter(state -> state != null && state.contentEquals(version, baseKey))
            .findFirst();
  }

  public Session promoteState(SessionState promotedState) {
    if(state != null) {
      this.previousStates.addFirst(state);
    }

    this.state = promotedState;
    if (previousStates.size() > ARCHIVED_STATES_MAX_LENGTH) {
      previousStates.removeLast();
    }

    return this;
  }

  public byte[] serialize() {
    return ProtobufEncoder.encode(this);
  }
}
