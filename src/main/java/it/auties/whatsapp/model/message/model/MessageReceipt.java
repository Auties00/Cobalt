package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.*;

/**
 * A model that represents the receipt for a message
 */
@ProtobufMessage(name = "UserReceipt")
public final class MessageReceipt {
    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    long deliveredTimestampSeconds;

    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    long readTimestampSeconds;

    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    long playedTimestampSeconds;

    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final Set<Jid> deliveredJids;

    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final Set<Jid> readJids;

    public MessageReceipt() {
        this.deliveredJids = new HashSet<>();
        this.readJids = new HashSet<>();
    }

    MessageReceipt(long deliveredTimestampSeconds, long readTimestampSeconds, long playedTimestampSeconds, Set<Jid> deliveredJids, Set<Jid> readJids) {
        this.deliveredTimestampSeconds = deliveredTimestampSeconds;
        this.readTimestampSeconds = readTimestampSeconds;
        this.playedTimestampSeconds = playedTimestampSeconds;
        this.deliveredJids = Objects.requireNonNullElseGet(deliveredJids, HashSet::new) ;
        this.readJids = Objects.requireNonNullElseGet(readJids, HashSet::new);
    }

    public long deliveredTimestampSeconds() {
        return deliveredTimestampSeconds;
    }

    public Optional<ZonedDateTime> deliveredTimestamp() {
        return Clock.parseSeconds(deliveredTimestampSeconds);
    }

    public long readTimestampSeconds() {
        return readTimestampSeconds;
    }

    public Optional<ZonedDateTime> readTimestamp() {
        return Clock.parseSeconds(readTimestampSeconds);
    }

    public long playedTimestampSeconds() {
        return playedTimestampSeconds;
    }

    public Optional<ZonedDateTime> playedTimestamp() {
        return Clock.parseSeconds(playedTimestampSeconds);
    }

    public Set<Jid> deliveredJids() {
        return Collections.unmodifiableSet(deliveredJids);
    }

    public void addDeliveredJid(Jid jid) {
        deliveredJids.add(jid);
    }

    public boolean removeDeliveredJid(Jid jid) {
        return deliveredJids.remove(jid);
    }

    public Set<Jid> readJids() {
        return Collections.unmodifiableSet(readJids);
    }

    public void addReadJid(Jid jid) {
        readJids.add(jid);
    }

    public boolean removeReadJid(Jid jid) {
        return readJids.remove(jid);
    }

    public void setReadTimestampSeconds(long readTimestampSeconds) {
        if (deliveredTimestampSeconds == 0) {
            this.deliveredTimestampSeconds = readTimestampSeconds;
        }
        this.readTimestampSeconds = readTimestampSeconds;
    }

    public void setPlayedTimestampSeconds(long playedTimestampSeconds) {
        if (deliveredTimestampSeconds == 0) {
            this.deliveredTimestampSeconds = playedTimestampSeconds;
        }
        if (readTimestampSeconds == 0) {
            this.readTimestampSeconds = playedTimestampSeconds;
        }
        this.playedTimestampSeconds = playedTimestampSeconds;
    }
}