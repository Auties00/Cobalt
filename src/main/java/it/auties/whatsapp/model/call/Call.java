package it.auties.whatsapp.model.call;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

@ProtobufMessage
public final class Call {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid chatJid;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final Jid callerJid;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String id;

    @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
    final long timestampSeconds;

    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    final boolean video;

    @ProtobufProperty(index = 6, type = ProtobufType.ENUM)
    final CallStatus status;

    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    final boolean offline;

    Call(Jid chatJid, Jid callerJid, String id, long timestampSeconds, boolean video, CallStatus status, boolean offline) {
        this.chatJid = Objects.requireNonNull(chatJid, "chatJid cannot be null");
        this.callerJid = Objects.requireNonNull(callerJid, "callerJid cannot be null");
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.timestampSeconds = timestampSeconds;
        this.video = video;
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.offline = offline;
    }

    public Jid chatJid() {
        return chatJid;
    }

    public Jid callerJid() {
        return callerJid;
    }

    public String id() {
        return id;
    }

    public long timestampSeconds() {
        return timestampSeconds;
    }

    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }

    public boolean video() {
        return video;
    }

    public CallStatus status() {
        return status;
    }

    public boolean offline() {
        return offline;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Call call
                && timestampSeconds == call.timestampSeconds
                && video == call.video
                && offline == call.offline
                && Objects.equals(chatJid, call.chatJid)
                && Objects.equals(callerJid, call.callerJid)
                && Objects.equals(id, call.id)
                && status == call.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatJid, callerJid, id, timestampSeconds, video, status, offline);
    }

    @Override
    public String toString() {
        return "Call[" +
                "chat=" + chatJid + ", " +
                "caller=" + callerJid + ", " +
                "id=" + id + ", " +
                "timestampSeconds=" + timestampSeconds + ", " +
                "video=" + video + ", " +
                "status=" + status + ", " +
                "offline=" + offline + ']';
    }
}