package it.auties.whatsapp.model.signal.session;


import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ProtobufMessage
public final class Session {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    private final ConcurrentHashMap.KeySetView<SessionState, Boolean> states;

    public Session() {
        this(ConcurrentHashMap.newKeySet());
    }

    public Session(ConcurrentHashMap.KeySetView<SessionState, Boolean> states) {
        Objects.requireNonNull(states);
        this.states = states;
    }

    public Session closeCurrentState() {
        var currentState = currentState();
        currentState.ifPresent(value -> value.closed(true));
        return this;
    }

    public Collection<SessionState> states() {
        return Collections.unmodifiableCollection(states);
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Session) obj;
        return Objects.equals(this.states, that.states);
    }

    @Override
    public int hashCode() {
        return Objects.hash(states);
    }

    @Override
    public String toString() {
        return "Session[" +
                "states=" + states + ']';
    }
}
