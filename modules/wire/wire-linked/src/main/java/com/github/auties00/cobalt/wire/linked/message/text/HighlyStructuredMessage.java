package com.github.auties00.cobalt.wire.linked.message.text;

import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.linked.message.interactive.TemplateMessage;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.*;

/**
 * A server-side template message rendered from a registered template
 * element, typically used for business notifications such as OTP codes,
 * order confirmations, or appointment reminders.
 *
 * <p>A highly structured message references a template element by its
 * namespace and element name; the template itself is pre-approved and
 * stored on WhatsApp's servers. Recipient clients substitute the
 * provided parameters into the template to render the final message.
 * Each parameter may be localised (via {@link HSMLocalizableParameter}),
 * allowing currency amounts and date/time values to be formatted
 * according to the recipient's locale.
 *
 * <p>When a highly structured message cannot be rendered (for example on
 * older clients that do not support the referenced template), the
 * {@code fallbackLg}/{@code fallbackLc} and {@code deterministicLg}/
 * {@code deterministicLc} locale hints guide the substitution of the
 * fallback string. A hydrated {@link TemplateMessage} may also be
 * attached, providing a fully resolved interactive template for clients
 * that support it.
 *
 * <p>Highly structured messages may also appear as the title of another
 * {@link TemplateMessage}, which is why this type implements
 * {@link TemplateMessage.Title}.
 */
@ProtobufMessage(name = "Message.HighlyStructuredMessage")
public final class HighlyStructuredMessage implements TemplateMessage.Title, Message {
    /**
     * The namespace of the referenced template, used by the server to
     * scope template names per business account.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String namespace;

    /**
     * The element name of the referenced template inside its namespace.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String elementName;

    /**
     * The ordered list of plain string parameters substituted into the
     * template's placeholders.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    List<String> params;

    /**
     * The BCP-47 language tag of the fallback message body used when the
     * client cannot render the template.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String fallbackLg;

    /**
     * The locale code (country variant) of the fallback message body,
     * paired with {@link #fallbackLg}.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String fallbackLc;

    /**
     * The ordered list of localisable parameters (currency, date-time,
     * or plain strings) substituted into the template.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    List<HSMLocalizableParameter> localizableParams;

    /**
     * The BCP-47 language tag used for deterministic rendering across
     * clients, ensuring all recipients see the same localised output.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String deterministicLg;

    /**
     * The locale code paired with {@link #deterministicLg} for
     * deterministic rendering.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String deterministicLc;

    /**
     * A fully hydrated {@link TemplateMessage} ready for interactive
     * rendering, attached so clients that support template messages can
     * skip the template-lookup step.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    TemplateMessage hydratedHsm;


    /**
     * Constructs a new highly structured message with all supported
     * fields populated.
     *
     * @param namespace          the template namespace
     * @param elementName        the template element name
     * @param params             the plain string parameters
     * @param fallbackLg         the fallback language tag
     * @param fallbackLc         the fallback locale code
     * @param localizableParams  the localisable parameters
     * @param deterministicLg    the deterministic language tag
     * @param deterministicLc    the deterministic locale code
     * @param hydratedHsm        the hydrated interactive template
     */
    HighlyStructuredMessage(String namespace, String elementName, List<String> params, String fallbackLg, String fallbackLc, List<HSMLocalizableParameter> localizableParams, String deterministicLg, String deterministicLc, TemplateMessage hydratedHsm) {
        this.namespace = namespace;
        this.elementName = elementName;
        this.params = params;
        this.fallbackLg = fallbackLg;
        this.fallbackLc = fallbackLc;
        this.localizableParams = localizableParams;
        this.deterministicLg = deterministicLg;
        this.deterministicLc = deterministicLc;
        this.hydratedHsm = hydratedHsm;
    }

    /**
     * Returns the namespace of the referenced template, if set.
     *
     * @return an {@link Optional} containing the namespace,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<String> namespace() {
        return Optional.ofNullable(namespace);
    }

    /**
     * Returns the element name of the referenced template, if set.
     *
     * @return an {@link Optional} containing the element name,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<String> elementName() {
        return Optional.ofNullable(elementName);
    }

    /**
     * Returns the ordered list of plain string parameters substituted
     * into the template.
     *
     * @return an unmodifiable list of string parameters; an empty list if
     *         none are set
     */
    public List<String> params() {
        return params == null ? List.of() : Collections.unmodifiableList(params);
    }

    /**
     * Returns the BCP-47 language tag of the fallback body, if set.
     *
     * @return an {@link Optional} containing the language tag,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<String> fallbackLg() {
        return Optional.ofNullable(fallbackLg);
    }

    /**
     * Returns the fallback locale code, if set.
     *
     * @return an {@link Optional} containing the locale code,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<String> fallbackLc() {
        return Optional.ofNullable(fallbackLc);
    }

    /**
     * Returns the ordered list of localisable parameters substituted into
     * the template.
     *
     * @return an unmodifiable list of {@link HSMLocalizableParameter};
     *         an empty list if none are set
     */
    public List<HSMLocalizableParameter> localizableParams() {
        return localizableParams == null ? List.of() : Collections.unmodifiableList(localizableParams);
    }

    /**
     * Returns the BCP-47 language tag used for deterministic rendering,
     * if set.
     *
     * @return an {@link Optional} containing the language tag,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<String> deterministicLg() {
        return Optional.ofNullable(deterministicLg);
    }

    /**
     * Returns the deterministic locale code, if set.
     *
     * @return an {@link Optional} containing the locale code,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<String> deterministicLc() {
        return Optional.ofNullable(deterministicLc);
    }

    /**
     * Returns the hydrated interactive {@link TemplateMessage}, if one
     * has been attached for template-aware clients.
     *
     * @return an {@link Optional} containing the hydrated template,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<TemplateMessage> hydratedHsm() {
        return Optional.ofNullable(hydratedHsm);
    }

    /**
     * Sets the namespace of the referenced template.
     *
     * @param namespace the namespace, or {@code null} to clear
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Sets the element name of the referenced template.
     *
     * @param elementName the element name, or {@code null} to clear
     */
    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    /**
     * Sets the plain string parameters substituted into the template.
     *
     * @param params the parameter list, or {@code null} to clear
     */
    public void setParams(List<String> params) {
        this.params = params;
    }

    /**
     * Sets the fallback language tag.
     *
     * @param fallbackLg the language tag, or {@code null} to clear
     */
    public void setFallbackLg(String fallbackLg) {
        this.fallbackLg = fallbackLg;
    }

    /**
     * Sets the fallback locale code.
     *
     * @param fallbackLc the locale code, or {@code null} to clear
     */
    public void setFallbackLc(String fallbackLc) {
        this.fallbackLc = fallbackLc;
    }

    /**
     * Sets the localisable parameters substituted into the template.
     *
     * @param localizableParams the parameter list, or {@code null} to clear
     */
    public void setLocalizableParams(List<HSMLocalizableParameter> localizableParams) {
        this.localizableParams = localizableParams;
    }

    /**
     * Sets the deterministic language tag.
     *
     * @param deterministicLg the language tag, or {@code null} to clear
     */
    public void setDeterministicLg(String deterministicLg) {
        this.deterministicLg = deterministicLg;
    }

    /**
     * Sets the deterministic locale code.
     *
     * @param deterministicLc the locale code, or {@code null} to clear
     */
    public void setDeterministicLc(String deterministicLc) {
        this.deterministicLc = deterministicLc;
    }

    /**
     * Sets the hydrated interactive template attached to the message.
     *
     * @param hydratedHsm the hydrated template, or {@code null} to clear
     */
    public void setHydratedHsm(TemplateMessage hydratedHsm) {
        this.hydratedHsm = hydratedHsm;
    }

    /**
     * Sealed marker interface for the polymorphic payload of an
     * {@link HSMLocalizableParameter}, distinguishing between currency
     * values and date/time values.
     *
     * <p>Exactly one of the permitted implementations is carried by a
     * parameter at a time; the parameter falls back to its default
     * string when none is present.
     */
    public sealed interface ParamOneof permits HSMLocalizableParameter.HSMCurrency, HSMLocalizableParameter.HSMDateTime {
    }

    /**
     * A single localisable parameter substituted into a
     * {@link HighlyStructuredMessage}.
     *
     * <p>Each parameter carries a {@link #defaultValue()} used when the
     * recipient's client cannot localise the value, plus an optional
     * typed payload describing how to format the value: a
     * {@link HSMCurrency} for amounts or an {@link HSMDateTime} for
     * dates and times.
     */
    @ProtobufMessage(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter")
    public static final class HSMLocalizableParameter {
        /**
         * The pre-rendered default value used when the parameter cannot
         * be localised on the recipient's device.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String defaultValue;

        /**
         * The currency payload for this parameter, if the parameter
         * represents a currency amount.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        HSMLocalizableParameter.HSMCurrency currency;

        /**
         * The date/time payload for this parameter, if the parameter
         * represents a date/time value.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        HSMLocalizableParameter.HSMDateTime dateTime;


        /**
         * Constructs a new localisable parameter with the supplied
         * fields. At most one of {@code currency} and {@code dateTime}
         * should be non-null.
         *
         * @param defaultValue the pre-rendered fallback value
         * @param currency     the optional currency payload
         * @param dateTime     the optional date/time payload
         */
        HSMLocalizableParameter(String defaultValue, HSMCurrency currency, HSMDateTime dateTime) {
            this.defaultValue = defaultValue;
            this.currency = currency;
            this.dateTime = dateTime;
        }

        /**
         * Returns the pre-rendered fallback value, if set.
         *
         * @return an {@link Optional} containing the default value,
         *         or {@link Optional#empty()} if unset
         */
        public Optional<String> defaultValue() {
            return Optional.ofNullable(defaultValue);
        }

        /**
         * Returns the typed payload for this parameter, or
         * {@link Optional#empty()} if the parameter carries only its
         * default string.
         *
         * @return an {@link Optional} containing the {@link HSMCurrency}
         *         or {@link HSMDateTime} payload, whichever is set, or
         *         {@link Optional#empty()} if neither is set
         */
        public Optional<? extends ParamOneof> paramOneof() {
            if (currency != null) return Optional.of(currency);
            if (dateTime != null) return Optional.of(dateTime);
            return Optional.empty();
        }

        /**
         * Sets the pre-rendered fallback value.
         *
         * @param defaultValue the fallback value, or {@code null} to clear
         */
        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
    }

        /**
         * Sets the currency payload, clearing the date/time payload
         * should be performed separately if mutual exclusivity is
         * desired.
         *
         * @param currency the currency payload, or {@code null} to clear
         */
        public void setCurrency(HSMCurrency currency) {
            this.currency = currency;
    }

        /**
         * Sets the date/time payload, clearing the currency payload
         * should be performed separately if mutual exclusivity is
         * desired.
         *
         * @param dateTime the date/time payload, or {@code null} to clear
         */
        public void setDateTime(HSMDateTime dateTime) {
            this.dateTime = dateTime;
    }

        /**
         * Sealed marker interface for the polymorphic payload of an
         * {@link HSMDateTime}, distinguishing between calendar-component
         * representation and Unix-epoch representation.
         */
        public sealed interface DatetimeOneof permits HSMDateTime.HSMDateTimeComponent, HSMDateTime.HSMDateTimeUnixEpoch {
        }

        /**
         * A currency amount parameter, comprising an ISO currency code
         * and an integer amount expressed in the currency's smallest
         * thousandth unit (that is, the amount multiplied by 1000 to
         * preserve fractional values without floating-point).
         */
        @ProtobufMessage(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMCurrency")
        public static final class HSMCurrency implements ParamOneof {
            /**
             * The ISO 4217 currency code of the amount (for example,
             * "USD", "EUR").
             */
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String currencyCode;

            /**
             * The amount multiplied by 1000 to preserve three fractional
             * digits without floating-point rounding.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.INT64)
            Long amount1000;


            /**
             * Constructs a new currency payload.
             *
             * @param currencyCode the ISO 4217 currency code
             * @param amount1000   the amount, multiplied by 1000
             */
            HSMCurrency(String currencyCode, Long amount1000) {
                this.currencyCode = currencyCode;
                this.amount1000 = amount1000;
            }

            /**
             * Returns the ISO 4217 currency code, if set.
             *
             * @return an {@link Optional} containing the currency code,
             *         or {@link Optional#empty()} if unset
             */
            public Optional<String> currencyCode() {
                return Optional.ofNullable(currencyCode);
            }

            /**
             * Returns the amount multiplied by 1000, if set.
             *
             * @return an {@link OptionalLong} containing the amount,
             *         or {@link OptionalLong#empty()} if unset
             */
            public OptionalLong amount1000() {
                return amount1000 == null ? OptionalLong.empty() : OptionalLong.of(amount1000);
            }

            /**
             * Sets the ISO 4217 currency code.
             *
             * @param currencyCode the currency code, or {@code null} to clear
             */
            public void setCurrencyCode(String currencyCode) {
                this.currencyCode = currencyCode;
    }

            /**
             * Sets the amount, scaled by a factor of 1000.
             *
             * @param amount1000 the amount multiplied by 1000, or
             *                   {@code null} to clear
             */
            public void setAmount1000(Long amount1000) {
                this.amount1000 = amount1000;
    }
        }

        /**
         * A date/time parameter, represented either as a calendar
         * component breakdown ({@link HSMDateTimeComponent}) or as a
         * Unix-epoch timestamp ({@link HSMDateTimeUnixEpoch}).
         *
         * <p>Exactly one of the two representations is carried at a
         * time, surfaced via {@link #datetimeOneof()}.
         */
        @ProtobufMessage(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime")
        public static final class HSMDateTime implements ParamOneof {
            /**
             * The calendar-component representation of the date/time, if
             * this is the chosen encoding.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
            HSMLocalizableParameter.HSMDateTime.HSMDateTimeComponent component;

            /**
             * The Unix-epoch representation of the date/time, if this is
             * the chosen encoding.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
            HSMLocalizableParameter.HSMDateTime.HSMDateTimeUnixEpoch unixEpoch;


            /**
             * Constructs a new date/time parameter carrying one of the
             * two possible representations.
             *
             * @param component the component-based representation
             * @param unixEpoch the epoch-based representation
             */
            HSMDateTime(HSMDateTimeComponent component, HSMDateTimeUnixEpoch unixEpoch) {
                this.component = component;
                this.unixEpoch = unixEpoch;
            }

            /**
             * Returns the active date/time representation, or
             * {@link Optional#empty()} if neither is set.
             *
             * @return an {@link Optional} containing the
             *         {@link HSMDateTimeComponent} or the
             *         {@link HSMDateTimeUnixEpoch}, whichever is set
             */
            public Optional<? extends DatetimeOneof> datetimeOneof() {
                if (component != null) return Optional.of(component);
                if (unixEpoch != null) return Optional.of(unixEpoch);
                return Optional.empty();
            }

            /**
             * Sets the calendar-component representation.
             *
             * @param component the component, or {@code null} to clear
             */
            public void setComponent(HSMDateTimeComponent component) {
                this.component = component;
    }

            /**
             * Sets the Unix-epoch representation.
             *
             * @param unixEpoch the epoch payload, or {@code null} to clear
             */
            public void setUnixEpoch(HSMDateTimeUnixEpoch unixEpoch) {
                this.unixEpoch = unixEpoch;
    }

            /**
             * A date/time expressed as individual calendar components
             * (year, month, day of week, etc.), paired with the
             * calendar system used to interpret them.
             *
             * <p>Any subset of components may be populated; missing
             * components indicate unspecified fields (for instance an
             * event "on Mondays" sets only {@link #dayOfWeek()}).
             */
            @ProtobufMessage(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime.HSMDateTimeComponent")
            public static final class HSMDateTimeComponent implements DatetimeOneof {
                /**
                 * The day of the week, 1 (Monday) through 7 (Sunday).
                 */
                @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
                HSMLocalizableParameter.HSMDateTime.HSMDateTimeComponent.DayOfWeekType dayOfWeek;

                /**
                 * The calendar year, interpreted in the
                 * {@link #calendar} system.
                 */
                @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
                Integer year;

                /**
                 * The calendar month (1-12).
                 */
                @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
                Integer month;

                /**
                 * The day of the month (1-31).
                 */
                @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
                Integer dayOfMonth;

                /**
                 * The hour of the day (0-23).
                 */
                @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
                Integer hour;

                /**
                 * The minute of the hour (0-59).
                 */
                @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
                Integer minute;

                /**
                 * The calendar system used to interpret the component
                 * fields.
                 */
                @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
                HSMLocalizableParameter.HSMDateTime.HSMDateTimeComponent.CalendarType calendar;


                /**
                 * Constructs a new calendar-component date/time.
                 *
                 * @param dayOfWeek  the day of the week
                 * @param year       the calendar year
                 * @param month      the calendar month
                 * @param dayOfMonth the day of the month
                 * @param hour       the hour of the day
                 * @param minute     the minute of the hour
                 * @param calendar   the calendar system
                 */
                HSMDateTimeComponent(DayOfWeekType dayOfWeek, Integer year, Integer month, Integer dayOfMonth, Integer hour, Integer minute, CalendarType calendar) {
                    this.dayOfWeek = dayOfWeek;
                    this.year = year;
                    this.month = month;
                    this.dayOfMonth = dayOfMonth;
                    this.hour = hour;
                    this.minute = minute;
                    this.calendar = calendar;
                }

                /**
                 * Returns the day of the week, if set.
                 *
                 * @return an {@link Optional} containing the
                 *         {@link DayOfWeekType}, or
                 *         {@link Optional#empty()} if unset
                 */
                public Optional<DayOfWeekType> dayOfWeek() {
                    return Optional.ofNullable(dayOfWeek);
                }

                /**
                 * Returns the calendar year, if set.
                 *
                 * @return an {@link OptionalInt} containing the year,
                 *         or {@link OptionalInt#empty()} if unset
                 */
                public OptionalInt year() {
                    return year == null ? OptionalInt.empty() : OptionalInt.of(year);
                }

                /**
                 * Returns the calendar month, if set.
                 *
                 * @return an {@link OptionalInt} containing the month
                 *         (1-12), or {@link OptionalInt#empty()} if unset
                 */
                public OptionalInt month() {
                    return month == null ? OptionalInt.empty() : OptionalInt.of(month);
                }

                /**
                 * Returns the day of the month, if set.
                 *
                 * @return an {@link OptionalInt} containing the day
                 *         (1-31), or {@link OptionalInt#empty()} if unset
                 */
                public OptionalInt dayOfMonth() {
                    return dayOfMonth == null ? OptionalInt.empty() : OptionalInt.of(dayOfMonth);
                }

                /**
                 * Returns the hour of the day, if set.
                 *
                 * @return an {@link OptionalInt} containing the hour
                 *         (0-23), or {@link OptionalInt#empty()} if unset
                 */
                public OptionalInt hour() {
                    return hour == null ? OptionalInt.empty() : OptionalInt.of(hour);
                }

                /**
                 * Returns the minute of the hour, if set.
                 *
                 * @return an {@link OptionalInt} containing the minute
                 *         (0-59), or {@link OptionalInt#empty()} if unset
                 */
                public OptionalInt minute() {
                    return minute == null ? OptionalInt.empty() : OptionalInt.of(minute);
                }

                /**
                 * Returns the calendar system used to interpret the
                 * component fields, if set.
                 *
                 * @return an {@link Optional} containing the
                 *         {@link CalendarType}, or
                 *         {@link Optional#empty()} if unset
                 */
                public Optional<CalendarType> calendar() {
                    return Optional.ofNullable(calendar);
                }

                /**
                 * Sets the day of the week.
                 *
                 * @param dayOfWeek the day of the week, or {@code null} to clear
                 */
                public void setDayOfWeek(DayOfWeekType dayOfWeek) {
                    this.dayOfWeek = dayOfWeek;
    }

                /**
                 * Sets the calendar year.
                 *
                 * @param year the year, or {@code null} to clear
                 */
                public void setYear(Integer year) {
                    this.year = year;
    }

                /**
                 * Sets the calendar month (1-12).
                 *
                 * @param month the month, or {@code null} to clear
                 */
                public void setMonth(Integer month) {
                    this.month = month;
    }

                /**
                 * Sets the day of the month (1-31).
                 *
                 * @param dayOfMonth the day of the month, or {@code null} to clear
                 */
                public void setDayOfMonth(Integer dayOfMonth) {
                    this.dayOfMonth = dayOfMonth;
    }

                /**
                 * Sets the hour of the day (0-23).
                 *
                 * @param hour the hour, or {@code null} to clear
                 */
                public void setHour(Integer hour) {
                    this.hour = hour;
    }

                /**
                 * Sets the minute of the hour (0-59).
                 *
                 * @param minute the minute, or {@code null} to clear
                 */
                public void setMinute(Integer minute) {
                    this.minute = minute;
    }

                /**
                 * Sets the calendar system used to interpret the
                 * component fields.
                 *
                 * @param calendar the calendar system, or {@code null} to clear
                 */
                public void setCalendar(CalendarType calendar) {
                    this.calendar = calendar;
    }

                /**
                 * The calendar systems supported for interpreting
                 * {@link HSMDateTimeComponent} fields.
                 */
                @ProtobufEnum(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime.HSMDateTimeComponent.CalendarType")
                public static enum CalendarType {
                    /**
                     * The Gregorian (Western) calendar system.
                     */
                    GREGORIAN(1),
                    /**
                     * The Solar Hijri (Persian/Iranian) calendar system.
                     */
                    SOLAR_HIJRI(2);

                    /**
                     * Constructs a calendar-type constant with the
                     * supplied protobuf index.
                     *
                     * @param index the wire-format index of the constant
                     */
                    CalendarType(@ProtobufEnumIndex int index) {
                        this.index = index;
                    }

                    /**
                     * The wire-format index corresponding to this
                     * constant.
                     */
                    final int index;

                    /**
                     * Returns the wire-format index of this constant.
                     *
                     * @return the numeric protobuf index
                     */
                    public int index() {
                        return this.index;
                    }
                }

                /**
                 * The days of the week, numbered according to the
                 * WhatsApp wire protocol (Monday = 1 through Sunday = 7).
                 */
                @ProtobufEnum(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime.HSMDateTimeComponent.DayOfWeekType")
                public static enum DayOfWeekType {
                    /**
                     * Monday.
                     */
                    MONDAY(1),
                    /**
                     * Tuesday.
                     */
                    TUESDAY(2),
                    /**
                     * Wednesday.
                     */
                    WEDNESDAY(3),
                    /**
                     * Thursday.
                     */
                    THURSDAY(4),
                    /**
                     * Friday.
                     */
                    FRIDAY(5),
                    /**
                     * Saturday.
                     */
                    SATURDAY(6),
                    /**
                     * Sunday.
                     */
                    SUNDAY(7);

                    /**
                     * Constructs a day-of-week constant with the
                     * supplied protobuf index.
                     *
                     * @param index the wire-format index of the constant
                     */
                    DayOfWeekType(@ProtobufEnumIndex int index) {
                        this.index = index;
                    }

                    /**
                     * The wire-format index corresponding to this
                     * constant.
                     */
                    final int index;

                    /**
                     * Returns the wire-format index of this constant.
                     *
                     * @return the numeric protobuf index
                     */
                    public int index() {
                        return this.index;
                    }
                }
            }

            /**
             * A date/time expressed as an absolute instant in time,
             * encoded as seconds since the Unix epoch.
             */
            @ProtobufMessage(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime.HSMDateTimeUnixEpoch")
            public static final class HSMDateTimeUnixEpoch implements DatetimeOneof {
                /**
                 * The absolute instant encoded by this payload.
                 */
                @ProtobufProperty(index = 1, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
                Instant timestamp;


                /**
                 * Constructs a new Unix-epoch date/time payload.
                 *
                 * @param timestamp the absolute instant to carry
                 */
                HSMDateTimeUnixEpoch(Instant timestamp) {
                    this.timestamp = timestamp;
                }

                /**
                 * Returns the absolute instant encoded by this payload,
                 * if set.
                 *
                 * @return an {@link Optional} containing the
                 *         {@link Instant}, or {@link Optional#empty()}
                 *         if unset
                 */
                public Optional<Instant> timestamp() {
                    return Optional.ofNullable(timestamp);
                }

                /**
                 * Sets the absolute instant encoded by this payload.
                 *
                 * @param timestamp the instant, or {@code null} to clear
                 */
                public void setTimestamp(Instant timestamp) {
                    this.timestamp = timestamp;
    }
            }
        }
    }
}
