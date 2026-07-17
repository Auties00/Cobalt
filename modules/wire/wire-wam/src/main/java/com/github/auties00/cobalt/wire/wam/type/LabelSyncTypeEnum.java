package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLabelSyncTypeEnum")
@WamEnum
public enum LabelSyncTypeEnum {
    @WamEnumConstant(1) LABEL_JID,
    @WamEnumConstant(2) LABEL_EDIT,
    @WamEnumConstant(3) LABEL_REORDER
}
