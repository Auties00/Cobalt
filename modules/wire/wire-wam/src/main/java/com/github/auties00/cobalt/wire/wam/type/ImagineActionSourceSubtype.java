package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumImagineActionSourceSubtype")
@WamEnum
public enum ImagineActionSourceSubtype {
    @WamEnumConstant(1) IMAGINE_EDIT,
    @WamEnumConstant(2) IMAGINE_ME
}
