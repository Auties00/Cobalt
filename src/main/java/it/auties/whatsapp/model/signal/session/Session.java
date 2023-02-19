package it.auties.whatsapp.model.signal.session;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

@Builder
@Jacksonized
public record Session(ConcurrentLinkedDeque<@NonNull SessionState> states) {
    public Session() {
        this(new ConcurrentLinkedDeque<>());
    }

    public Session closeCurrentState() {
        var currentState = currentState();
        if (currentState != null) {
            currentState.closed(true);
        }
        return this;
    }

    public SessionState currentState() {
        return states.stream().filter(state -> !state.closed()).findFirst().orElse(null);
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
