package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.OBJECT;
import static it.auties.protobuf.model.ProtobufType.STRING;

@ProtobufMessageName("ClientPayload.UserAgent")
public record UserAgent(@ProtobufProperty(index = 1, type = OBJECT) PlatformType platform,
                        @ProtobufProperty(index = 2, type = OBJECT) Version appVersion,
                        @ProtobufProperty(index = 3, type = STRING) String mcc,
                        @ProtobufProperty(index = 4, type = STRING) String mnc,
                        @ProtobufProperty(index = 5, type = STRING) String osVersion,
                        @ProtobufProperty(index = 6, type = STRING) String manufacturer,
                        @ProtobufProperty(index = 7, type = STRING) String device,
                        @ProtobufProperty(index = 8, type = STRING) String osBuildNumber,
                        @ProtobufProperty(index = 9, type = STRING) String phoneId,
                        @ProtobufProperty(index = 10, type = OBJECT) ReleaseChannel releaseChannel,
                        @ProtobufProperty(index = 11, type = STRING) String localeLanguageIso6391,
                        @ProtobufProperty(index = 12, type = STRING) String localeCountryIso31661Alpha2,
                        @ProtobufProperty(index = 13, type = STRING) String deviceBoard) implements ProtobufMessage {

    @ProtobufMessageName("ClientPayload.UserAgent.Platform")
    public enum PlatformType implements ProtobufEnum {
        UNKNOWN(999),
        ANDROID(0),
        IOS(1),
        WINDOWS_PHONE(2),
        BLACKBERRY(3),
        BLACKBERRYX(4),
        S40(5),
        S60(6),
        PYTHON_CLIENT(7),
        TIZEN(8),
        ENTERPRISE(9),
        SMB_ANDROID(10),
        KAIOS(11),
        SMB_IOS(12),
        WINDOWS(13),
        WEB(14),
        PORTAL(15),
        GREEN_ANDROID(16),
        GREEN_IPHONE(17),
        BLUE_ANDROID(18),
        BLUE_IPHONE(19),
        FBLITE_ANDROID(20),
        MLITE_ANDROID(21),
        IGLITE_ANDROID(22),
        PAGE(23),
        MACOS(24),
        OCULUS_MSG(25),
        OCULUS_CALL(26),
        MILAN(27),
        CAPI(28);

        PlatformType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }

        public boolean isBusiness() {
            return this == SMB_ANDROID || this == SMB_IOS || this == ENTERPRISE;
        }
    }

    @ProtobufMessageName("ClientPayload.UserAgent.ReleaseChannel")
    public enum ReleaseChannel implements ProtobufEnum {

        RELEASE(0),
        BETA(1),
        ALPHA(2),
        DEBUG(3);

        ReleaseChannel(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }
    }
}
