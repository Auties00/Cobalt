package it.auties.whatsapp.model.newsletter;

import io.avaje.jsonb.Json;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@ProtobufMessage
@Json
public final class NewsletterReactionSettings {
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    @Json.Property("type")
    final Type value;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    @Json.Property("blocked_codes")
    final List<String> blockedCodes;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    @Json.Property("enabled_ts_sec")
    final long enabledTimestampSeconds;

    NewsletterReactionSettings(Type value, List<String> blockedCodes, long enabledTimestampSeconds) {
        this.value = Objects.requireNonNullElse(value, Type.UNKNOWN);
        this.blockedCodes = Objects.requireNonNullElse(blockedCodes, List.of());
        this.enabledTimestampSeconds = enabledTimestampSeconds;
    }

    public Type value() {
        return value;
    }

    public List<String> blockedCodes() {
        return Collections.unmodifiableList(blockedCodes);
    }

    public long enabledTimestampSeconds() {
        return enabledTimestampSeconds;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterReactionSettings that
                && Objects.equals(value, that.value)
                && Objects.equals(blockedCodes, that.blockedCodes)
                && enabledTimestampSeconds == that.enabledTimestampSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, blockedCodes, enabledTimestampSeconds);
    }

    @Override
    public String toString() {
        return "NewsletterReactionSettings[" +
                "value=" + value +
                ", blockedCodes=" + blockedCodes +
                ", enabledTimestampSeconds=" + enabledTimestampSeconds +
                ']';
    }

    @ProtobufEnum
    public enum Type {
        UNKNOWN(0),
        ALL(1),
        BASIC(2),
        NONE(3),
        BLOCKLIST(4);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}