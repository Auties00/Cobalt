package com.github.auties00.cobalt.model.media;

import java.util.List;

public final class MediaConnection {
    private final String auth;
    private final int ttl;
    private final int maxBuckets;
    private final long timestamp;
    private final List<String> hosts;

    public MediaConnection(String auth, int ttl, int maxBuckets, long timestamp, List<String> hosts) {
        this.auth = auth;
        this.ttl = ttl;
        this.maxBuckets = maxBuckets;
        this.timestamp = timestamp;
        this.hosts = hosts;
    }

    public String auth() {
        return auth;
    }

    public int ttl() {
        return ttl;
    }

    public int maxBuckets() {
        return maxBuckets;
    }

    public long timestamp() {
        return timestamp;
    }

    public List<String> hosts() {
        return hosts;
    }

    @Override
    public String toString() {
        return "MediaConnection[" +
               "auth=" + auth + ", " +
               "ttl=" + ttl + ", " +
               "maxBuckets=" + maxBuckets + ", " +
               "timestamp=" + timestamp + ", " +
               "hosts=" + hosts + ']';
    }
}
