package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumExpressPathDownloadState")
@WamEnum
public enum ExpressPathDownloadState {
    @WamEnumConstant(1) PARTIAL_OR_NONE,
    @WamEnumConstant(2) FULL,
    @WamEnumConstant(3) MEDIANOTIFY_RECEIVED_EP_DISABLED,
    @WamEnumConstant(4) SKIPPED_DIFF_POP,
    @WamEnumConstant(5) SKIPPED_AUTODOWNLOAD
}
