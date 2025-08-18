package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage(name = "AutoDownloadSettings")
public record AutoDownloadSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean downloadImages,
        @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
        boolean downloadAudio,
        @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
        boolean downloadVideo,
        @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
        boolean downloadDocuments
) implements Setting {
    @Override
    public int settingVersion() {
        return -1;
    }

    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send setting: no index name");
    }
}