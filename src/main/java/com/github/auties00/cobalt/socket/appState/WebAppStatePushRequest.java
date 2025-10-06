package com.github.auties00.cobalt.socket.appState;

import com.github.auties00.cobalt.model.sync.PatchType;

import java.util.List;

public record WebAppStatePushRequest(PatchType type, List<WebAppStatePatch> patches) {

}
