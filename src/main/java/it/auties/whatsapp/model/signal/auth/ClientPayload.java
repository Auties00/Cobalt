package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import java.util.List;

import static it.auties.protobuf.model.ProtobufType.*;

@ProtobufMessage(name = "ClientPayload")
public record ClientPayload(@ProtobufProperty(index = 1, type = UINT64) Long username,
                            @ProtobufProperty(index = 3, type = BOOL) Boolean passive,
                            @ProtobufProperty(index = 5, type = MESSAGE) UserAgent userAgent,
                            @ProtobufProperty(index = 6, type = MESSAGE) WebInfo webInfo,
                            @ProtobufProperty(index = 7, type = STRING) String pushName,
                            @ProtobufProperty(index = 9, type = SFIXED32) Integer sessionId,
                            @ProtobufProperty(index = 10, type = BOOL) Boolean shortConnect,
                            @ProtobufProperty(index = 12, type = ENUM) ClientPayloadConnectType connectType,
                            @ProtobufProperty(index = 13, type = ENUM) ClientPayloadConnectReason connectReason,
                            @ProtobufProperty(index = 14, type = INT32) List<Integer> shards,
                            @ProtobufProperty(index = 15, type = MESSAGE) DNSSource dnsSource,
                            @ProtobufProperty(index = 16, type = UINT32) Integer connectAttemptCount,
                            @ProtobufProperty(index = 18, type = UINT32) Integer device,
                            @ProtobufProperty(index = 19, type = MESSAGE) CompanionRegistrationData regData,
                            @ProtobufProperty(index = 20, type = ENUM) ClientPayloadProduct product,
                            @ProtobufProperty(index = 21, type = BYTES) byte[] fbCat,
                            @ProtobufProperty(index = 22, type = BYTES) byte[] fbUserAgent,
                            @ProtobufProperty(index = 23, type = BOOL) Boolean oc,
                            @ProtobufProperty(index = 24, type = INT32) Integer lc,
                            @ProtobufProperty(index = 30, type = ENUM) ClientPayloadIOSAppExtension iosAppExtension,
                            @ProtobufProperty(index = 31, type = UINT64) Long fbAppId,
                            @ProtobufProperty(index = 32, type = BYTES) byte[] fbDeviceId,
                            @ProtobufProperty(index = 33, type = BOOL) Boolean pull) {

    @ProtobufEnum
    public enum ClientPayloadConnectType {
        CELLULAR_UNKNOWN(0),
        WIFI_UNKNOWN(1),
        CELLULAR_EDGE(100),
        CELLULAR_IDEN(101),
        CELLULAR_UMTS(102),
        CELLULAR_EVDO(103),
        CELLULAR_GPRS(104),
        CELLULAR_HSDPA(105),
        CELLULAR_HSUPA(106),
        CELLULAR_HSPA(107),
        CELLULAR_CDMA(108),
        CELLULAR_1XRTT(109),
        CELLULAR_EHRPD(110),
        CELLULAR_LTE(111),
        CELLULAR_HSPAP(112);

        ClientPayloadConnectType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }
    }

    @ProtobufEnum
    public enum ClientPayloadConnectReason {
        PUSH(0),
        USER_ACTIVATED(1),
        SCHEDULED(2),
        ERROR_RECONNECT(3),
        NETWORK_SWITCH(4),
        PING_RECONNECT(5);

        ClientPayloadConnectReason(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }
    }

    @ProtobufEnum
    public enum ClientPayloadProduct {
        WHATSAPP(0),
        MESSENGER(1);

        ClientPayloadProduct(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }
    }

    @ProtobufEnum
    public enum ClientPayloadIOSAppExtension {
        SHARE_EXTENSION(0),
        SERVICE_EXTENSION(1),
        INTENTS_EXTENSION(2);

        ClientPayloadIOSAppExtension(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }
    }
}
