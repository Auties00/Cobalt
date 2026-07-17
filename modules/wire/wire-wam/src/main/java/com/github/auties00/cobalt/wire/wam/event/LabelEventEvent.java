package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.LabelOperations;
import com.github.auties00.cobalt.wire.wam.type.LabelTargets;
import com.github.auties00.cobalt.wire.wam.type.LastMessageDirection;
import com.github.auties00.cobalt.wire.wam.type.SmbListFeatureNameType;
import com.github.auties00.cobalt.wire.wam.type.SmbListSurfaceType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebLabelEventWamEvent")
@WamEvent(id = 1422)
public interface LabelEventEvent extends WamEventSpec {
    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> customLabelTitle();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> entryPointConversionSource();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> extraAttributes();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong itemsLabeledCount();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong labelCount();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<LabelOperations> labelOperation();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> labelOperationEntryPoint();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<LabelTargets> labelTarget();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<LastMessageDirection> lastMessageDirection();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong messageDepth();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong predefinedLabelNumber();

    @WamProperty(index = 19, type = WamType.ENUM)
    Optional<SmbListFeatureNameType> smbListFeatureName();

    @WamProperty(index = 20, type = WamType.ENUM)
    Optional<SmbListSurfaceType> smbListSurface();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> threadCreationDate();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> threadId();

    @WamProperty(index = 13, type = WamType.STRING)
    Optional<String> threadIdHmac();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> userActionTarget();
}
