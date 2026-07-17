package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMmUserControlsAction")
@WamEnum
public enum MmUserControlsAction {
    @WamEnumConstant(1) INTERESTED,
    @WamEnumConstant(2) NOT_INTERESTED,
    @WamEnumConstant(3) STOP,
    @WamEnumConstant(4) BLOCK,
    @WamEnumConstant(5) BLOCK_AND_REPORT,
    @WamEnumConstant(6) RESUME,
    @WamEnumConstant(7) DISMISS,
    @WamEnumConstant(8) UNDO,
    @WamEnumConstant(9) IMPRESSION,
    @WamEnumConstant(10) STOP_CONFIRMATION,
    @WamEnumConstant(11) RESUME_CONFIRMATION,
    @WamEnumConstant(12) FEEDBACK,
    @WamEnumConstant(13) LEARN_MORE,
    @WamEnumConstant(14) MANAGE_MESSAGES
}
