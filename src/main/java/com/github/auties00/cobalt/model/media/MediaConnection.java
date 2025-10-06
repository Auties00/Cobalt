package com.github.auties00.cobalt.model.media;

import java.util.List;

public record MediaConnection(String auth, int ttl, int maxBuckets, long timestamp, List<String> hosts) {

}
