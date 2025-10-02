package it.auties.whatsapp.stream.webAppState;

import it.auties.whatsapp.model.companion.CompanionHashState;
import it.auties.whatsapp.model.sync.ActionDataSync;

import java.util.List;

record SyncRecord(CompanionHashState state, List<ActionDataSync> records) {

}
