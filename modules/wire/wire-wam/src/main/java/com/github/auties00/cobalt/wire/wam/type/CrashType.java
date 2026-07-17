package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCrashType")
@WamEnum
public enum CrashType {
    @WamEnumConstant(0) CRASH,
    @WamEnumConstant(1) OOM,
    @WamEnumConstant(15) MAIN_THREAD_STALL_3_SECONDS,
    @WamEnumConstant(21) MAIN_THREAD_STALL_5_SECONDS,
    @WamEnumConstant(2) MAIN_THREAD_STALL_30_SECONDS,
    @WamEnumConstant(16) MAIN_THREAD_STALL_60_SECONDS,
    @WamEnumConstant(17) MAIN_THREAD_STALL_POKED,
    @WamEnumConstant(3) MAIN_THREAD_STALL_DEBUG,
    @WamEnumConstant(4) AUDIO_TRANSCODING_ERROR,
    @WamEnumConstant(5) VIDEO_TRANSCODING_ERROR,
    @WamEnumConstant(6) ANR,
    @WamEnumConstant(7) CRITICAL_EVENT,
    @WamEnumConstant(8) UNHANDLED_EXCEPTION,
    @WamEnumConstant(9) APP_CRASH,
    @WamEnumConstant(10) NATIVE_CRASH,
    @WamEnumConstant(11) UFAD,
    @WamEnumConstant(13) UX_BREAKING_EXCEPTION,
    @WamEnumConstant(14) UX_GRACEFUL_RECOVERY_EXCEPTION,
    @WamEnumConstant(18) WEB_UI_COMPONENT_FAILURE,
    @WamEnumConstant(19) WEB_BROKEN_USER_EXPERIENCE,
    @WamEnumConstant(20) WEB_FORCED_LOGOUT,
    @WamEnumConstant(22) THREADPOOL_NOT_RESPONDING,
    @WamEnumConstant(23) DISPATCHER_NOT_RESPONDING,
    @WamEnumConstant(24) MS_CRASH,
    @WamEnumConstant(25) MAIN_THREAD_STALL
}
