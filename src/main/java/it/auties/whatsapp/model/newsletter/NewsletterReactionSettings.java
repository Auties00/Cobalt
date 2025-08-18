package it.auties.whatsapp.model.newsletter;

import com.alibaba.fastjson2.JSONObject;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@ProtobufMessage
public final class NewsletterReactionSettings {
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final Type value;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final List<String> blockedCodes;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    final long enabledTimestampSeconds;

    NewsletterReactionSettings(Type value, List<String> blockedCodes, long enabledTimestampSeconds) {
        this.value = Objects.requireNonNullElse(value, Type.UNKNOWN);
        this.blockedCodes = Objects.requireNonNullElse(blockedCodes, List.of());
        this.enabledTimestampSeconds = enabledTimestampSeconds;
    }

    public static Optional<NewsletterReactionSettings> ofJson(JSONObject jsonObject) {
        if(jsonObject == null) {
            return Optional.empty();
        }

        var value = Type.of(jsonObject.getString("type"));
        var blockedCodesJsonValues = jsonObject.getJSONArray("blocked_codes");
        var blockedCodes = blockedCodesJsonValues == null ? new ArrayList<String>() : new ArrayList<String>(blockedCodesJsonValues.size());
        if(blockedCodesJsonValues != null) {
            for (var i = 0; i < blockedCodesJsonValues.size(); i++) {
                blockedCodes.add(blockedCodesJsonValues.getString(i));
            }
        }
        var enabledTimestampSeconds = jsonObject.getLongValue("enabled_ts_sec", 0);
        var result = new NewsletterReactionSettings(value, blockedCodes, enabledTimestampSeconds);
        return Optional.of(result);
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

        private static final Map<String, Type> BY_NAME = Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(key -> key.name().toLowerCase(), Function.identity()));

        static Type of(String name) {
            return name == null ? UNKNOWN : BY_NAME.getOrDefault(name.toLowerCase(), UNKNOWN);
        }

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}