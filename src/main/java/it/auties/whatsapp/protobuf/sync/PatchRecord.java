package it.auties.whatsapp.protobuf.sync;

import java.util.List;

public record PatchRecord(LTHashState state, List<GenericSync> mutations) {
}
