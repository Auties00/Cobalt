package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSnackbarActionType")
@WamEnum
public enum SnackbarActionType {
    @WamEnumConstant(0) SNACKBAR_SHOWN,
    @WamEnumConstant(1) MESSAGE_UNDELETE
}
