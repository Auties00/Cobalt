package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumImagineActionTarget")
@WamEnum
public enum ImagineActionTarget {
    @WamEnumConstant(0) NONE,
    @WamEnumConstant(1) IMAGINE_EDIT,
    @WamEnumConstant(2) IMAGINE_ME,
    @WamEnumConstant(3) IMAGINE_FLASH,
    @WamEnumConstant(4) IMAGINE,
    @WamEnumConstant(5) MEDIA_INPUT,
    @WamEnumConstant(6) MEDIA_SHARING_EDIT,
    @WamEnumConstant(7) MEDIA_SHARING_STYLES,
    @WamEnumConstant(8) MEDIA_SHARING_INTENTS,
    @WamEnumConstant(9) MEDIA_SHARING_FILTERS
}
