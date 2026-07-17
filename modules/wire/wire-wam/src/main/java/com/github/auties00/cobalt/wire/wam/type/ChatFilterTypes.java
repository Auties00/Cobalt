package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChatFilterTypes")
@WamEnum
public enum ChatFilterTypes {
    @WamEnumConstant(0) NONE,
    @WamEnumConstant(1) OTHER_LABELS,
    @WamEnumConstant(2) UNREAD,
    @WamEnumConstant(3) GROUP,
    @WamEnumConstant(4) BROADCAST_LIST,
    @WamEnumConstant(5) NEW_CUSTOMER,
    @WamEnumConstant(6) NEW_ORDER,
    @WamEnumConstant(7) PENDING_PAYMENT,
    @WamEnumConstant(8) PAID,
    @WamEnumConstant(9) ORDER_COMPLETE,
    @WamEnumConstant(10) CONTACT,
    @WamEnumConstant(11) NON_CONTACT,
    @WamEnumConstant(12) PHOTOS,
    @WamEnumConstant(13) GIFS,
    @WamEnumConstant(14) LINKS,
    @WamEnumConstant(15) VIDEOS,
    @WamEnumConstant(16) DOCUMENTS,
    @WamEnumConstant(17) AUDIOS,
    @WamEnumConstant(18) ASSIGNED_TO_YOU,
    @WamEnumConstant(19) PERSONAL,
    @WamEnumConstant(20) BUSINESS,
    @WamEnumConstant(21) LABEL,
    @WamEnumConstant(22) FAVORITES,
    @WamEnumConstant(23) STICKERS,
    @WamEnumConstant(24) COMMUNITY,
    @WamEnumConstant(25) BUSINESS_AI,
    @WamEnumConstant(26) DRAFTS,
    @WamEnumConstant(27) CAMPAIGN_REPLIES,
    @WamEnumConstant(28) BUSINESS_FOLDER
}
