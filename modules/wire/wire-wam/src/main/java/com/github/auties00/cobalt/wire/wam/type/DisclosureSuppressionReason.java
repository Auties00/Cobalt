package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDisclosureSuppressionReason")
@WamEnum
public enum DisclosureSuppressionReason {
    @WamEnumConstant(0) COOLDOWN_FROM_CTWA,
    @WamEnumConstant(1) COOLDOWN_FROM_MM,
    @WamEnumConstant(2) DISCLOSED,
    @WamEnumConstant(3) OPT_OUT_TOS2016,
    @WamEnumConstant(4) NO_SHOW_MM_DISCLOSURE_FLAG,
    @WamEnumConstant(5) NOT_FROM_CAPI,
    @WamEnumConstant(6) NOT_MARKETING_MESSAGE,
    @WamEnumConstant(7) ABPROP_DISABLED,
    @WamEnumConstant(8) OPT_OUT_TOS2021,
    @WamEnumConstant(9) BODY_LINK_DISCLOSURE_AB_PROP_DISABLED,
    @WamEnumConstant(10) ANOTHER_DIALOG_DISPLAYED,
    @WamEnumConstant(11) EVENT_TYPE_AB_PROP_DISABLED,
    @WamEnumConstant(12) BLOCKED,
    @WamEnumConstant(13) ACCOUNT_LINKED
}
