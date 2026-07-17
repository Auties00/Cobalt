package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumShopsManagementAction")
@WamEnum
public enum ShopsManagementAction {
    @WamEnumConstant(1) ACTION_CLICK_SHOPS_SETTING,
    @WamEnumConstant(2) ACTION_CLICK_CATALOG_SETTING,
    @WamEnumConstant(3) ACTION_CLICK_COMMERCE_MANAGER_IN_CATALOG_SETTING,
    @WamEnumConstant(4) ACTION_CLICK_CANCEL_IN_CATALOG_SETTING,
    @WamEnumConstant(5) ACTION_CLICK_VIEW_SHOPS_IN_SHOPS_SETTING,
    @WamEnumConstant(6) ACTION_CLICK_COMMERCE_MANAGER_IN_SHOPS_SETTING,
    @WamEnumConstant(7) ACTION_CLICK_CANCEL_IN_SHOPS_SETTING,
    @WamEnumConstant(8) ACTION_SHOPS_PRODUCT_PREVIEW_VISIBLE,
    @WamEnumConstant(9) ACTION_CLICK_SHOPS_PRODUCT_PREVIEW_TILE,
    @WamEnumConstant(10) ACTION_CLICK_MANAGE_SHOPS,
    @WamEnumConstant(11) ACTION_SHARE_SHOPS,
    @WamEnumConstant(12) ACTION_CLICK_VIEW_SHOPS_FROM_EDIT_BIZ_PROFILE,
    @WamEnumConstant(13) ACTION_CLICK_COMMERCE_MANAGER_FROM_EDIT_BIZ_PROFILE,
    @WamEnumConstant(14) ACTION_CLICK_CANCEL_FROM_EDIT_BIZ_PROFILE
}
