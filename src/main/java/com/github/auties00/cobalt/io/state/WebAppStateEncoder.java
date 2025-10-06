package com.github.auties00.cobalt.io.state;

import com.github.auties00.cobalt.socket.appState.WebAppStatePatch;
import com.github.auties00.cobalt.model.sync.AppStateSyncHash;
import com.github.auties00.cobalt.model.sync.AppStateSyncKey;

import java.nio.ByteBuffer;
import java.util.SequencedCollection;

public final class WebAppStateEncoder {
    public static ByteBuffer encode(AppStateSyncKey appStateSyncKey, AppStateSyncHash appStateSyncHash, SequencedCollection<WebAppStatePatch> patches) {
        throw new UnsupportedOperationException();
    }
}
