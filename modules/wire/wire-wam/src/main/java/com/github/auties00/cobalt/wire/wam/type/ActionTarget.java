package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumActionTarget")
@WamEnum
public enum ActionTarget {
    @WamEnumConstant(0) REACH_TAB,
    @WamEnumConstant(1) GROWTH_TAB,
    @WamEnumConstant(2) FOLLOWERS_TAB,
    @WamEnumConstant(3) ACCOUNTS_REACHED_INFO_ICON,
    @WamEnumConstant(4) TOP_REGIONS_REACH_INFO_ICON,
    @WamEnumConstant(5) GROWTH_CHART_INFO_ICON,
    @WamEnumConstant(6) TOP_REGIONS_FOLLOWERS_INFO_ICON,
    @WamEnumConstant(7) HELP_CENTER_DATA_UNAVAILABLE_ARTICLE,
    @WamEnumConstant(8) HELP_CENTER_CHANNEL_METRICS_ARTICLE,
    @WamEnumConstant(9) WIDGET_INFO_ICON,
    @WamEnumConstant(10) TOTAL_FOLLOWERS_INFO_ICON
}
