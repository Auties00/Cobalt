package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDefenseModeQuarantineAction")
@WamEnum
public enum DefenseModeQuarantineAction {
    @WamEnumConstant(0) QUARANTINED_MSG,
    @WamEnumConstant(1) QUARANTINE_RESTORE_CLICK,
    @WamEnumConstant(2) QUARANTINE_RESTORE_DISMISS,
    @WamEnumConstant(3) QUARANTINE_RESTORE_CONFIRM,
    @WamEnumConstant(4) QUARANTINE_RESTORE_AUTO,
    @WamEnumConstant(5) QUARANTINE_RESTORE_SUCCESS,
    @WamEnumConstant(6) QUARANTINE_RESTORE_FAILED
}
