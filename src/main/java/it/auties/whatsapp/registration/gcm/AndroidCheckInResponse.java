package it.auties.whatsapp.registration.gcm;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

record AndroidCheckInResponse(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean statsOk,
        @ProtobufProperty(index = 3, type = ProtobufType.INT64)
        long timeMs,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String digest,
        @ProtobufProperty(index = 9, type = ProtobufType.BOOL)
        boolean settingsDiff,
        @ProtobufProperty(index = 10, type = ProtobufType.STRING)
        List<String> deleteSetting,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        List<Setting> setting,
        @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
        boolean marketOk,
        @ProtobufProperty(index = 7, type = ProtobufType.FIXED64)
        long androidId,
        @ProtobufProperty(index = 8, type = ProtobufType.FIXED64)
        long securityToken,
        @ProtobufProperty(index = 11, type = ProtobufType.STRING)
        String versionInfo
) implements ProtobufMessage {
    record Setting(
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String key,
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String value
    ) implements ProtobufMessage {

    }
}