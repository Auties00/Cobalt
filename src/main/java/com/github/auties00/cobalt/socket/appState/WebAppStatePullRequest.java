package com.github.auties00.cobalt.socket.appState;

import com.github.auties00.cobalt.model.sync.PatchType;

import java.util.Set;

public record WebAppStatePullRequest(Set<PatchType> types, boolean fullSync, boolean initialSync) {

}
