package it.auties.whatsapp.io.state;

import it.auties.whatsapp.stream.webAppState2.WebAppStatePatch;
import it.auties.whatsapp.model.sync.AppStateSyncHash;
import it.auties.whatsapp.model.sync.AppStateSyncKey;

import java.nio.ByteBuffer;
import java.util.SequencedCollection;

public final class WebAppStateEncoder {
    public static ByteBuffer encode(AppStateSyncKey appStateSyncKey, AppStateSyncHash appStateSyncHash, SequencedCollection<WebAppStatePatch> patches) {
        throw new UnsupportedOperationException();
    }
}
