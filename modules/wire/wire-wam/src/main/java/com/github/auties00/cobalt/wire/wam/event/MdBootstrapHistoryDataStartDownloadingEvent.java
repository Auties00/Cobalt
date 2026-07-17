package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MdBootstrapHistoryPayloadType;
import com.github.auties00.cobalt.wire.wam.type.MdBootstrapPayloadType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdBootstrapHistoryDataStartDownloadingWamEvent")
@WamEvent(id = 4650)
public interface MdBootstrapHistoryDataStartDownloadingEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong historySyncChunkOrder();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> historySyncRetryRequestId();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong historySyncStageProgress();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<MdBootstrapHistoryPayloadType> mdBootstrapHistoryPayloadType();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong mdBootstrapPayloadSize();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<MdBootstrapPayloadType> mdBootstrapPayloadType();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong mdBootstrapStepDuration();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> mdSessionId();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong mdTimestamp();
}
