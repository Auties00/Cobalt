package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGooglePlayServicesStatus")
@WamEnum
public enum GooglePlayServicesStatus {
    @WamEnumConstant(0) SUCCESS,
    @WamEnumConstant(1) SERVICE_MISSING,
    @WamEnumConstant(2) SERVICE_VERSION_UPDATE_REQUIRED,
    @WamEnumConstant(3) SERVICE_DISABLED,
    @WamEnumConstant(4) SIGN_IN_REQUIRED,
    @WamEnumConstant(5) INVALID_ACCOUNT,
    @WamEnumConstant(6) RESOLUTION_REQUIRED,
    @WamEnumConstant(7) NETWORK_ERROR,
    @WamEnumConstant(8) INTERNAL_ERROR,
    @WamEnumConstant(9) SERVICE_INVALID,
    @WamEnumConstant(10) DEVELOPER_ERROR,
    @WamEnumConstant(11) LICENSE_CHECK_FAILED,
    @WamEnumConstant(13) CANCELED,
    @WamEnumConstant(14) TIMEOUT,
    @WamEnumConstant(15) INTERRUPTED,
    @WamEnumConstant(16) API_UNAVAILABLE,
    @WamEnumConstant(17) SIGN_IN_FAILED,
    @WamEnumConstant(18) SERVICE_UPDATING,
    @WamEnumConstant(19) SERVICE_MISSING_PERMISSION,
    @WamEnumConstant(20) RESTRICTED_PROFILE,
    @WamEnumConstant(22) RESOLUTION_ACTIVITY_NOT_FOUND,
    @WamEnumConstant(23) API_DISABLED,
    @WamEnumConstant(24) API_DISABLED_FOR_CONNECTION
}
