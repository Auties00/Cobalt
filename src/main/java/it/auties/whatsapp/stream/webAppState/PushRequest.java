package it.auties.whatsapp.stream.webAppState;

import it.auties.whatsapp.model.companion.CompanionHashState;
import it.auties.whatsapp.model.sync.PatchSync;
import it.auties.whatsapp.model.sync.PatchType;

record PushRequest(PatchType type, CompanionHashState oldState, CompanionHashState newState,
                   PatchSync sync) {

}
