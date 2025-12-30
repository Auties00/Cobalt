package com.github.auties00.cobalt.model.message.model;

import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.util.Clock;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents an ephemeral message that was saved manually by the user in a chat
 */
@ProtobufMessage(name = "KeepInChat")
public final class KeepInChat {
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final Type keepType;

    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    final long serverTimestampSeconds;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final ChatMessageKey key;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final Jid deviceJid;

    @ProtobufProperty(index = 5, type = ProtobufType.INT64)
    final long clientTimestampInMilliseconds;

    @ProtobufProperty(index = 6, type = ProtobufType.INT64)
    final long serverTimestampMilliseconds;

    KeepInChat(Type keepType, long serverTimestampSeconds, ChatMessageKey key, Jid deviceJid, long clientTimestampInMilliseconds, long serverTimestampMilliseconds) {
        this.keepType = keepType;
        this.serverTimestampSeconds = serverTimestampSeconds;
        this.key = key;
        this.deviceJid = deviceJid;
        this.clientTimestampInMilliseconds = clientTimestampInMilliseconds;
        this.serverTimestampMilliseconds = serverTimestampMilliseconds;
    }

    public Optional<Type> keepType() {
        return Optional.ofNullable(keepType);
    }

    public long serverTimestampSeconds() {
        return serverTimestampSeconds;
    }

    public Optional<ChatMessageKey> key() {
        return Optional.ofNullable(key);
    }

    public Optional<Jid> deviceJid() {
        return Optional.ofNullable(deviceJid);
    }

    public long clientTimestampInMilliseconds() {
        return clientTimestampInMilliseconds;
    }

    public long serverTimestampMilliseconds() {
        return serverTimestampMilliseconds;
    }

    public Optional<ZonedDateTime> serverTimestamp() {
        if(serverTimestampMilliseconds > 0) {
            return Clock.parseMilliseconds(serverTimestampMilliseconds);
        } else if(serverTimestampSeconds > 0) {
            return Clock.parseSeconds(serverTimestampSeconds);
        } else {
            return Optional.empty();
        }
    }

    public Optional<ZonedDateTime> clientTimestamp() {
        return Clock.parseMilliseconds(clientTimestampInMilliseconds);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof KeepInChat that
                && Objects.equals(keepType, that.keepType)
                && serverTimestampSeconds == that.serverTimestampSeconds
                && Objects.equals(key, that.key)
                && Objects.equals(deviceJid, that.deviceJid)
                && clientTimestampInMilliseconds == that.clientTimestampInMilliseconds
                && serverTimestampMilliseconds == that.serverTimestampMilliseconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(keepType, serverTimestampSeconds, key, deviceJid, clientTimestampInMilliseconds, serverTimestampMilliseconds);
    }

    @Override
    public String toString() {
        return "KeepInChat[" +
                "keepType=" + keepType +
                ", serverTimestampSeconds=" + serverTimestampSeconds +
                ", key=" + key +
                ", deviceJid=" + deviceJid +
                ", clientTimestampInMilliseconds=" + clientTimestampInMilliseconds +
                ", serverTimestampMilliseconds=" + serverTimestampMilliseconds +
                ']';
    }

    @ProtobufEnum(name = "KeepType")
    public enum Type {
        UNKNOWN(0),
        KEEP_FOR_ALL(1),
        UNDO_KEEP_FOR_ALL(2);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}