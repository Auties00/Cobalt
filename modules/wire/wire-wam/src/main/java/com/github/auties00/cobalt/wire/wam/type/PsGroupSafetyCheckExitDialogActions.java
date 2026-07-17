package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPsGroupSafetyCheckExitDialogActions")
@WamEnum
public enum PsGroupSafetyCheckExitDialogActions {
    @WamEnumConstant(0) GROUP_EXIT_DIALOG_DISMISS,
    @WamEnumConstant(1) GROUP_EXIT_DIALOG_EXIT,
    @WamEnumConstant(2) GROUP_EXIT_DIALOG_EXIT_AND_REPORT,
    @WamEnumConstant(3) COMMUNITY_EXIT_DIALOG_DISMISS,
    @WamEnumConstant(4) COMMUNITY_EXIT_DIALOG_EXIT,
    @WamEnumConstant(5) COMMUNITY_EXIT_DIALOG_EXIT_AND_DELETE
}
