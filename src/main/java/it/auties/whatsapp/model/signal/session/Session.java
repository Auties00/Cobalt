package it.auties.whatsapp.model.signal.session;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Builder
@Jacksonized
public record Session(Set<@NonNull SessionState> states) {
    public Session(Set<SessionState> states) {
        this.states = Objects.requireNonNullElseGet(states, ConcurrentHashMap::newKeySet);
    }

    public Session() {
        this(ConcurrentHashMap.newKeySet());
    }

    public Session closeCurrentState() {
        var currentState = currentState();
        currentState.ifPresent(value -> value.closed(true));
        return this;
    }

    public Optional<SessionState> currentState() {
        return states.stream()
                .filter(state -> !state.closed())
                .findFirst();
    }

    public boolean hasState(int version, byte[] baseKey) {
        return states.stream().anyMatch(state -> state.contentEquals(version, baseKey));
    }

    public Optional<SessionState> findState(int version, byte[] baseKey) {
        return states.stream().filter(state -> state.contentEquals(version, baseKey)).findFirst();
    }

    public void addState(SessionState state) {
        states.add(state);
    }
}
