package it.auties.whatsapp.registration.gcm;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Optional;

final class McsExchange {
    static final byte TAG_HEARTBEAT_PING = 0;
    static final byte TAG_HEARTBEAT_ACK = 1;
    static final byte TAG_LOGIN_REQUEST = 2;
    static final byte TAG_LOGIN_RESPONSE = 3;
    static final byte TAG_CLOSE = 4;
    static final byte TAG_MESSAGE_STANZA = 5;
    static final byte TAG_PRESENCE_STANZA = 6;
    static final byte TAG_IQ_STANZA = 7;
    static final byte TAG_DATA_MESSAGE_STANZA = 8;
    static final byte TAG_BATCH_PRESENCE_STANZA = 9;
    static final byte TAG_STREAM_ERROR_STANZA = 10;
    static final byte TAG_HTTP_REQUEST = 11;
    static final byte TAG_HTTP_RESPONSE = 12;
    static final byte TAG_BIND_ACCOUNT_REQUEST = 13;
    static final byte TAG_BIND_ACCOUNT_RESPONSE = 14;
    static final byte TAG_TALK_METADATA = 15;
    static final byte TAG_NUM_PROTO_TYPES = 16;

            
    private McsExchange() {

    }

    static Optional<ProtobufMessage> readMessage(byte tag, byte[] message) {
        return Optional.ofNullable(switch (tag) {
            case TAG_HEARTBEAT_PING -> McsExchangeHeartbeatPingSpec.decode(message);
            case TAG_HEARTBEAT_ACK -> McsExchangeHeartbeatAckSpec.decode(message);
            case TAG_LOGIN_REQUEST -> McsExchangeLoginRequestSpec.decode(message);
            case TAG_LOGIN_RESPONSE -> McsExchangeLoginResponseSpec.decode(message);
            case TAG_CLOSE -> McsExchangeCloseSpec.decode(message);
            case TAG_IQ_STANZA -> McsExchangeIqStanzaSpec.decode(message);
            case TAG_DATA_MESSAGE_STANZA -> McsExchangeDataMessageStanzaSpec.decode(message);
            case TAG_STREAM_ERROR_STANZA -> McsExchangeStreamErrorStanzaSpec.decode(message);
            default -> null;
        });
    }
    
    record HeartbeatPing(
            @ProtobufProperty(index = 1, type = ProtobufType.INT32)
            int streamId,
            @ProtobufProperty(index = 2, type = ProtobufType.INT32)
            int lastStreamIdReceived,
            @ProtobufProperty(index = 3, type = ProtobufType.INT64)
            long status
    ) implements ProtobufMessage {

    }

    record HeartbeatAck(
            @ProtobufProperty(index = 1, type = ProtobufType.INT32)
            int streamId,
            @ProtobufProperty(index = 2, type = ProtobufType.INT32)
            int lastStreamIdReceived,
            @ProtobufProperty(index = 3, type = ProtobufType.INT64)
            long status
    ) implements ProtobufMessage {

    }

    record ErrorInfo(
            @ProtobufProperty(index = 1, type = ProtobufType.INT32)
            int code,
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String message,
            @ProtobufProperty(index = 3, type = ProtobufType.STRING)
            String type,
            @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
            Extension extension
    ) implements ProtobufMessage {

    }

    record Setting(
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String name,
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String value
    ) implements ProtobufMessage {

    }

    record HeartbeatStat(
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String ip,
            @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
            boolean timeout,
            @ProtobufProperty(index = 3, type = ProtobufType.INT32)
            int intervalMs
    ) implements ProtobufMessage {

    }

    record HeartbeatConfig(
            @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
            boolean uploadStat,
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String ip,
            @ProtobufProperty(index = 3, type = ProtobufType.INT32)
            int intervalMs
    ) implements ProtobufMessage {

    }

    record ClientEvent(
            @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
            Type type,
            @ProtobufProperty(index = 100, type = ProtobufType.UINT32)
            int numberDiscardedEvents,
            @ProtobufProperty(index = 200, type = ProtobufType.INT32)
            int networkType,
            @ProtobufProperty(index = 202, type = ProtobufType.UINT64)
            long timeConnectionStartedMs,
            @ProtobufProperty(index = 203, type = ProtobufType.UINT64)
            long timeConnectionEndedMs,
            @ProtobufProperty(index = 204, type = ProtobufType.INT32)
            int errorCode,
            @ProtobufProperty(index = 300, type = ProtobufType.UINT64)
            long timeConnectionEstablishedMs
    ) implements ProtobufMessage {
        enum Type implements ProtobufEnum {
            UNKNOWN(0),
            DISCARDED_EVENTS(1),
            FAILED_CONNECTION(2),
            SUCCESSFUL_CONNECTION(3);

            final int index;

            Type(@ProtobufEnumIndex int index) {
                this.index = index;
            }
        }
    }

    record LoginRequest(
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String id,
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String domain,
            @ProtobufProperty(index = 3, type = ProtobufType.STRING)
            String user,
            @ProtobufProperty(index = 4, type = ProtobufType.STRING)
            String resource,
            @ProtobufProperty(index = 5, type = ProtobufType.STRING)
            String authToken,
            @ProtobufProperty(index = 6, type = ProtobufType.STRING)
            String deviceId,
            @ProtobufProperty(index = 7, type = ProtobufType.INT64)
            Long lastRmqId,
            @ProtobufProperty(index = 8, type = ProtobufType.OBJECT)
            List<Setting> setting,
            @ProtobufProperty(index = 10, type = ProtobufType.STRING)
            List<String> receivedPersistentId,
            @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
            Boolean adaptiveHeartbeat,
            @ProtobufProperty(index = 13, type = ProtobufType.OBJECT)
            HeartbeatStat heartbeatStat,
            @ProtobufProperty(index = 14, type = ProtobufType.BOOL)
            Boolean useRmq2,
            @ProtobufProperty(index = 15, type = ProtobufType.INT64)
            Long accountId,
            @ProtobufProperty(index = 16, type = ProtobufType.OBJECT)
            AuthService authService,
            @ProtobufProperty(index = 17, type = ProtobufType.INT32)
            Integer networkType,
            @ProtobufProperty(index = 18, type = ProtobufType.INT64)
            Long status,
            @ProtobufProperty(index = 22, type = ProtobufType.OBJECT)
            List<ClientEvent> clientEvent
    ) implements ProtobufMessage {
        enum AuthService implements ProtobufEnum {
            ANDROID_ID(2);

            final int index;

            AuthService(@ProtobufEnumIndex int index) {
                this.index = index;
            }
        }
    }

    record LoginResponse(
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String id,
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String jid,
            @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
            ErrorInfo error,
            @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
            List<Setting> setting,
            @ProtobufProperty(index = 5, type = ProtobufType.INT32)
            int streamId,
            @ProtobufProperty(index = 6, type = ProtobufType.INT32)
            int lastStreamIdReceived,
            @ProtobufProperty(index = 7, type = ProtobufType.OBJECT)
            HeartbeatConfig heartbeatConfig,
            @ProtobufProperty(index = 8, type = ProtobufType.INT64)
            long serverTimestamp
    ) implements ProtobufMessage {

    }

    record StreamErrorStanza(
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String type,
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String text
    ) implements ProtobufMessage {

    }

    record Close() implements ProtobufMessage {

    }

    record Extension(
            @ProtobufProperty(index = 1, type = ProtobufType.INT32)
            int id,
            @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
            byte[] data
    ) implements ProtobufMessage {

    }

    record IqStanza(
            @ProtobufProperty(index = 1, type = ProtobufType.INT64)
            long rmqId,
            @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
            IqType type,
            @ProtobufProperty(index = 3, type = ProtobufType.STRING)
            String id,
            @ProtobufProperty(index = 4, type = ProtobufType.STRING)
            String from,
            @ProtobufProperty(index = 5, type = ProtobufType.STRING)
            String to,
            @ProtobufProperty(index = 6, type = ProtobufType.OBJECT)
            ErrorInfo error,
            @ProtobufProperty(index = 7, type = ProtobufType.OBJECT)
            Extension extension,
            @ProtobufProperty(index = 8, type = ProtobufType.STRING)
            String persistentId,
            @ProtobufProperty(index = 9, type = ProtobufType.INT32)
            int streamId,
            @ProtobufProperty(index = 10, type = ProtobufType.INT32)
            int lastStreamIdReceived,
            @ProtobufProperty(index = 11, type = ProtobufType.INT64)
            long accountId,
            @ProtobufProperty(index = 12, type = ProtobufType.INT64)
            long status
    ) implements ProtobufMessage {
        enum IqType implements ProtobufEnum {
            GET(0),
            SET(1),
            RESULT(2),
            IQ_ERROR(3);

            final int index;

            IqType(@ProtobufEnumIndex int index) {
                this.index = index;
            }
        }
    }

    record AppData(
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String key,
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String value
    ) implements ProtobufMessage {

    }

    record DataMessageStanza(
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String id,
            @ProtobufProperty(index = 3, type = ProtobufType.STRING)
            String from,
            @ProtobufProperty(index = 4, type = ProtobufType.STRING)
            String to,
            @ProtobufProperty(index = 5, type = ProtobufType.STRING)
            String category,
            @ProtobufProperty(index = 7, type = ProtobufType.OBJECT)
            List<AppData> appData,
            @ProtobufProperty(index = 9, type = ProtobufType.STRING)
            String persistentId,
            @ProtobufProperty(index = 10, type = ProtobufType.INT32)
            int streamId,
            @ProtobufProperty(index = 11, type = ProtobufType.INT32)
            int lastStreamIdReceived,
            @ProtobufProperty(index = 13, type = ProtobufType.STRING)
            String regId,
            @ProtobufProperty(index = 16, type = ProtobufType.INT64)
            long deviceUserId,
            @ProtobufProperty(index = 17, type = ProtobufType.INT32)
            int ttl,
            @ProtobufProperty(index = 18, type = ProtobufType.INT64)
            long sent,
            @ProtobufProperty(index = 19, type = ProtobufType.INT32)
            int queued,
            @ProtobufProperty(index = 20, type = ProtobufType.INT64)
            long status,
            @ProtobufProperty(index = 21, type = ProtobufType.BYTES)
            byte[] rawData,
            @ProtobufProperty(index = 24, type = ProtobufType.BOOL)
            boolean immediateAck
    ) implements ProtobufMessage {

    }

    record StreamAck() implements ProtobufMessage {

    }

    record SelectiveAck(
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            List<String> id
    ) implements ProtobufMessage {

    }
}
