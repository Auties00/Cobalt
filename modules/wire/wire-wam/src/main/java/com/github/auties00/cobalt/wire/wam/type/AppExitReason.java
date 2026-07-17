package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAppExitReason")
@WamEnum
public enum AppExitReason {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) EXIT_SELF,
    @WamEnumConstant(2) SIGNALED,
    @WamEnumConstant(3) LOW_MEMORY,
    @WamEnumConstant(4) CRASH,
    @WamEnumConstant(5) CRASH_NATIVE,
    @WamEnumConstant(6) ANR,
    @WamEnumConstant(7) INITIALIZATION_FAILURE,
    @WamEnumConstant(8) PERMISSION_CHANGE,
    @WamEnumConstant(9) EXCESSIVE_RESOURCE_USAGE,
    @WamEnumConstant(10) USER_REQUESTED,
    @WamEnumConstant(11) USER_STOPPED,
    @WamEnumConstant(12) DEPENDENCY_DIED,
    @WamEnumConstant(13) OTHER,
    @WamEnumConstant(14) FREEZER,
    @WamEnumConstant(15) PACKAGE_STATE_CHANGE,
    @WamEnumConstant(16) PACKAGE_UPDATED,
    @WamEnumConstant(200) ANDROID_NEW_VALUES
}
