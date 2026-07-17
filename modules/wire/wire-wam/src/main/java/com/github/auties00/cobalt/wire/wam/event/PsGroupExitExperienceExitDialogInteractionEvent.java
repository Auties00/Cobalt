package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.GroupExitExperienceOrigin;
import com.github.auties00.cobalt.wire.wam.type.PsGroupExitExperienceExitDialogActions;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebPsGroupExitExperienceExitDialogInteractionWamEvent")
@WamEvent(id = 6318, channel = WamChannel.PRIVATE, privateStatsId = 152546501)
public interface PsGroupExitExperienceExitDialogInteractionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> psExitExperienceReportingEnabled();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> psGroupExitExperienceEnabled();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<PsGroupExitExperienceExitDialogActions> psGroupExitExperienceExitDialogAction();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> psGroupExitExperienceGroupJid();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<GroupExitExperienceOrigin> psGroupExitExperienceTouchPoint();
}
