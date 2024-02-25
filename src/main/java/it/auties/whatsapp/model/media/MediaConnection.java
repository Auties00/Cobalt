package it.auties.whatsapp.model.media;


import java.util.List;

public record MediaConnection(String auth, int ttl, int maxBuckets, long timestamp, List<String> hosts) {
}
