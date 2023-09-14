package it.auties.whatsapp.model.media;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

public record MediaConnection(@NonNull String auth, int ttl, int maxBuckets, long timestamp, @NonNull List<@NonNull String> hosts) {
}
