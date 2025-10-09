package com.github.auties00.cobalt.io.media;

import java.io.IOException;
import java.io.InputStream;

// TODO: Implement an input stream that guesses the mime type of the underlying media and computes metadata while streaming
public final class MediaMetadataInputStream extends MediaInputStream{
    public MediaMetadataInputStream(InputStream rawInputStream) {
        super(rawInputStream);
    }

    @Override
    public int read() throws IOException {
        return 0;
    }
}
