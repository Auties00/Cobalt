package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupHistoryReceiverUserJourneyActionType")
@WamEnum
public enum GroupHistoryReceiverUserJourneyActionType {
    @WamEnumConstant(0) GROUP_HISTORY_MESSAGE_RECEIVED,
    @WamEnumConstant(1) GROUP_HISTORY_DOWNLOAD_STARTED,
    @WamEnumConstant(2) GROUP_HISTORY_DOWNLOAD_SUCCEEDED,
    @WamEnumConstant(3) GROUP_HISTORY_DOWNLOAD_FAILED,
    @WamEnumConstant(4) GROUP_HISTORY_PARSE_HISTORY_PROTO_FAILED,
    @WamEnumConstant(5) GROUP_HISTORY_PARSE_HISTORY_PROTO_SUCCEEDED,
    @WamEnumConstant(6) GROUP_HISTORY_DB_INSERTED,
    @WamEnumConstant(7) GROUP_HISTORY_VIEW_BUTTON_CLICKED,
    @WamEnumConstant(8) GROUP_HISTORY_DOWNLOAD_BUTTON_CLICKED,
    @WamEnumConstant(9) GROUP_HISTORY_DOWNLOAD_EXPIRED,
    @WamEnumConstant(10) GROUP_HISTORY_FOOTER_FLOATING_SHOWN,
    @WamEnumConstant(11) GROUP_HISTORY_FOOTER_INLINE_SHOWN
}
