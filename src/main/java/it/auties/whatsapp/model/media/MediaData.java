package it.auties.whatsapp.model.media;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class MediaData implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = STRING)
    private String localPath;

    @ProtobufProperty(index = 2, name = "mediaKeyTimestamp", type = INT64)
    private Long mediaKeyTimestamp;

    @ProtobufProperty(index = 3, name = "fileSha256", type = BYTES)
    private byte[] fileSha256;

    @ProtobufProperty(index = 4, name = "fileEncSha256", type = BYTES)
    private byte[] fileEncSha256;

    @ProtobufProperty(index = 5, name = "directPath", type = STRING)
    private String directPath;
}