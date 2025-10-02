package it.auties.whatsapp.stream.webAppState2;

import it.auties.whatsapp.model.sync.PatchType;

import java.util.List;

public record WebAppStatePushRequest(PatchType type, List<WebAppStatePatch> patches) {

}
