package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.UpdateTabSearchEventType;
import com.github.auties00.cobalt.wire.wam.type.UpdatesTabSearchModeType;
import com.github.auties00.cobalt.wire.wam.type.UpdatesTabSearchResultType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebUpdatesTabSearchWamEvent")
@WamEvent(id = 4838)
public interface UpdatesTabSearchEvent extends WamEventSpec {
    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong channelsAdminCount();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong channelsFollowedCount();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong premiumChannelsFollowedCount();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong recentStatusItemCount();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong recentStatusRowCount();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<UpdateTabSearchEventType> updateTabSearchEventType();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<UpdatesTabSearchModeType> updatesTabSearchModeType();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<UpdatesTabSearchResultType> updatesTabSearchResultType();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> updatesTabSearchSessionId();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong viewedStatusItemCount();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong viewedStatusRowCount();
}
