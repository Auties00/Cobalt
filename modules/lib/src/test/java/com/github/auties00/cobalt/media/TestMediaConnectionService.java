package com.github.auties00.cobalt.media;

import com.github.auties00.cobalt.wire.linked.media.MediaPath;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.io.InputStream;
import java.util.SequencedCollection;

/**
 * Test double for {@link MediaConnectionService} whose every method throws
 * {@link UnsupportedOperationException}, letting a test supply a collaborator
 * without exercising the CDN; any unexpected media-connection call fails the
 * test with a message naming the called method.
 */
public final class TestMediaConnectionService implements MediaConnectionService {
    public static TestMediaConnectionService create() {
        return new TestMediaConnectionService();
    }

    @Override
    public void update(Stanza response) {
        throw unsupported("update");
    }

    @Override
    public boolean upload(MediaProvider provider, MediaPayload payload) {
        throw unsupported("upload");
    }

    @Override
    public InputStream download(MediaProvider provider) {
        throw unsupported("download");
    }

    @Override
    public InputStream tryDownload(MediaProvider provider, String downloadUrl) {
        throw unsupported("tryDownload");
    }

    @Override
    public void checkExistence(String hostname, MediaPath mediaType, String directPath, String encFileHash) {
        throw unsupported("checkExistence");
    }

    @Override
    public long getEncryptedMediaSize(String hostname, MediaPath mediaType, String directPath, String encFileHash) {
        throw unsupported("getEncryptedMediaSize");
    }

    @Override
    public String auth() {
        throw unsupported("auth");
    }

    @Override
    public int ttl() {
        throw unsupported("ttl");
    }

    @Override
    public int authTtl() {
        throw unsupported("authTtl");
    }

    @Override
    public int maxBuckets() {
        throw unsupported("maxBuckets");
    }

    @Override
    public int maxManualRetry() {
        throw unsupported("maxManualRetry");
    }

    @Override
    public int maxAutoDownloadRetry() {
        throw unsupported("maxAutoDownloadRetry");
    }

    @Override
    public long timestamp() {
        throw unsupported("timestamp");
    }

    @Override
    public SequencedCollection<? extends MediaHost> hosts() {
        throw unsupported("hosts");
    }

    @Override
    public boolean isExpired() {
        throw unsupported("isExpired");
    }

    @Override
    public boolean needsRefresh() {
        throw unsupported("needsRefresh");
    }

    private static UnsupportedOperationException unsupported(String method) {
        return new UnsupportedOperationException("TestMediaConnectionService." + method + " is not stubbed");
    }
}
