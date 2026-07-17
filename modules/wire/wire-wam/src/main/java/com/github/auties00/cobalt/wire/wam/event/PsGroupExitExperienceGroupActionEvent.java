package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.GroupExitExperienceOrigin;
import com.github.auties00.cobalt.wire.wam.type.PsGroupExitExperienceActions;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebPsGroupExitExperienceGroupActionWamEvent")
@WamEvent(id = 6332, channel = WamChannel.PRIVATE, privateStatsId = 152546501)
public interface PsGroupExitExperienceGroupActionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<PsGroupExitExperienceActions> psGroupExitExperienceAction();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> psGroupExitExperienceEnabled();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> psGroupExitExperienceGroupJid();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<GroupExitExperienceOrigin> psGroupExitExperienceTouchPoint();
}
