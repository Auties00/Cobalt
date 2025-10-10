package com.github.auties00.cobalt.io.sync;

import com.github.auties00.cobalt.model.sync.AppStateSyncHash;
import com.github.auties00.cobalt.model.sync.AppStateSyncKey;
import com.github.auties00.cobalt.socket.appState.WebAppStatePatch;

import java.nio.ByteBuffer;
import java.util.SequencedCollection;

public final class WebAppStateEncoder {
    public static ByteBuffer encode(AppStateSyncKey appStateSyncKey, AppStateSyncHash appStateSyncHash, SequencedCollection<WebAppStatePatch> patches) {
        throw new UnsupportedOperationException();
    }
}
