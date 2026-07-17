package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ActionThreadTypeType;
import com.github.auties00.cobalt.wire.wam.type.AttachmentTrayActionTargetType;
import com.github.auties00.cobalt.wire.wam.type.AttachmentTrayActionType;
import com.github.auties00.cobalt.wire.wam.type.ClientGroupSizeBucket;
import com.github.auties00.cobalt.wire.wam.type.SendMediaTypeType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebAttachmentTrayActionsWamEvent")
@WamEvent(id = 3980)
public interface AttachmentTrayActionsEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong actionDurationMs();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<ActionThreadTypeType> actionThreadType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<AttachmentTrayActionType> attachmentTrayAction();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<AttachmentTrayActionTargetType> attachmentTrayActionTarget();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<ClientGroupSizeBucket> groupSizeBucket();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> isSuccessful();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<SendMediaTypeType> sendMediaType();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong sendTime();
}
