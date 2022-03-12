package it.auties.whatsapp.model.signal.session;

import lombok.NonNull;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Optional;

public record Session(LinkedHashSet<@NonNull SessionState> states) {
  public Session(){
    this(new LinkedHashSet<>());
  }

  public SessionState currentState(){
    return states.stream()
            .filter(state -> !state.closed())
            .findFirst()
            .orElse(null);
  }

  public Session closeCurrentState(){
    var currentState = currentState();
    if(currentState != null){
      currentState.closed(true);
    }

    return this;
  }

  public boolean hasState(int version, byte[] baseKey) {
    return states.stream()
            .anyMatch(state -> state.contentEquals(version, baseKey));
  }

  public Optional<SessionState> findState(int version, byte[] baseKey) {
    return states.stream()
            .filter(state -> state.contentEquals(version, baseKey))
            .findFirst();
  }

  public Session addState(SessionState state) {
    states.add(state);
    return this;
  }
}
