package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChannelDirectoryAction;
import com.github.auties00.cobalt.wire.wam.type.ChannelDirectoryEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.ChannelDirectoryImpReason;
import com.github.auties00.cobalt.wire.wam.type.ChannelDirectoryPillSelected;
import com.github.auties00.cobalt.wire.wam.type.ChannelDirectorySurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChannelDirectoryWamEvent")
@WamEvent(id = 4544)
public interface ChannelDirectoryEvent extends WamEventSpec {
    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong channelCategoryIndex();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> channelCategoryName();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ChannelDirectoryAction> channelDirectoryAction();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong channelDirectoryActionSequenceNumber();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ChannelDirectoryEntryPoint> channelDirectoryEntryPoint();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> channelDirectorySearchSessionId();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong channelDirectorySessionId();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<ChannelDirectorySurface> channelDirectorySurface();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong channelIndex();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> countrySelector();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<ChannelDirectoryImpReason> impReason();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<ChannelDirectoryPillSelected> pillSelected();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> searchMode();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();
}
