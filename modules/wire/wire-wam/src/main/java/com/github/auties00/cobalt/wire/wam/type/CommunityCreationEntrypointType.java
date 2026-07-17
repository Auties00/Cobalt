package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCommunityCreationEntrypointType")
@WamEnum
public enum CommunityCreationEntrypointType {
    @WamEnumConstant(1) CHATS_TAB,
    @WamEnumConstant(2) COMMUNITIES_TAB,
    @WamEnumConstant(3) GROUP_INFO,
    @WamEnumConstant(4) DEEP_LINK,
    @WamEnumConstant(5) DEEP_LINK_BANNER,
    @WamEnumConstant(6) DEEP_LINK_PSA,
    @WamEnumConstant(7) DEEP_LINK_CHAT,
    @WamEnumConstant(8) DEEP_LINK_CHANNEL,
    @WamEnumConstant(9) COMMUNITY_FILTER,
    @WamEnumConstant(10) OVERFLOW_MENU
}
