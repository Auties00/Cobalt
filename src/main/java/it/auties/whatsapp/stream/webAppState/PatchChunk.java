package it.auties.whatsapp.stream.webAppState;

import it.auties.whatsapp.model.sync.ActionDataSync;
import it.auties.whatsapp.model.sync.PatchType;

import java.util.List;

record PatchChunk(PatchType patchType, List<ActionDataSync> records, boolean hasMore) {

}
