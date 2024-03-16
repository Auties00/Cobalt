package it.auties.whatsapp.registration.gcm;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

record AndroidCheckInData(
        @ProtobufProperty(index = 2, type = ProtobufType.INT64)
        long lastCheckInMs,
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        String cellOperator,
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        String simOperator,
        @ProtobufProperty(index = 8, type = ProtobufType.STRING)
        String roaming,
        @ProtobufProperty(index = 9, type = ProtobufType.INT32)
        int userNumber,
        @ProtobufProperty(index = 12, type = ProtobufType.OBJECT)
        DeviceType type,
        @ProtobufProperty(index = 13, type = ProtobufType.OBJECT)
        ChromeBuild chromeBuild
) implements ProtobufMessage {
    enum DeviceType implements ProtobufEnum {
        DEVICE_ANDROID_OS(1),
        DEVICE_IOS_OS(2),
        DEVICE_CHROME_BROWSER(3),
        DEVICE_CHROME_OS(4);

        final int index;
        DeviceType(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}
