package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPsGroupExitExperienceExitDialogActions")
@WamEnum
public enum PsGroupExitExperienceExitDialogActions {
    @WamEnumConstant(0) GROUP_EXIT_EXPERIENCE_DIALOG_OPEN,
    @WamEnumConstant(1) GROUP_EXIT_EXPERIENCE_DIALOG_OLD_EXIT_TAPPED,
    @WamEnumConstant(2) GROUP_EXIT_EXPERIENCE_DIALOG_OLD_EXIT_AND_REPORT_TAPPED,
    @WamEnumConstant(3) GROUP_EXIT_EXPERIENCE_DIALOG_OLD_ARCHIVE_TAPPED,
    @WamEnumConstant(4) GROUP_EXIT_EXPERIENCE_DIALOG_NEW_EXIT_TAPPED,
    @WamEnumConstant(5) GROUP_EXIT_EXPERIENCE_DIALOG_NEW_EXIT_AND_DELETE_TAPPED,
    @WamEnumConstant(6) GROUP_EXIT_EXPERIENCE_DIALOG_NEW_LEARN_MORE_TAPPED,
    @WamEnumConstant(7) GROUP_EXIT_EXPERIENCE_DIALOG_CANCELLED
}
