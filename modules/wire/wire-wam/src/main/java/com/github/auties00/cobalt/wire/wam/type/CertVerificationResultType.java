package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCertVerificationResultType")
@WamEnum
public enum CertVerificationResultType {
    @WamEnumConstant(1) SUCCESS,
    @WamEnumConstant(2) FAILED_INVALID_CERT,
    @WamEnumConstant(3) FAILED_EXPIRED_CERT,
    @WamEnumConstant(4) FAILED_CHAIN_VALIDATION,
    @WamEnumConstant(5) FAILED_SIGNATURE_INVALID,
    @WamEnumConstant(6) FAILED_UNKNOWN_ERROR,
    @WamEnumConstant(7) SKIPPED_AB_DISABLED,
    @WamEnumConstant(8) FAILED_ALGORITHM_UNSUPPORTED,
    @WamEnumConstant(9) FAILED_KEY_USAGE_INVALID,
    @WamEnumConstant(10) FAILED_EXTENDED_KEY_USAGE_INVALID,
    @WamEnumConstant(11) FAILED_SAN_INVALID,
    @WamEnumConstant(12) FAILED_CERTIFICATE_REVOKED,
    @WamEnumConstant(13) FAILED_CRL_UNAVAILABLE,
    @WamEnumConstant(14) FAILED_CRL_SIGNATURE_INVALID,
    @WamEnumConstant(15) FAILED_CRL_EXPIRED,
    @WamEnumConstant(16) FAILED_SIGNATURE_DATA_MISSING,
    @WamEnumConstant(17) FAILED_SIGNATURE_DATA_MALFORMED,
    @WamEnumConstant(18) FAILED_SIGNATURE_CRYPTO_FAILED,
    @WamEnumConstant(19) FAILED_CHAIN_INCOMPLETE,
    @WamEnumConstant(20) FAILED_CHAIN_UNTRUSTED,
    @WamEnumConstant(21) FAILED_CHAIN_CYCLIC,
    @WamEnumConstant(22) FAILED_CERT_NOT_YET_VALID,
    @WamEnumConstant(23) FAILED_TIMESTAMP_INVALID,
    @WamEnumConstant(24) FAILED_FORWARDING_DATA_MISSING,
    @WamEnumConstant(25) FAILED_FORWARDING_DATA_TAMPERED
}
