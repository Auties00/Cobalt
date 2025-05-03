package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

@ProtobufMessage(name = "AutoDownloadSettings")
public final class AutoDownloadSettings implements Setting {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean downloadImages;

    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean downloadAudio;

    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final boolean downloadVideo;

    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean downloadDocuments;

    AutoDownloadSettings(boolean downloadImages, boolean downloadAudio, boolean downloadVideo, boolean downloadDocuments) {
        this.downloadImages = downloadImages;
        this.downloadAudio = downloadAudio;
        this.downloadVideo = downloadVideo;
        this.downloadDocuments = downloadDocuments;
    }

    public boolean downloadImages() {
        return downloadImages;
    }

    public boolean downloadAudio() {
        return downloadAudio;
    }

    public boolean downloadVideo() {
        return downloadVideo;
    }

    public boolean downloadDocuments() {
        return downloadDocuments;
    }

    @Override
    public int settingVersion() {
        return -1;
    }

    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send setting: no index name");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AutoDownloadSettings that
                && downloadImages == that.downloadImages
                && downloadAudio == that.downloadAudio
                && downloadVideo == that.downloadVideo
                && downloadDocuments == that.downloadDocuments;
    }

    @Override
    public int hashCode() {
        return Objects.hash(downloadImages, downloadAudio, downloadVideo, downloadDocuments);
    }

    @Override
    public String toString() {
        return "AutoDownloadSettings[" +
                "downloadImages=" + downloadImages + ", " +
                "downloadAudio=" + downloadAudio + ", " +
                "downloadVideo=" + downloadVideo + ", " +
                "downloadDocuments=" + downloadDocuments + ']';
    }
}