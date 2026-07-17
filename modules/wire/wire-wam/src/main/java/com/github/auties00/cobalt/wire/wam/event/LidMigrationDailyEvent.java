package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.LidMigrationSourceType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebLidMigrationDailyWamEvent")
@WamEvent(id = 5842)
public interface LidMigrationDailyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> completedMigrations();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<LidMigrationSourceType> lidMigrationSource();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong numberOfChatsWithClientAssignedLid();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong numberOfDeprecatedChats();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong numberOfLidBroadcastLists();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong numberOfLidGroups();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong numberOfPnBroadcastLists();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong numberOfPnChatsWithoutMapping();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong numberOfPnGroups();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong numberOfPnhCtwaThreadsKnownMapping();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong numberOfPnhCtwaThreadsMissingMapping();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong numberOfRegularPnChats();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong numberOfSplitThreads();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong numberOfUserChatsWithoutAccountLid();
}
