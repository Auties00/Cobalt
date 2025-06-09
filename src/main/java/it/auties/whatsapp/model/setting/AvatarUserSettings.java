package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

@ProtobufMessage(name = "AvatarUserSettings")
public final class AvatarUserSettings implements Setting {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String facebookId;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String password;

    AvatarUserSettings(String facebookId, String password) {
        this.facebookId = Objects.requireNonNull(facebookId, "facebookId cannot be null");
        this.password = Objects.requireNonNull(password, "password cannot be null");
    }

    public String facebookId() {
        return facebookId;
    }

    public String password() {
        return password;
    }

    @Override
    public int settingVersion() {
        return -1;
    }

    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send setting: no index name");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AvatarUserSettings that
                && Objects.equals(facebookId, that.facebookId)
                && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(facebookId, password);
    }

    @Override
    public String toString() {
        return "AvatarUserSettings[" +
                "facebookId=" + facebookId + ", " +
                "password=" + password + ']';
    }
}