package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPsGroupExitExperienceDeleteConfirmationDialogActions")
@WamEnum
public enum PsGroupExitExperienceDeleteConfirmationDialogActions {
    @WamEnumConstant(0) GROUP_EXIT_EXPERIENCE_DELETE_CONFIRMATION_DIALOG_DELETE_TAPPED,
    @WamEnumConstant(1) GROUP_EXIT_EXPERIENCE_DELETE_CONFIRMATION_DIALOG_CANCELLED
}
