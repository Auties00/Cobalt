package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBootstrapAppStateDataStageCode")
@WamEnum
public enum BootstrapAppStateDataStageCode {
    @WamEnumConstant(1) REQUEST_BUILT,
    @WamEnumConstant(2) RESPONSE_RECEIVED,
    @WamEnumConstant(3) RESPONSE_PARSED_VALID,
    @WamEnumConstant(4) MISSING_KEYS_REQUESTED,
    @WamEnumConstant(5) MISSING_KEYS_RECEIVED,
    @WamEnumConstant(6) MUTATIONS_DECRYPTED,
    @WamEnumConstant(7) ABOUT_TO_APPLY_MUTATIONS,
    @WamEnumConstant(8) APPLIED_MUTATIONS,
    @WamEnumConstant(9) PUSHNAME_APPLIED,
    @WamEnumConstant(10) PUSHNAME_INVALID,
    @WamEnumConstant(11) ENTERED_RETRY_MODE
}
