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

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

@AllArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SessionRecord {
  private static final int ARCHIVED_STATES_MAX_LENGTH = 40;

  @JsonProperty("1")
  @JsonPropertyDescription("SessionStructure")
  private SessionStructure currentSession;
  
  @JsonProperty("2")
  @JsonPropertyDescription("SessionStructure")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private LinkedList<SessionStructure> previousSessions;
  
  private boolean fresh;

  public SessionRecord() {
    this.currentSession = new SessionStructure();
    this.previousSessions = new LinkedList<>();
    this.fresh = true;
  }

  public SessionRecord(SessionStructure state) {
    this.currentSession = state;
    this.previousSessions = new LinkedList<>();
  }

  public static SessionRecord ofEncoded(byte[] serialized) {
    try {
      return ProtobufDecoder.forType(SessionRecord.class)
              .decode(serialized);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Cannot decode %s".formatted(BinaryArray.of(serialized).toBase64()), exception);
    }
  }

  public boolean hasSessionState(int version, byte[] aliceBaseKey) {
    return aliceBaseKey != null
            && currentSession.version() == version
            && (Arrays.equals(aliceBaseKey, currentSession.aliceBaseKey()) || previousSessions.stream().anyMatch(state -> state.version() == version && Arrays.equals(aliceBaseKey, state.aliceBaseKey())));
  }

  public void removePreviousSessionStates() {
    previousSessions.clear();
  }

  public void archiveCurrentState() {
    promoteState(new SessionStructure());
  }

  public void promoteState(SessionStructure promotedState) {
    this.previousSessions.addFirst(currentSession);
    this.currentSession = promotedState;
    if (previousSessions.size() <= ARCHIVED_STATES_MAX_LENGTH) {
      return;
    }

    previousSessions.removeLast();
  }

  public byte[] serialize() {
    return ProtobufEncoder.encode(this);
  }
}
