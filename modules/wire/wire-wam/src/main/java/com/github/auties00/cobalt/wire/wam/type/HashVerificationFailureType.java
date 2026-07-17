package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumHashVerificationFailureType")
@WamEnum
public enum HashVerificationFailureType {
    @WamEnumConstant(1) HMAC_CHUNK,
    @WamEnumConstant(2) ENCRYPTED_SHA256_MISMATCH,
    @WamEnumConstant(3) PLAINTEXT_SHA256_MISMATCH,
    @WamEnumConstant(4) ENCRYPTED_SHA256_NULL,
    @WamEnumConstant(5) PLAINTEXT_SHA256_NULL,
    @WamEnumConstant(6) DECRYPTION_FAILED,
    @WamEnumConstant(7) TRUNCATED_HMAC_VERIFICATION_FAILED
}
