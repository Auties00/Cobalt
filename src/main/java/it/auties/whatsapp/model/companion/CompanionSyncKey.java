package it.auties.whatsapp.model.companion;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.sync.AppStateSyncKey;

import java.util.*;

@ProtobufMessage
public final class CompanionSyncKey {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid companion;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final LinkedList<AppStateSyncKey> keys;

    CompanionSyncKey(Jid companion, LinkedList<AppStateSyncKey> keys) {
        this.companion = companion;
        this.keys = Objects.requireNonNullElseGet(keys, LinkedList::new);
    }

    public Jid companion() {
        return companion;
    }

    public SequencedCollection<AppStateSyncKey> keys() {
        return Collections.unmodifiableSequencedCollection(keys);
    }

    public void addKeys(Collection<AppStateSyncKey> keys) {
        if (keys != null) {
            this.keys.addAll(keys);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CompanionSyncKey that && Objects.equals(companion, that.companion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companion);
    }

    @Override
    public String toString() {
        return "CompanionSyncKey[" + "companion=" + companion + ", " + "keys=" + keys + ']';
    }
}
