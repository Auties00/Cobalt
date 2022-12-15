package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.media.AttachmentProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class HistorySyncNotification implements ProtobufMessage, AttachmentProvider {
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] mediaSha256;

    @ProtobufProperty(index = 2, type = UINT64)
    private long mediaSize;

    @ProtobufProperty(index = 3, type = BYTES)
    private byte[] mediaKey;

    @ProtobufProperty(index = 4, type = BYTES)
    private byte[] mediaEncryptedSha256;

    @ProtobufProperty(index = 5, type = STRING)
    private String mediaDirectPath;

    @ProtobufProperty(index = 6, type = MESSAGE, implementation = HistorySyncNotificationHistorySyncType.class)
    private HistorySyncNotificationHistorySyncType syncType;

    @ProtobufProperty(index = 7, type = UINT32)
    private Integer chunkOrder;

    @ProtobufProperty(index = 8, type = STRING)
    private String originalMessageId;

    @Override
    public String mediaUrl() {
        return null;
    }

    @Override
    public AttachmentProvider mediaUrl(String mediaUrl) {
        return this;
    }

    @Override
    public String mediaName() {
        return "WhatsApp History Keys";
    }

    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum HistorySyncNotificationHistorySyncType {
        INITIAL_BOOTSTRAP(0),
        INITIAL_STATUS_V3(1),
        FULL(2),
        RECENT(3),
        PUSH_NAME(4);

        @Getter
        private final int index;

        @JsonCreator
        public static HistorySyncNotificationHistorySyncType of(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }
}
