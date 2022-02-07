package it.auties.whatsapp.protobuf.sync;

public record SyncRequest(LTHashState state, PatchSync patch) {
}
