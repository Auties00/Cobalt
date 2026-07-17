package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPttMessageUserJourneyFailureReason")
@WamEnum
public enum PttMessageUserJourneyFailureReason {
    @WamEnumConstant(1) FAIL_OTHER,
    @WamEnumConstant(2) FAIL_CREATE_RECORDER,
    @WamEnumConstant(3) FAIL_PREPARE_RECORDER,
    @WamEnumConstant(4) FAIL_START_RECORDER,
    @WamEnumConstant(5) FAIL_RESUME_RECORDER,
    @WamEnumConstant(6) FAIL_MIC_PERMISSION,
    @WamEnumConstant(7) FAIL_STORAGE_READ_WRITE_PERMISSION,
    @WamEnumConstant(8) FAIL_MINIMUM_STORAGE_REQUIREMENT,
    @WamEnumConstant(9) FAIL_PAUSE_RECORDER,
    @WamEnumConstant(10) FAIL_MOVE_PTT_FILE_TO_PERMANENT_STORAGE_ON_SEND,
    @WamEnumConstant(11) FAIL_STORE_PTT_FILE_IN_DRAFT_CACHE,
    @WamEnumConstant(12) FAIL_RECORDER_STOP,
    @WamEnumConstant(13) FAIL_DRAFT_PLAYBACK_START,
    @WamEnumConstant(14) FAIL_DRAFT_PLAYBACK_SEEK,
    @WamEnumConstant(15) FAIL_MIC_AND_STORAGE_READ_WRITE_PERMISSION
}
