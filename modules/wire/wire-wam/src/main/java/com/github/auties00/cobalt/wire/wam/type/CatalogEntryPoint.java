package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCatalogEntryPoint")
@WamEnum
public enum CatalogEntryPoint {
    @WamEnumConstant(1) CATALOG_ENTRY_POINT_PROFILE,
    @WamEnumConstant(2) CATALOG_ENTRY_POINT_SETTINGS,
    @WamEnumConstant(3) CATALOG_ENTRY_POINT_MESSAGE,
    @WamEnumConstant(4) CATALOG_ENTRY_POINT_ATTACHMENT_PANEL,
    @WamEnumConstant(5) CATALOG_ENTRY_POINT_NUX,
    @WamEnumConstant(6) CATALOG_ENTRY_POINT_DEEPLINK,
    @WamEnumConstant(7) CATALOG_ENTRY_POINT_CHAT,
    @WamEnumConstant(8) CATALOG_ENTRY_POINT_ORDER_MESSAGE,
    @WamEnumConstant(9) CATALOG_ENTRY_POINT_INVOICE_ATTACHMENT_PANEL,
    @WamEnumConstant(10) CATALOG_ENTRY_POINT_PRODUCT_LIST_MESSAGE,
    @WamEnumConstant(11) CATALOG_ENTRY_POINT_TOP_BAR,
    @WamEnumConstant(12) CATALOG_ENTRY_POINT_PROFILE_ITEM_CLICK,
    @WamEnumConstant(13) CATALOG_ENTRY_POINT_PROFILE_SEE_ALL,
    @WamEnumConstant(14) CATALOG_ENTRY_POINT_QUOTED_PRODUCT,
    @WamEnumConstant(15) CATALOG_ENTRY_POINT_CATALOG_MESSAGE,
    @WamEnumConstant(16) CATALOG_ENTRY_QBM,
    @WamEnumConstant(17) CATALOG_ENTRY_POINT_ADS_DEEPLINK,
    @WamEnumConstant(18) CATALOG_ENTRY_POINT_WA_PAGES,
    @WamEnumConstant(19) CATALOG_ENTRY_POINT_DIRECTORY_BARE_TIPS,
    @WamEnumConstant(20) CATALOG_ENTRY_POINT_BIZ_ACTION_BAR,
    @WamEnumConstant(21) CATALOG_ENTRY_POINT_BIZ_ONBOARDING,
    @WamEnumConstant(22) CATALOG_ENTRY_POINT_TRUST_CARD,
    @WamEnumConstant(23) CATALOG_ENTRY_POINT_FLOWS,
    @WamEnumConstant(24) CATALOG_ENTRY_POINT_BIZ_AI_HOME,
    @WamEnumConstant(25) CATALOG_ENTRY_POINT_BUSINESS_HOME,
    @WamEnumConstant(26) CATALOG_ENTRY_POINT_LEARNING_HUB
}
