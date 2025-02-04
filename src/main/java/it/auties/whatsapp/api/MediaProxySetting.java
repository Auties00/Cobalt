package it.auties.whatsapp.api;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 *  The constants of this enumerated type describe the various ways the proxy should be used when downloading or uploading a media to Whatsapp's servers
 *  By default, ALL is used to protect the real IP address
 */
@ProtobufEnum
public enum MediaProxySetting {
    NONE(0),
    DOWNLOADS(1),
    UPLOADS(2),
    ALL(3);

    final int index;

    MediaProxySetting(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public boolean allowsUploads() {
        return this == ALL || this == UPLOADS;
    }

    public boolean allowsDownloads() {
        return this == ALL || this == DOWNLOADS;
    }
}
