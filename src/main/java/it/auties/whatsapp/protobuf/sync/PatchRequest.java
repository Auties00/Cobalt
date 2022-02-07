package it.auties.whatsapp.protobuf.sync;

public record PatchRequest(ActionValueSync action, String index, String type, int version, MutationSync.Operation operation) {

}
