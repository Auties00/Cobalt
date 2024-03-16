package it.auties.whatsapp.registration.gcm;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

record AndroidCheckInRequest(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String imei,
        @ProtobufProperty(index = 10, type = ProtobufType.STRING)
        String meId,
        @ProtobufProperty(index = 9, type = ProtobufType.STRING)
        List<String> macAddress,
        @ProtobufProperty(index = 19, type = ProtobufType.STRING)
        List<String> macAddressType,
        @ProtobufProperty(index = 16, type = ProtobufType.STRING)
        String serialNumber,
        @ProtobufProperty(index = 17, type = ProtobufType.STRING)
        String esn,
        @ProtobufProperty(index = 2, type = ProtobufType.INT64)
        long id,
        @ProtobufProperty(index = 7, type = ProtobufType.INT64)
        long loggingId,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String digest,
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        String locale,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        AndroidCheckInData data,
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        String desiredBuild,
        @ProtobufProperty(index = 8, type = ProtobufType.STRING)
        String marketCheckIn,
        @ProtobufProperty(index = 11, type = ProtobufType.STRING)
        List<String> accountCookie,
        @ProtobufProperty(index = 12, type = ProtobufType.STRING)
        String timeZone,
        @ProtobufProperty(index = 13, type = ProtobufType.FIXED64)
        long securityToken,
        @ProtobufProperty(index = 14, type = ProtobufType.INT32)
        int version,
        @ProtobufProperty(index = 15, type = ProtobufType.STRING)
        List<String> otaCert,
        @ProtobufProperty(index = 20, type = ProtobufType.INT32)
        int fragment,
        @ProtobufProperty(index = 21, type = ProtobufType.STRING)
        String userName,
        @ProtobufProperty(index = 22, type = ProtobufType.INT32)
        int userSerialNumber
) implements ProtobufMessage {

}