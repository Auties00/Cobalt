package it.auties.whatsapp.protobuf.sync;

import java.util.ArrayList;
import java.util.List;

public record AppStateChunk(List<ActionSyncRecord> mutations, List<String> collectionToHandle) {
    public AppStateChunk(){
        this(new ArrayList<>(), new ArrayList<>());
    }
}
