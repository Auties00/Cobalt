package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOverallLastUploadRetryPhaseType")
@WamEnum
public enum OverallLastUploadRetryPhaseType {
    @WamEnumConstant(1) RESUME_CHECK,
    @WamEnumConstant(2) UPLOAD,
    @WamEnumConstant(3) FINALIZE
}
