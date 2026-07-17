package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLabelSyncResultType")
@WamEnum
public enum LabelSyncResultType {
    @WamEnumConstant(1) SUCCESS,
    @WamEnumConstant(2) SKIP_PENDING_NEWER,
    @WamEnumConstant(3) FAILED_INVALID_LABEL_ID,
    @WamEnumConstant(4) FAILED_DEPS_MISSING,
    @WamEnumConstant(5) FAILED_DB_UPDATE,
    @WamEnumConstant(6) FAILED_LABEL_NOT_FOUND,
    @WamEnumConstant(7) FAILED_LABEL_STILL_MISSING,
    @WamEnumConstant(8) FAILED_INVALID_JID,
    @WamEnumConstant(9) FAILED_MISSING_ACTION,
    @WamEnumConstant(10) FAILED_ORPHANED,
    @WamEnumConstant(11) SKIP_STATIC_TYPE_EXISTS,
    @WamEnumConstant(12) SKIP_FEATURE_DISABLED,
    @WamEnumConstant(13) SKIP_EMPTY_LIST
}
