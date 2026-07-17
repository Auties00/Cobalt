package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.LabelOperations;
import com.github.auties00.cobalt.wire.wam.type.LabelTargets;
import com.github.auties00.cobalt.wire.wam.type.LastMessageDirection;
import com.github.auties00.cobalt.wire.wam.type.ListType;
import com.github.auties00.cobalt.wire.wam.type.SmbListFeatureNameType;
import com.github.auties00.cobalt.wire.wam.type.SmbListSurfaceType;
import com.github.auties00.cobalt.wire.wam.type.UpdateEntryPoint;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebSmbListEventWamEvent")
@WamEvent(id = 7102)
public interface SmbListEventEvent extends WamEventSpec {
    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> bulkLabeling();

    @WamProperty(index = 13, type = WamType.STRING)
    Optional<String> currentListState();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> customListTitle();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> entryPointConversionSource();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> extraAttributes();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<LabelOperations> labelOperation();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<LabelTargets> labelTarget();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<LastMessageDirection> lastMessageDirection();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong listId();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong listIndex();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<ListType> listType();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> listsApplied();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> listsRemoved();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong messageDepth();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong predefinedId();

    @WamProperty(index = 20, type = WamType.ENUM)
    Optional<SmbListFeatureNameType> smbListFeatureName();

    @WamProperty(index = 21, type = WamType.ENUM)
    Optional<SmbListSurfaceType> smbListSurface();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> threadIdHmac();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<UpdateEntryPoint> updateEntryPoint();

    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> userActionTarget();
}
