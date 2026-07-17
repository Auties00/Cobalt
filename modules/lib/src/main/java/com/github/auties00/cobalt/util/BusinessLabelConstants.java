package com.github.auties00.cobalt.util;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.wam.type.CtwaLabelType;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Holds the compile-time constants and pure lookup helpers that WhatsApp
 * Business clients use to render and classify the built-in chat labels.
 *
 * <p>The two colour palettes are the swatch sets the mobile clients paint label
 * chips with; the predefined-id and display-name constants are the eight
 * built-in labels every Business account ships with; the custom-label subtype
 * strings are the canonical wire form WhatsApp uses on the CTWA telemetry
 * channel; {@link #LABEL_NAME_MAX_LENGTH} is the name-length ceiling the mobile
 * clients enforce. The three mapping helpers are the name-to-id, subtype-to-CTWA,
 * and id-to-name lookups that label-editing and customer-management flows
 * consume.
 *
 * @implNote
 * This implementation exposes the colour palettes as
 * {@link List#copyOf(java.util.Collection)}-equivalent unmodifiable lists,
 * mirroring the {@code Object.freeze} immutability WhatsApp Web applies to the
 * underlying arrays; the lookup helpers return {@link OptionalInt} and
 * {@link Optional} where WhatsApp Web returns {@code undefined}-or-value.
 */
@WhatsAppWebModule(moduleName = "WAWebLabelConstants")
public final class BusinessLabelConstants {
    /**
     * The ordered palette of twenty hex colour swatches the Android WhatsApp
     * client uses to paint chat labels.
     *
     * <p>Indexing into this list with a
     * {@link com.github.auties00.cobalt.wire.linked.preference.Label}'s stored palette
     * slot resolves the same numeric value to the same visual colour the
     * Android client would have painted.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "ANDROID_LABEL_COLOR_PALETTE",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final List<String> ANDROID_LABEL_COLOR_PALETTE = List.of(
            "#FF9485", "#64C4FF", "#FFD429", "#DFAEF0", "#99B6C1",
            "#55CCB3", "#FF9DFF", "#D3A91D", "#6D7CCE", "#D7E752",
            "#00D0E2", "#FFC5C7", "#93CEAC", "#F74848", "#00A0F2",
            "#83E422", "#FFAF04", "#B5EBFF", "#9BA6FF", "#9368CF"
    );

    /**
     * The ordered palette of twenty hex colour swatches the iPhone WhatsApp
     * client uses to paint chat labels.
     *
     * <p>Indexing into this list with a
     * {@link com.github.auties00.cobalt.wire.linked.preference.Label}'s stored palette
     * slot resolves the same numeric value to the same visual colour the iPhone
     * client would have painted.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "IPHONE_LABEL_COLOR_PALETTE",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final List<String> IPHONE_LABEL_COLOR_PALETTE = List.of(
            "#A62C71", "#90A841", "#C1A03F", "#792138", "#AE8774",
            "#F0B330", "#B6B327", "#C69FCC", "#8B6990", "#FF8A8C",
            "#54C265", "#FF7B6B", "#26C4DC", "#57C9FF", "#74676A",
            "#7E90A3", "#5696FF", "#6E257E", "#7ACBA5", "#243640"
    );

    /**
     * The predefined-id integer assigned to the "New customer" label.
     *
     * <p>Matching this value against the {@code predefinedId} on a
     * {@link com.github.auties00.cobalt.wire.linked.preference.Label} synced from the
     * server identifies the "New customer" built-in label.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_IDS",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int PREDEFINED_LABEL_ID_NEW_CUSTOMER = 1;

    /**
     * The predefined-id integer assigned to the "New order" label.
     *
     * <p>Matching this value against the {@code predefinedId} on a
     * {@link com.github.auties00.cobalt.wire.linked.preference.Label} synced from the
     * server identifies the "New order" built-in label.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_IDS",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int PREDEFINED_LABEL_ID_NEW_ORDER = 2;

    /**
     * The predefined-id integer assigned to the "Pending payment" label.
     *
     * <p>Matching this value against the {@code predefinedId} on a
     * {@link com.github.auties00.cobalt.wire.linked.preference.Label} synced from the
     * server identifies the "Pending payment" built-in label.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_IDS",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int PREDEFINED_LABEL_ID_PENDING_PAYMENT = 3;

    /**
     * The predefined-id integer assigned to the "Paid" label.
     *
     * <p>Matching this value against the {@code predefinedId} on a
     * {@link com.github.auties00.cobalt.wire.linked.preference.Label} synced from the
     * server identifies the "Paid" built-in label.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_IDS",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int PREDEFINED_LABEL_ID_PAID = 4;

    /**
     * The predefined-id integer assigned to the "Order complete" label.
     *
     * <p>Matching this value against the {@code predefinedId} on a
     * {@link com.github.auties00.cobalt.wire.linked.preference.Label} synced from the
     * server identifies the "Order complete" built-in label.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_IDS",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int PREDEFINED_LABEL_ID_ORDER_COMPLETE = 5;

    /**
     * The predefined-id integer assigned to the "Important" label.
     *
     * <p>Matching this value against the {@code predefinedId} on a
     * {@link com.github.auties00.cobalt.wire.linked.preference.Label} synced from the
     * server identifies the "Important" built-in label.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_IDS",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int PREDEFINED_LABEL_ID_IMPORTANT = 6;

    /**
     * The predefined-id integer assigned to the "Follow up" label.
     *
     * <p>Matching this value against the {@code predefinedId} on a
     * {@link com.github.auties00.cobalt.wire.linked.preference.Label} synced from the
     * server identifies the "Follow up" built-in label.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_IDS",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int PREDEFINED_LABEL_ID_FOLLOW_UP = 7;

    /**
     * The predefined-id integer assigned to the "Lead" label.
     *
     * <p>Matching this value against the {@code predefinedId} on a
     * {@link com.github.auties00.cobalt.wire.linked.preference.Label} synced from the
     * server identifies the "Lead" built-in label; the customer-manager
     * "apply lead label" automation keys off this identifier.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_IDS",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int PREDEFINED_LABEL_ID_LEAD = 8;

    /**
     * The predefined-id integer reserved for the "Delivery-Order: new order"
     * derived label.
     *
     * <p>The Delivery-Order surface synthesises this derived id alongside its
     * parent {@link #PREDEFINED_LABEL_ID_NEW_ORDER};
     * {@link #mapPredefinedIdToLabelName(int)} folds it back onto the parent
     * display name so the UI shows a single chip.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_IDS",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int PREDEFINED_LABEL_ID_DO_NEW_ORDER = 9;

    /**
     * The predefined-id integer reserved for the "Delivery-Order: lead" derived
     * label.
     *
     * <p>The Delivery-Order surface synthesises this derived id alongside its
     * parent {@link #PREDEFINED_LABEL_ID_LEAD};
     * {@link #mapPredefinedIdToLabelName(int)} folds it back onto the parent
     * display name so the UI shows a single chip.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_IDS",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int PREDEFINED_LABEL_ID_DO_LEAD = 10;

    /**
     * The user-visible display name of the "New customer" predefined label.
     *
     * <p>Serves as the {@code labelName} input to
     * {@link #mapLabelNameToPredefinedId(String)} when matching server-side
     * label additions back to the built-in id.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_NAMES",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String PREDEFINED_LABEL_NAME_NEW_CUSTOMER = "New customer";

    /**
     * The user-visible display name of the "New order" predefined label.
     *
     * <p>Serves as the {@code labelName} input to
     * {@link #mapLabelNameToPredefinedId(String)} when matching server-side
     * label additions back to the built-in id.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_NAMES",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String PREDEFINED_LABEL_NAME_NEW_ORDER = "New order";

    /**
     * The user-visible display name of the "Pending payment" predefined label.
     *
     * <p>Serves as the {@code labelName} input to
     * {@link #mapLabelNameToPredefinedId(String)} when matching server-side
     * label additions back to the built-in id.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_NAMES",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String PREDEFINED_LABEL_NAME_PENDING_PAYMENT = "Pending payment";

    /**
     * The user-visible display name of the "Paid" predefined label.
     *
     * <p>Serves as the {@code labelName} input to
     * {@link #mapLabelNameToPredefinedId(String)} when matching server-side
     * label additions back to the built-in id.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_NAMES",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String PREDEFINED_LABEL_NAME_PAID = "Paid";

    /**
     * The user-visible display name of the "Order complete" predefined label.
     *
     * <p>Serves as the {@code labelName} input to
     * {@link #mapLabelNameToPredefinedId(String)} when matching server-side
     * label additions back to the built-in id.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_NAMES",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String PREDEFINED_LABEL_NAME_ORDER_COMPLETE = "Order complete";

    /**
     * The user-visible display name of the "Important" predefined label.
     *
     * <p>Serves as the {@code labelName} input to
     * {@link #mapLabelNameToPredefinedId(String)} when matching server-side
     * label additions back to the built-in id.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_NAMES",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String PREDEFINED_LABEL_NAME_IMPORTANT = "Important";

    /**
     * The user-visible display name of the "Follow up" predefined label.
     *
     * <p>Serves as the {@code labelName} input to
     * {@link #mapLabelNameToPredefinedId(String)} when matching server-side
     * label additions back to the built-in id.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_NAMES",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String PREDEFINED_LABEL_NAME_FOLLOW_UP = "Follow up";

    /**
     * The user-visible display name of the "Lead" predefined label.
     *
     * <p>Serves as the {@code labelName} input to
     * {@link #mapLabelNameToPredefinedId(String)} when matching server-side
     * label additions back to the built-in id.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "PREDEFINED_LABEL_NAMES",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String PREDEFINED_LABEL_NAME_LEAD = "Lead";

    /**
     * The canonical subtype string WhatsApp transmits on the wire for the
     * "New customer" custom-label subtype.
     *
     * <p>Feeds {@link #mapCustomLabelSubtypeToCTWALabelType(String)} when
     * translating a sync mutation into the corresponding CTWA telemetry enum.
     */
    static final String CUSTOM_LABEL_SUBTYPE_NEW_CUSTOMER = "new_customer";

    /**
     * The canonical subtype string WhatsApp transmits on the wire for the
     * "New order" custom-label subtype.
     *
     * <p>Feeds {@link #mapCustomLabelSubtypeToCTWALabelType(String)} when
     * translating a sync mutation into the corresponding CTWA telemetry enum.
     */
    static final String CUSTOM_LABEL_SUBTYPE_NEW_ORDER = "new_order";

    /**
     * The canonical subtype string WhatsApp transmits on the wire for the
     * "Pending payment" custom-label subtype.
     *
     * <p>Feeds {@link #mapCustomLabelSubtypeToCTWALabelType(String)} when
     * translating a sync mutation into the corresponding CTWA telemetry enum.
     */
    static final String CUSTOM_LABEL_SUBTYPE_PENDING_PAYMENT = "pending_payment";

    /**
     * The canonical subtype string WhatsApp transmits on the wire for the
     * "Paid" custom-label subtype.
     *
     * <p>Feeds {@link #mapCustomLabelSubtypeToCTWALabelType(String)} when
     * translating a sync mutation into the corresponding CTWA telemetry enum.
     */
    static final String CUSTOM_LABEL_SUBTYPE_PAID = "paid";

    /**
     * The canonical subtype string WhatsApp transmits on the wire for the
     * "Order complete" custom-label subtype.
     *
     * <p>Feeds {@link #mapCustomLabelSubtypeToCTWALabelType(String)} when
     * translating a sync mutation into the corresponding CTWA telemetry enum.
     */
    static final String CUSTOM_LABEL_SUBTYPE_ORDER_COMPLETE = "order_complete";

    /**
     * The canonical subtype string WhatsApp transmits on the wire for the
     * "Important" custom-label subtype.
     *
     * <p>Feeds {@link #mapCustomLabelSubtypeToCTWALabelType(String)} when
     * translating a sync mutation into the corresponding CTWA telemetry enum.
     */
    static final String CUSTOM_LABEL_SUBTYPE_IMPORTANT = "important";

    /**
     * The canonical subtype string WhatsApp transmits on the wire for the
     * "Follow up" custom-label subtype.
     *
     * <p>Feeds {@link #mapCustomLabelSubtypeToCTWALabelType(String)} when
     * translating a sync mutation into the corresponding CTWA telemetry enum.
     */
    static final String CUSTOM_LABEL_SUBTYPE_FOLLOW_UP = "follow_up";

    /**
     * The canonical subtype string WhatsApp transmits on the wire for the
     * "Lead" custom-label subtype.
     *
     * <p>Feeds {@link #mapCustomLabelSubtypeToCTWALabelType(String)} when
     * translating a sync mutation into the corresponding CTWA telemetry enum.
     */
    static final String CUSTOM_LABEL_SUBTYPE_LEAD = "lead";

    /**
     * The maximum number of characters a user may type into a label name.
     *
     * <p>User-entered names are validated against this ceiling before a
     * {@link com.github.auties00.cobalt.wire.linked.preference.Label} is constructed;
     * WhatsApp enforces the limit client-side so labels always fit on the
     * chat-list card.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "LABEL_NAME_MAX_LENGTH",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int LABEL_NAME_MAX_LENGTH = 100;

    /**
     * Prevents instantiation of this utility class.
     *
     * <p>The class is a pure constants-and-helpers holder; instances carry no
     * state.
     *
     * @throws AssertionError always
     */
    private BusinessLabelConstants() {
        throw new AssertionError("BusinessLabelConstants is not instantiable");
    }

    /**
     * Returns the predefined-label identifier associated with the given
     * user-visible label name when that name matches one of the eight predefined
     * display strings WhatsApp ships.
     *
     * <p>When handling a label-add action from the server, a name matching one
     * of the eight built-ins yields the id to store on the
     * {@link com.github.auties00.cobalt.wire.linked.preference.Label} so subsequent
     * renders pick up the built-in styling. Any custom user-defined name,
     * including {@code null}, returns empty.
     *
     * @implNote
     * This implementation matches case-sensitively against the exact display
     * strings; it does not mirror the case-folding the JS export performs
     * against the locale-translated names, since Cobalt holds only the canonical
     * English strings.
     *
     * @param labelName the user-visible label name to resolve
     * @return the predefined identifier of the matching built-in label, or empty
     *         when {@code labelName} is not a predefined display name
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "mapLabelNameToPredefinedId",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static OptionalInt mapLabelNameToPredefinedId(String labelName) {
        if (labelName == null) {
            return OptionalInt.empty();
        }
        return switch (labelName) {
            case PREDEFINED_LABEL_NAME_NEW_CUSTOMER -> OptionalInt.of(PREDEFINED_LABEL_ID_NEW_CUSTOMER);
            case PREDEFINED_LABEL_NAME_NEW_ORDER -> OptionalInt.of(PREDEFINED_LABEL_ID_NEW_ORDER);
            case PREDEFINED_LABEL_NAME_PENDING_PAYMENT -> OptionalInt.of(PREDEFINED_LABEL_ID_PENDING_PAYMENT);
            case PREDEFINED_LABEL_NAME_PAID -> OptionalInt.of(PREDEFINED_LABEL_ID_PAID);
            case PREDEFINED_LABEL_NAME_ORDER_COMPLETE -> OptionalInt.of(PREDEFINED_LABEL_ID_ORDER_COMPLETE);
            case PREDEFINED_LABEL_NAME_IMPORTANT -> OptionalInt.of(PREDEFINED_LABEL_ID_IMPORTANT);
            case PREDEFINED_LABEL_NAME_FOLLOW_UP -> OptionalInt.of(PREDEFINED_LABEL_ID_FOLLOW_UP);
            case PREDEFINED_LABEL_NAME_LEAD -> OptionalInt.of(PREDEFINED_LABEL_ID_LEAD);
            default -> OptionalInt.empty();
        };
    }

    /**
     * Returns the {@link CtwaLabelType} that corresponds to the given
     * custom-label subtype string used on the Click-to-WhatsApp ads telemetry
     * channel.
     *
     * <p>Translates a custom-label subtype received from the server into the
     * enum carried on a CTWA WAM event. A {@code null} or unrecognised subtype
     * folds back to {@link CtwaLabelType#LEAD} so the CTWA sink always receives a
     * valid value.
     *
     * @param subtype the custom-label subtype string, typically sourced from a
     *                server-side sync, may be {@code null}
     * @return the matching {@link CtwaLabelType}, or {@link CtwaLabelType#LEAD}
     *         when {@code subtype} is {@code null} or unrecognised
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "mapCustomLabelSubtypeToCTWALabelType",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static CtwaLabelType mapCustomLabelSubtypeToCTWALabelType(String subtype) {
        if (subtype == null) {
            return CtwaLabelType.LEAD;
        }
        return switch (subtype) {
            case CUSTOM_LABEL_SUBTYPE_NEW_CUSTOMER -> CtwaLabelType.NEW_CUSTOMER;
            case CUSTOM_LABEL_SUBTYPE_NEW_ORDER -> CtwaLabelType.NEW_ORDER;
            case CUSTOM_LABEL_SUBTYPE_PENDING_PAYMENT -> CtwaLabelType.PENDING_PAYMENT;
            case CUSTOM_LABEL_SUBTYPE_PAID -> CtwaLabelType.PAID;
            case CUSTOM_LABEL_SUBTYPE_ORDER_COMPLETE -> CtwaLabelType.ORDER_COMPLETE;
            case CUSTOM_LABEL_SUBTYPE_IMPORTANT -> CtwaLabelType.IMPORTANT;
            case CUSTOM_LABEL_SUBTYPE_FOLLOW_UP -> CtwaLabelType.FOLLOW_UP;
            default -> CtwaLabelType.LEAD;
        };
    }

    /**
     * Returns the user-visible display name for the given predefined label
     * identifier.
     *
     * <p>Resolves the label chip text from a server-synced
     * {@link com.github.auties00.cobalt.wire.linked.preference.Label}. The eight
     * primary predefined ids ({@link #PREDEFINED_LABEL_ID_NEW_CUSTOMER} through
     * {@link #PREDEFINED_LABEL_ID_LEAD}) and the two derived Delivery-Order
     * identifiers are accepted; the derived ids fold back onto the corresponding
     * primary names so the UI shows a single chip.
     *
     * @param predefinedId the predefined label identifier to resolve
     * @return the display name associated with {@code predefinedId}, or empty
     *         when {@code predefinedId} is not a known predefined label id
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelConstants",
            exports = "mapPredefinedIdToLabelName",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static Optional<String> mapPredefinedIdToLabelName(int predefinedId) {
        return switch (predefinedId) {
            case PREDEFINED_LABEL_ID_NEW_CUSTOMER -> Optional.of(PREDEFINED_LABEL_NAME_NEW_CUSTOMER);
            case PREDEFINED_LABEL_ID_NEW_ORDER, PREDEFINED_LABEL_ID_DO_NEW_ORDER
                    -> Optional.of(PREDEFINED_LABEL_NAME_NEW_ORDER);
            case PREDEFINED_LABEL_ID_PENDING_PAYMENT -> Optional.of(PREDEFINED_LABEL_NAME_PENDING_PAYMENT);
            case PREDEFINED_LABEL_ID_PAID -> Optional.of(PREDEFINED_LABEL_NAME_PAID);
            case PREDEFINED_LABEL_ID_ORDER_COMPLETE -> Optional.of(PREDEFINED_LABEL_NAME_ORDER_COMPLETE);
            case PREDEFINED_LABEL_ID_IMPORTANT -> Optional.of(PREDEFINED_LABEL_NAME_IMPORTANT);
            case PREDEFINED_LABEL_ID_FOLLOW_UP -> Optional.of(PREDEFINED_LABEL_NAME_FOLLOW_UP);
            case PREDEFINED_LABEL_ID_LEAD, PREDEFINED_LABEL_ID_DO_LEAD
                    -> Optional.of(PREDEFINED_LABEL_NAME_LEAD);
            default -> Optional.empty();
        };
    }
}
