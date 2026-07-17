package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPsGroupExitExperienceActions")
@WamEnum
public enum PsGroupExitExperienceActions {
    @WamEnumConstant(0) GROUP_DELETED,
    @WamEnumConstant(1) GROUP_EXITED
}
