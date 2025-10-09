package com.github.auties00.cobalt.media;

import java.io.IOException;
import java.io.InputStream;

// TODO: Implement an input stream that transforms the underlying picture in a 640x640 jpg image thumbnail
public final class MediaProfilePictureInputStream extends MediaInputStream {
    public MediaProfilePictureInputStream(InputStream rawInputStream) {
        super(rawInputStream);
    }

    @Override
    public int available() {
        return 0;
    }

    @Override
    public int read() throws IOException {
        return 0;
    }
}
