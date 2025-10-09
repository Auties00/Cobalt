package com.github.auties00.cobalt.io.media.download;

import com.github.auties00.cobalt.io.media.MediaInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public abstract class MediaDownloadInputStream extends MediaInputStream {
    private final Inflater inflater;
    protected MediaDownloadInputStream(InputStream rawInputStream, boolean inflate) {
        Objects.requireNonNull(rawInputStream, "rawInputStream must not be null");
        var inflater = inflate ? new Inflater() : null;
        super(inflater != null ? new InflaterInputStream(rawInputStream, inflater, BUFFER_LENGTH) : rawInputStream);
        this.inflater = inflater;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if(inflater != null) {
            inflater.close();
        }
    }
}
