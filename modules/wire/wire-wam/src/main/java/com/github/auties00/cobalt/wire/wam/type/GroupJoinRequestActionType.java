package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupJoinRequestActionType")
@WamEnum
public enum GroupJoinRequestActionType {
    @WamEnumConstant(1) MEMBERSHIP_REQUEST_CREATE,
    @WamEnumConstant(2) VIEW_PENDING_PARTICIPANTS,
    @WamEnumConstant(3) MEMBERSHIP_REQUEST_APPROVAL_MODE_ON,
    @WamEnumConstant(4) MEMBERSHIP_REQUEST_APPROVAL_MODE_OFF,
    @WamEnumConstant(5) MEMBERSHIP_REQUEST_APPROVE,
    @WamEnumConstant(6) MEMBERSHIP_REQUEST_REJECT,
    @WamEnumConstant(7) MEMBERSHIP_REQUEST_CANCEL,
    @WamEnumConstant(8) DISMISS_PENDING_PARTICIPANTS_BANNER
}
