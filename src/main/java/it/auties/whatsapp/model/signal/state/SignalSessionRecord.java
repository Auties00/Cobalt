package it.auties.whatsapp.model.signal.state;


import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.SequencedCollection;

@ProtobufMessage
public final class SignalSessionRecord {
    private static final int ARCHIVED_STATES_MAX_LENGTH = 40;

    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    SignalSessionState sessionState;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final SequencedCollection<SignalSessionState> previousStates;

    private final boolean fresh;

    SignalSessionRecord(SignalSessionState sessionState, SequencedCollection<SignalSessionState> previousStates) {
        this.sessionState = sessionState;
        this.previousStates = previousStates;
        this.fresh = false;
    }

    public SignalSessionRecord() {
        this.sessionState = new SignalSessionState();
        this.previousStates = new LinkedList<>();
        this.fresh = true;
    }

    public SignalSessionRecord(SignalSessionState sessionState) {
        this.sessionState = sessionState;
        this.previousStates = new LinkedList<>();
        this.fresh = false;
    }

    public boolean hasSessionState(int version, byte[] aliceBaseKey) {
        if (sessionState.sessionVersion() == version && Arrays.equals(aliceBaseKey, sessionState.baseKey())) {
            return true;
        }

        return previousStates.stream()
                .anyMatch(state -> state.sessionVersion() == version
                        && Arrays.equals(aliceBaseKey, state.baseKey()));
    }

    public SignalSessionState sessionState() {
        return sessionState;
    }

    public SequencedCollection<SignalSessionState> previousSessionStates() {
        return Collections.unmodifiableSequencedCollection(previousStates);
    }

    public void removePreviousSessionStates() {
        previousStates.clear();
    }

    public boolean isFresh() {
        return fresh;
    }

    public void archiveCurrentState() {
        promoteState(new SignalSessionState());
    }

    public void promoteState(SignalSessionState promotedState) {
        this.previousStates.addFirst(sessionState);
        this.sessionState = promotedState;

        if (previousStates.size() > ARCHIVED_STATES_MAX_LENGTH) {
            previousStates.removeLast();
        }
    }

    public void setState(SignalSessionState sessionState) {
        this.sessionState = sessionState;
    }
}
