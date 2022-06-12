package it.auties.whatsapp.model.sync;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.media.AttachmentProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class ExternalBlobReference implements ProtobufMessage, AttachmentProvider {
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] key;

    @ProtobufProperty(index = 2, type = STRING)
    private String directPath;

    @ProtobufProperty(index = 3, type = STRING)
    private String handle;

    @ProtobufProperty(index = 4, type = UINT64)
    private long fileLength;

    @ProtobufProperty(index = 5, type = BYTES)
    private byte[] fileSha256;

    @ProtobufProperty(index = 6, type = BYTES)
    private byte[] fileEncSha256;

    @Override
    public String url() {
        return null;
    }

    @Override
    public String name() {
        return "md-app-state";
    }

    @Override
    public String keyName() {
        return "WhatsApp App State Keys";
    }
}
