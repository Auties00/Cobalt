package it.auties.whatsapp.protobuf.sync;

import java.util.List;
import java.util.Map;

public record MutationsRecord(byte[] hash, Map<String, byte[]> indexValueMap, List<ActionDataSync> records) {
}
