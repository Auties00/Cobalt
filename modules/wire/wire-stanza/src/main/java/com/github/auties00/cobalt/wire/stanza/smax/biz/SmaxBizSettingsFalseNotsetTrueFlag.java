package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.BusinessDataSharingConsent;

import java.util.Optional;

/**
 * Validates the tri-state SMB data-sharing consent attribute carried on business-settings stanzas.
 * <p>
 * Accepts the lowercase wire literals {@code "false"}, {@code "notset"}, and {@code "true"} carried
 * on the {@code value} attribute of the {@code <smb_data_sharing_with_meta_consent>} grandchild of
 * the {@code <privacy>} body in {@link SmaxGetPrivacySettingResponse.Success},
 * {@link SmaxSetPrivacySettingResponse.Success}, and
 * {@link SmaxSyncPrivacySettingResponse.Notification}. The parsed value gates the CTWA share-data-
 * with-Meta upsell modal and the per-customer data-sharing controls; {@link #NOTSET} surfaces the
 * consent dialog proactively.
 *
 * @implNote This implementation carries the model-side {@link BusinessDataSharingConsent} as a
 * bridge field so callers translating between the wire form parsed here and the protobuf form used
 * by the higher-level business-privacy surface can map without a second lookup. The two enums are
 * kept separate because the {@code modules/model} submodule does not carry source provenance
 * annotations per the lib-only rule.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizSettingsEnums")
@WhatsAppWebExport(
        moduleName = "WASmaxInBizSettingsEnums",
        exports = "ENUM_FALSE_NOTSET_TRUE",
        adaptation = WhatsAppAdaptation.ADAPTED
)
public enum SmaxBizSettingsFalseNotsetTrueFlag {
    /**
     * The wire literal {@code "false"}.
     * <p>
     * The user has explicitly declined to share SMB data with Meta.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizSettingsEnums",
            exports = "ENUM_FALSE_NOTSET_TRUE",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    FALSE("false", BusinessDataSharingConsent.FALSE),

    /**
     * The wire literal {@code "notset"}.
     * <p>
     * The user has not yet been prompted to make a choice; the consent dialog is surfaced
     * proactively whenever this value is observed.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizSettingsEnums",
            exports = "ENUM_FALSE_NOTSET_TRUE",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    NOTSET("notset", BusinessDataSharingConsent.NOT_SET),

    /**
     * The wire literal {@code "true"}.
     * <p>
     * The user has explicitly granted consent to share SMB data with Meta; downstream surfaces
     * unlock the per-customer data-sharing controls.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizSettingsEnums",
            exports = "ENUM_FALSE_NOTSET_TRUE",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    TRUE("true", BusinessDataSharingConsent.TRUE);

    /**
     * The wire literal carried on the {@code value} attribute.
     */
    private final String wireValue;

    /**
     * The matching model-side {@link BusinessDataSharingConsent} constant.
     */
    private final BusinessDataSharingConsent consent;

    /**
     * Constructs a constant binding a wire literal to its model-side companion.
     *
     * @param wireValue the wire literal; never {@code null}
     * @param consent   the matching model-side consent; never {@code null}
     */
    SmaxBizSettingsFalseNotsetTrueFlag(String wireValue, BusinessDataSharingConsent consent) {
        this.wireValue = wireValue;
        this.consent = consent;
    }

    /**
     * Returns the wire literal carried on the {@code value} attribute.
     * <p>
     * Round-trips with {@link #of(String)}.
     *
     * @return the wire literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Returns the matching model-side {@link BusinessDataSharingConsent} constant.
     * <p>
     * Bridges between this lib-side wire-validator and the model-side protobuf-wired consent enum.
     *
     * @return the model-side consent; never {@code null}
     */
    public BusinessDataSharingConsent consent() {
        return consent;
    }

    /**
     * Parses a wire-form attribute string into the matching enum constant.
     * <p>
     * Any value other than the three documented lowercase literals yields empty, which aborts the
     * surrounding stanza parse.
     *
     * @implNote This implementation iterates the constants comparing {@link #wireValue} rather than
     * using {@link Enum#valueOf(Class, String)} because the wire literal {@code "notset"} does not
     * match the Java constant name {@code NOTSET} under a case-insensitive normalisation step. A
     * field-by-field iteration keeps the mapping explicit and avoids relying on locale-sensitive
     * case normalisation.
     *
     * @param value the attribute value; may be {@code null}
     * @return an {@link Optional} carrying the matching constant, or empty when {@code value} is
     *         {@code null} or does not match
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizSettingsEnums",
            exports = "ENUM_FALSE_NOTSET_TRUE",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public static Optional<SmaxBizSettingsFalseNotsetTrueFlag> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (var flag : values()) {
            if (flag.wireValue.equals(value)) {
                return Optional.of(flag);
            }
        }
        return Optional.empty();
    }
}
