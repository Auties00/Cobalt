package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPollActionType")
@WamEnum
public enum PollActionType {
    @WamEnumConstant(1) OPEN_CREATE_MODAL,
    @WamEnumConstant(2) CREATE_POLL,
    @WamEnumConstant(4) VIEW_RESULTS_MODAL,
    @WamEnumConstant(5) REMOVE_VOTE,
    @WamEnumConstant(6) VOTE,
    @WamEnumConstant(7) CHANGE_VOTE,
    @WamEnumConstant(8) EDIT_POLL_INITIATED,
    @WamEnumConstant(9) EDIT_POLL_COMPLETED
}
