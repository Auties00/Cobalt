package it.auties.whatsapp.model.signal.sender;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@ProtobufMessage
public final class SenderPreKeys {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final Collection<Jid> preKeys;

    public SenderPreKeys() {
        this.preKeys = new ArrayList<>();
    }

    SenderPreKeys(Collection<Jid> preKeys) {
        this.preKeys = preKeys;
    }

    public Collection<Jid> preKeys() {
        return Collections.unmodifiableCollection(preKeys);
    }

    public void addPreKey(Jid jid) {
        preKeys.add(jid);
    }

    public void addPreKeys(Collection<Jid> recipients) {
        preKeys.addAll(recipients);
    }

    public boolean contains(Jid recipient) {
        return preKeys.contains(recipient);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SenderPreKeys) obj;
        return Objects.equals(this.preKeys, that.preKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(preKeys);
    }
}
