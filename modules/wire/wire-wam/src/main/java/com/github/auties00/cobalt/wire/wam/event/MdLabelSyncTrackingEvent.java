package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.LabelSyncDeviceRoleType;
import com.github.auties00.cobalt.wire.wam.type.LabelSyncDirectionType;
import com.github.auties00.cobalt.wire.wam.type.LabelSyncResultType;
import com.github.auties00.cobalt.wire.wam.type.LabelSyncTypeEnum;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdLabelSyncTrackingWamEvent")
@WamEvent(id = 7638)
public interface MdLabelSyncTrackingEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<LabelSyncDeviceRoleType> labelSyncDeviceRole();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<LabelSyncDirectionType> labelSyncDirection();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> labelSyncHasPending();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> labelSyncHash();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> labelSyncIsCapiHosted();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> labelSyncIsLabeled();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong labelSyncPredefinedId();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<LabelSyncResultType> labelSyncResult();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong labelSyncTimestamp();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<LabelSyncTypeEnum> labelSyncType();
}
