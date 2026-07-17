package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMlUndershootPytorchEdgeLibLoadErrorCode")
@WamEnum
public enum MlUndershootPytorchEdgeLibLoadErrorCode {
    @WamEnumConstant(0) PT_LOAD_ERROR_NONE,
    @WamEnumConstant(1) PT_LOAD_ERROR_DLOPEN_FAILED,
    @WamEnumConstant(2) PT_LOAD_ERROR_DLSYM_FAILED,
    @WamEnumConstant(3) PT_LOAD_ERROR_SYMBOL_DEREFERENCE_FAILED,
    @WamEnumConstant(4) PT_LOAD_ERROR_INVALID_FRAMEWORK_TYPE,
    @WamEnumConstant(5) PT_LOAD_ERROR_ET_LOAD_METHOD_FAILED
}
