package it.auties.whatsapp.model.setting;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("AutoDownloadSettings")
public final class AutoDownloadSettings implements Setting {
    @ProtobufProperty(index = 1, name = "downloadImages", type = ProtobufType.BOOL)
    private boolean downloadImages;

    @ProtobufProperty(index = 2, name = "downloadAudio", type = ProtobufType.BOOL)
    private boolean downloadAudio;

    @ProtobufProperty(index = 3, name = "downloadVideo", type = ProtobufType.BOOL)
    private boolean downloadVideo;

    @ProtobufProperty(index = 4, name = "downloadDocuments", type = ProtobufType.BOOL)
    private boolean downloadDocuments;

    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send setting: no index name");
    }
}