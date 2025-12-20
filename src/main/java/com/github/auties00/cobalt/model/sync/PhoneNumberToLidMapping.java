package com.github.auties00.cobalt.model.sync;

import com.github.auties00.cobalt.model.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Represents a simple phone number to LID mapping.
 * <p>
 * This record stores JID strings directly, providing a simpler
 * mapping structure compared to {@link LIDMigrationMapping}.
 */
@ProtobufMessage(name = "PhoneNumberToLIDMapping")
public final class PhoneNumberToLidMapping {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid pnJid;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final Jid lidJid;

    PhoneNumberToLidMapping(Jid pnJid, Jid lidJid) {
        this.pnJid = pnJid;
        this.lidJid = lidJid;
    }

    public Optional<Jid> pnJid() {
        return Optional.ofNullable(pnJid);
    }

    public Optional<Jid> lidJid() {
        return Optional.ofNullable(lidJid);
    }
}
