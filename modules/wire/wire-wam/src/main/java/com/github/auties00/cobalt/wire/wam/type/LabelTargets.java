package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLabelTargets")
@WamEnum
public enum LabelTargets {
    @WamEnumConstant(1) LABEL,
    @WamEnumConstant(2) MESSAGE,
    @WamEnumConstant(3) CONTACT,
    @WamEnumConstant(4) LABELS_SCREEN,
    @WamEnumConstant(5) LABEL_DETAILS_SCREEN,
    @WamEnumConstant(6) EDIT_LABEL_DIALOG,
    @WamEnumConstant(7) DELETE_LABEL_DIALOG,
    @WamEnumConstant(8) LABEL_MESSAGE_DIALOG,
    @WamEnumConstant(9) LABEL_CHAT_DIALOG,
    @WamEnumConstant(10) ADD_LABEL_DIALOG,
    @WamEnumConstant(11) BULK_UNLABEL_DIALOG,
    @WamEnumConstant(12) LABEL_COMBINED_DIALOG,
    @WamEnumConstant(13) GROUP,
    @WamEnumConstant(14) BROADCAST,
    @WamEnumConstant(15) NEW_LIST_SCREEN
}
