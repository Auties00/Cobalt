package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOpusAction")
@WamEnum
public enum OpusAction {
    @WamEnumConstant(0) OPUS_NOT_WORKING,
    @WamEnumConstant(1) OPUS_JOB_RUN,
    @WamEnumConstant(2) OPUS_FALLBACK_RUN,
    @WamEnumConstant(3) OPUS_MESSAGE_CLICKED,
    @WamEnumConstant(4) OPUS_JOB_FAILED,
    @WamEnumConstant(5) OPUS_FALLBACK_FAILED,
    @WamEnumConstant(6) OPUS_HOOK_FAILED
}
