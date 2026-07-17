package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.GroupExitExperienceOrigin;
import com.github.auties00.cobalt.wire.wam.type.PsGroupExitExperienceDeleteConfirmationDialogActions;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebPsGroupExitExperienceExitDeleteConfirmationDialogUiInteractionWamEvent")
@WamEvent(id = 6316, channel = WamChannel.PRIVATE, privateStatsId = 152546501)
public interface PsGroupExitExperienceExitDeleteConfirmationDialogUiInteractionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<PsGroupExitExperienceDeleteConfirmationDialogActions> psGroupExitExperienceDeleteConfirmationDialogAction();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> psGroupExitExperienceGroupJid();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<GroupExitExperienceOrigin> psGroupExitExperienceTouchPoint();
}
