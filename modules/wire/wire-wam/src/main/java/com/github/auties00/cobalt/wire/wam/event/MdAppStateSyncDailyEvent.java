package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdAppStateSyncDailyWamEvent")
@WamEvent(id = 2300)
public interface MdAppStateSyncDailyEvent extends WamEventSpec {
    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong crossIndexConflictCount();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong invalidActionCount();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong keyRotationRemoveCount();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong missingKeyCount();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong mutationCount();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong storedMutationCount();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong unsetActionCount();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong unsupportedActionCount();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong uploadConflictCount();
}
