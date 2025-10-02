package it.auties.whatsapp.stream.webAppState2;

import it.auties.whatsapp.model.sync.PatchType;

import java.util.Set;

public record WebAppStatePullRequest(Set<PatchType> types, boolean fullSync, boolean initialSync) {

}
