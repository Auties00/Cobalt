package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.PeerDataRequestType;
import com.github.auties00.cobalt.wire.wam.type.PeerDataResponseResultType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebNonMessagePeerDataMediaUploadWamEvent")
@WamEvent(id = 3902)
public interface NonMessagePeerDataMediaUploadEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong peerDataErrorCount();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong peerDataExistingDataNoUploadCount();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong peerDataNotFoundCount();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong peerDataRequestCount();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> peerDataRequestSessionId();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<PeerDataRequestType> peerDataRequestType();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<PeerDataResponseResultType> peerDataResponseResult();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong peerDataSuccessInlineNoUploadCount();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong peerDataSuccessUploadCount();
}
