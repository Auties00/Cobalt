package it.auties.whatsapp.model.privacy;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A model that represents a privacy setting entry
 */
@ProtobufMessage
public final class PrivacySettingEntry {
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final PrivacySettingType type;

    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    final PrivacySettingValue value;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<Jid> excluded;

    PrivacySettingEntry(PrivacySettingType type, PrivacySettingValue value, List<Jid> excluded) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
        this.excluded = Objects.requireNonNullElse(excluded, List.of());
    }

    public PrivacySettingType type() {
        return type;
    }

    public PrivacySettingValue value() {
        return value;
    }

    public List<Jid> excluded() {
        return Collections.unmodifiableList(excluded);
    }

    /**
     * Checks if {@link PrivacySettingEntry#value} == {@link PrivacySettingValue#CONTACTS_EXCEPT}
     *
     * @return a boolean
     */
    private boolean hasExcluded() {
        return value == PrivacySettingValue.CONTACTS_EXCEPT;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PrivacySettingEntry that
                && Objects.equals(type, that.type)
                && Objects.equals(value, that.value)
                && Objects.equals(excluded, that.excluded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value, excluded);
    }

    @Override
    public String toString() {
        return "PrivacySettingEntry[" +
                "type=" + type +
                ", value=" + value +
                ", excluded=" + excluded +
                ']';
    }
}