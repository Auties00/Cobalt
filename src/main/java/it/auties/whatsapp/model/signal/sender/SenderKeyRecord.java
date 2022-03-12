package it.auties.whatsapp.model.signal.sender;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class SenderKeyRecord {
  private static final int MAX_STATES = 5;

  @JsonProperty("1")
  @JsonPropertyDescription("SenderKeyStateStructure")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  @Default
  private LinkedList<SenderKeyState> states = new LinkedList<>();

  public SenderKeyState currentState() {
    return states.getFirst();
  }

  public SenderKeyState findStateById(int keyId) {
    return states().stream()
            .filter(key -> key.id() == keyId)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Cannot find state with id %s".formatted(keyId)));
  }

  public void addState(int id, int iteration, byte[] chainKey, byte[] signatureKey){
    addState(id, iteration, chainKey, signatureKey, null);
  }

  public void addState(int id, int iteration, byte[] chainKey, byte[] signaturePublic, byte[] signaturePrivate){
    states.removeIf(item -> item.id() == id);
    var state = new SenderKeyState(id, iteration, chainKey, signaturePublic, signaturePrivate);
    states.add(state);
  }

  public boolean isEmpty() {
    return states.isEmpty();
  }

  public boolean equals(Object object){
    return object instanceof SenderKeyRecord that
            && Objects.equals(this.states(), that.states());
  }
}
