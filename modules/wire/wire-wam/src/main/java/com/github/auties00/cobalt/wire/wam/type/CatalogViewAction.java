package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCatalogViewAction")
@WamEnum
public enum CatalogViewAction {
    @WamEnumConstant(2) ACTION_CARD_ITEM_CLICK,
    @WamEnumConstant(3) ACTION_CARD_MORE_CLICK,
    @WamEnumConstant(4) ACTION_LIST_IMPRESSION,
    @WamEnumConstant(6) ACTION_DETAIL_IMAGE_CLICK,
    @WamEnumConstant(7) ACTION_DETAIL_LINK_CLICK,
    @WamEnumConstant(11) ACTION_FULL_IMAGE_SWIPE,
    @WamEnumConstant(12) ACTION_DETAIL_IMPRESSION,
    @WamEnumConstant(13) ACTION_REPORT_PRODUCT,
    @WamEnumConstant(14) ACTION_SEND_PRODUCT_MESSAGE,
    @WamEnumConstant(15) ACTION_REPORT_PRODUCT_SUCCESS,
    @WamEnumConstant(16) ACTION_REPORT_PRODUCT_FAILURE,
    @WamEnumConstant(17) ACTION_QUOTED_PRODUCT_IN_CONVERSATION_CLICK,
    @WamEnumConstant(18) ACTION_PRODUCT_IN_CONVERSATION_CLICK,
    @WamEnumConstant(19) ACTION_SHARE_CATALOG_LINK_CLICK,
    @WamEnumConstant(20) ACTION_SHARE_PRODUCT_LINK_CLICK,
    @WamEnumConstant(21) ACTION_CATALOG_IN_CONVERSATION_CLICK,
    @WamEnumConstant(22) ACTION_SHARE_CATALOG_VIA_WA_LINK_CLICK,
    @WamEnumConstant(23) ACTION_SHARE_PRODUCT_VIA_WA_LINK_CLICK,
    @WamEnumConstant(24) ACTION_SHARE_CATALOG_COPY_LINK_CLICK,
    @WamEnumConstant(25) ACTION_SHARE_PRODUCT_COPY_LINK_CLICK,
    @WamEnumConstant(26) ACTION_CHAT_CATALOG_ICON_CLICK,
    @WamEnumConstant(27) ACTION_MESSAGE_BUSINESS_BUTTON_CLICK,
    @WamEnumConstant(28) ACTION_CART_ADD_PRODUCT,
    @WamEnumConstant(29) ACTION_CART_EDIT_PRODUCT,
    @WamEnumConstant(30) ACTION_CART_DELETE_PRODUCT,
    @WamEnumConstant(31) ACTION_CART_ABANDON,
    @WamEnumConstant(32) ACTION_CART_ICON_CLICK,
    @WamEnumConstant(33) ACTION_CART_SNACKBAR_CLICK,
    @WamEnumConstant(34) ACTION_SEND_ORDER_MESSAGE,
    @WamEnumConstant(35) ACTION_ORDER_LIST_IMPRESSION,
    @WamEnumConstant(36) ACTION_QUOTED_PRODUCT_MESSAGE_SEND,
    @WamEnumConstant(37) ACTION_CART_LIST_IMPRESSION,
    @WamEnumConstant(38) ACTION_ORDER_MESSAGE_CLICK,
    @WamEnumConstant(39) ACTION_ORDER_LIST_ITEM_CLICK,
    @WamEnumConstant(40) ACTION_PLM_CART_CTA_CLICK,
    @WamEnumConstant(42) ACTION_CART_CHAT_ICON_CLICK,
    @WamEnumConstant(43) ACTION_CREATE_CART,
    @WamEnumConstant(44) ACTION_PLP_PRODUCT_VARIANT_BOTTOM_SHEET_OPEN,
    @WamEnumConstant(45) ACTION_PLP_PRODUCT_VARIANT_CHANGE,
    @WamEnumConstant(46) ACTION_PDP_PRODUCT_VARIANT_CHANGE,
    @WamEnumConstant(47) ACTION_PLP_BOTTOM_SHEET_SEE_MORE_DETAILS,
    @WamEnumConstant(48) ACTION_UPDATE_VARIANT_SELECTION,
    @WamEnumConstant(49) ACTION_VIEW_PRODUCT_VARIANTS_BOTTOM_SHEET,
    @WamEnumConstant(50) ACTION_WATCH_PRODUCT_VIDEO,
    @WamEnumConstant(51) ACTION_DETAIL_VIDEO_CLICK,
    @WamEnumConstant(52) ACTION_FULL_VIDEO_SWIPE,
    @WamEnumConstant(53) ACTION_LIST_LOAD,
    @WamEnumConstant(54) ACTION_DETAIL_LOAD
}
