package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDocumentType")
@WamEnum
public enum DocumentType {
    @WamEnumConstant(1) OTHER,
    @WamEnumConstant(2) IMAGE,
    @WamEnumConstant(3) VIDEO,
    @WamEnumConstant(4) AUDIO,
    @WamEnumConstant(5) DOCUMENT,
    @WamEnumConstant(6) COMPRESSED_FILE,
    @WamEnumConstant(7) EXECUTABLE,
    @WamEnumConstant(8) VCARD
}
