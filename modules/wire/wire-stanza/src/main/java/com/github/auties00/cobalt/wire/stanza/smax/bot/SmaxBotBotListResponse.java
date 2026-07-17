package com.github.auties00.cobalt.wire.stanza.smax.bot;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The reply produced by the relay for a {@link SmaxBotBotListRequest}; a V2 directory, a
 * V3 directory, or an error envelope.
 *
 * <p>The {@link SuccessV2} and {@link SuccessV3} arms hand the AI-bot-directory UI a
 * structured projection of the bot catalogue (sections, entries, themes, display-type
 * hints); the {@link Error} arm carries the relay's rejection code-text pair.
 */
public sealed interface SmaxBotBotListResponse extends SmaxStanza.Response
        permits SmaxBotBotListResponse.SuccessV2, SmaxBotBotListResponse.SuccessV3, SmaxBotBotListResponse.Error {

    /**
     * Resolves an inbound IQ reply into the first matching variant in
     * V2-then-V3-then-error priority.
     *
     * <p>Invoked by the smax send pipeline after dispatching a
     * {@link SmaxBotBotListRequest}. The dispatcher tries {@link SuccessV2} first
     * (matching legacy clients that pin {@code botV="2"}), then {@link SuccessV3}, then
     * the {@link Error} envelope, and returns {@link Optional#empty()} when no documented
     * variant matched.
     *
     * @implNote
     * This implementation mirrors the WA Web disjunction's priority order.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the originating outbound IQ stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or
     *         {@link Optional#empty()} when no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBotBotListRPC",
            exports = "sendBotListRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxBotBotListResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var v2 = SuccessV2.of(stanza, request);
        if (v2.isPresent()) {
            return v2;
        }
        var v3 = SuccessV3.of(stanza, request);
        if (v3.isPresent()) {
            return v3;
        }
        return Error.of(stanza, request);
    }

    /**
     * Reports whether a JID's server domain belongs to the user-JID admit set (phone,
     * legacy phone, interop, messenger, lid, or bot).
     *
     * <p>Used by every inbound parser branch that reads a bot or default-entry JID. The
     * bot wire schema admits both {@code @bot} JIDs (persona entries) and standard user
     * JIDs (the bot-of-the-day default).
     *
     * @implNote
     * This implementation tests each server-domain value via
     * {@link Jid#hasServer(JidServer)} rather than allocating an intermediate
     * {@link java.util.Set}; the call site is hot on directory replies and the linear
     * test is faster for six entries.
     *
     * @param jid the JID to test; never {@code null}
     * @return {@code true} when {@code jid}'s server is one of the admitted user-JID
     *         domains
     */
    @WhatsAppWebExport(moduleName = "WAJids",
            exports = "validateUserJid", adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isUserJidServer(Jid jid) {
        return jid.hasServer(JidServer.user())
                || jid.hasServer(JidServer.legacyUser())
                || jid.hasServer(JidServer.interop())
                || jid.hasServer(JidServer.messenger())
                || jid.hasServer(JidServer.lid())
                || jid.hasServer(JidServer.bot());
    }

    /**
     * The legacy V2 directory reply, pinned by {@code v="2"} on the top-level
     * {@code <bot>} child.
     *
     * <p>Returned when the originating request asked for {@code botV="2"}; carries the
     * bot-of-the-day default by JID and persona-id only, plus a non-empty list of
     * sections.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBotBotListResponseSuccessV2")
    @WhatsAppWebModule(moduleName = "WASmaxInBotIQResultResponseMixin")
    final class SuccessV2 implements SmaxBotBotListResponse {
        /**
         * The protocol revision; always the literal {@code "2"}.
         */
        private final String botV;

        /**
         * The JID of the bot-of-the-day default entry.
         *
         * <p>Either an {@code @bot} JID or a standard user JID, per the
         * {@link #isUserJidServer(Jid)} admit set.
         */
        private final Jid botDefaultJid;

        /**
         * The persona-id of the bot-of-the-day default entry.
         */
        private final String botDefaultPersonaId;

        /**
         * The non-empty list of directory sections.
         *
         * <p>V2 requires at least one section per the WA Web schema bound.
         */
        private final List<Section> botSection;

        /**
         * Constructs a V2 directory reply.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after a successful parse; not intended
         * for direct caller use.
         *
         * @implNote
         * This implementation defensively copies the section list, substituting an empty
         * list for {@code null} input so the accessor never returns {@code null}.
         *
         * @param botV                the protocol revision (always {@code "2"}); never
         *                            {@code null}
         * @param botDefaultJid       the default-entry JID; never {@code null}
         * @param botDefaultPersonaId the default-entry persona id; never {@code null}
         * @param botSection          the section list; may be {@code null}, treated as
         *                            empty
         * @throws NullPointerException if any of {@code botV}, {@code botDefaultJid}, or
         *                              {@code botDefaultPersonaId} is {@code null}
         */
        public SuccessV2(String botV, Jid botDefaultJid, String botDefaultPersonaId,
                         List<Section> botSection) {
            this.botV = Objects.requireNonNull(botV, "botV cannot be null");
            this.botDefaultJid = Objects.requireNonNull(botDefaultJid, "botDefaultJid cannot be null");
            this.botDefaultPersonaId = Objects.requireNonNull(botDefaultPersonaId, "botDefaultPersonaId cannot be null");
            this.botSection = List.copyOf(Objects.requireNonNullElse(botSection, List.of()));
        }

        /**
         * Returns the protocol revision.
         *
         * @return the revision; always {@code "2"}; never {@code null}
         */
        public String botV() {
            return botV;
        }

        /**
         * Returns the default-entry JID.
         *
         * <p>Lets the bot-directory UI render the bot-of-the-day chip without scanning
         * the sections.
         *
         * @return the JID; never {@code null}
         */
        public Jid botDefaultJid() {
            return botDefaultJid;
        }

        /**
         * Returns the default-entry persona id.
         *
         * @return the id; never {@code null}
         */
        public String botDefaultPersonaId() {
            return botDefaultPersonaId;
        }

        /**
         * Returns the directory sections.
         *
         * <p>The list is non-empty per the V2 schema bound.
         *
         * @return an unmodifiable list; never {@code null}
         */
        public List<Section> botSection() {
            return botSection;
        }

        /**
         * Parses a V2 reply from the given inbound stanza cross-checked against the
         * originating request.
         *
         * <p>Returns {@link Optional#empty()} for any deviation from the documented V2
         * schema (missing {@code <bot>} child, wrong version literal, missing default
         * entry, missing or malformed sections, empty section list).
         *
         * @implNote
         * This implementation delegates IQ-envelope validation to
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound IQ stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBotBotListResponseSuccessV2",
                exports = "parseBotListResponseSuccessV2",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<SuccessV2> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var botChild = stanza.getChild("bot").orElse(null);
            if (botChild == null) {
                return Optional.empty();
            }
            if (!botChild.hasAttribute("v", "2")) {
                return Optional.empty();
            }
            var defaultChild = botChild.getChild("default").orElse(null);
            if (defaultChild == null) {
                return Optional.empty();
            }
            var defaultJid = defaultChild.getAttributeAsJid("jid").orElse(null);
            if (defaultJid == null || !isUserJidServer(defaultJid)) {
                return Optional.empty();
            }
            var defaultPersonaId = defaultChild.getAttributeAsString("persona_id").orElse(null);
            if (defaultPersonaId == null) {
                return Optional.empty();
            }
            var sections = new ArrayList<Section>();
            for (var sectionNode : botChild.getChildren("section")) {
                var section = Section.of(sectionNode).orElse(null);
                if (section == null) {
                    return Optional.empty();
                }
                sections.add(section);
            }
            if (sections.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new SuccessV2("2", defaultJid, defaultPersonaId, sections));
        }

        /**
         * Compares this V2 reply to another for value equality.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link SuccessV2} with identical
         *         fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (SuccessV2) obj;
            return Objects.equals(this.botV, that.botV)
                    && Objects.equals(this.botDefaultJid, that.botDefaultJid)
                    && Objects.equals(this.botDefaultPersonaId, that.botDefaultPersonaId)
                    && Objects.equals(this.botSection, that.botSection);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(botV, botDefaultJid, botDefaultPersonaId, botSection);
        }

        /**
         * Returns a debug-friendly representation of this V2 reply.
         *
         * <p>The format is intended for logging and is not part of the contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxBotBotListResponse.SuccessV2[botV=" + botV
                    + ", botDefaultJid=" + botDefaultJid
                    + ", botDefaultPersonaId=" + botDefaultPersonaId
                    + ", botSection=" + botSection + ']';
        }

        /**
         * A single V2 directory section, grouping {@link BotEntry} entries under a typed
         * name.
         */
        public static final class Section {
            /**
             * The displayed section name.
             */
            private final String name;

            /**
             * The section-type discriminator.
             *
             * <p>Lets the UI label the section without inspecting the name string.
             */
            private final SmaxBotBotListSectionType type;

            /**
             * The bot entries belonging to this section.
             */
            private final List<BotEntry> bot;

            /**
             * Constructs a V2 section.
             *
             * <p>Invoked by {@link #of(Stanza)} after a successful parse.
             *
             * @implNote
             * This implementation defensively copies the entry list and substitutes an
             * empty list for {@code null}.
             *
             * @param name the section name; never {@code null}
             * @param type the section type; never {@code null}
             * @param bot  the bot entries; may be {@code null}, treated as empty
             * @throws NullPointerException if {@code name} or {@code type} is
             *                              {@code null}
             */
            public Section(String name, SmaxBotBotListSectionType type, List<BotEntry> bot) {
                this.name = Objects.requireNonNull(name, "name cannot be null");
                this.type = Objects.requireNonNull(type, "type cannot be null");
                this.bot = List.copyOf(Objects.requireNonNullElse(bot, List.of()));
            }

            /**
             * Returns the section name.
             *
             * @return the name; never {@code null}
             */
            public String name() {
                return name;
            }

            /**
             * Returns the section type.
             *
             * @return the type; never {@code null}
             */
            public SmaxBotBotListSectionType type() {
                return type;
            }

            /**
             * Returns the bot entries.
             *
             * @return an unmodifiable list; never {@code null}
             */
            public List<BotEntry> bot() {
                return bot;
            }

            /**
             * Parses a V2 section from the given {@code <section>} child.
             *
             * <p>Returns {@link Optional#empty()} for any deviation from the V2 section
             * schema.
             *
             * @param stanza the {@code <section>} child
             * @return an {@link Optional} carrying the parsed section
             */
            @WhatsAppWebExport(moduleName = "WASmaxInBotBotListResponseSuccessV2",
                    exports = "parseBotListResponseSuccessV2BotSection",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<Section> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("section")) {
                    return Optional.empty();
                }
                var name = stanza.getAttributeAsString("name").orElse(null);
                if (name == null) {
                    return Optional.empty();
                }
                var typeAttr = stanza.getAttributeAsString("type").orElse(null);
                if (typeAttr == null) {
                    return Optional.empty();
                }
                var type = SmaxBotBotListSectionType.ofWire(typeAttr).orElse(null);
                if (type == null) {
                    return Optional.empty();
                }
                var bots = new ArrayList<BotEntry>();
                for (var botNode : stanza.getChildren("bot")) {
                    var bot = BotEntry.of(botNode).orElse(null);
                    if (bot == null) {
                        return Optional.empty();
                    }
                    bots.add(bot);
                }
                return Optional.of(new Section(name, type, bots));
            }

            /**
             * Compares this section to another for value equality.
             *
             * @param obj the object to compare against
             * @return {@code true} when {@code obj} is a {@link Section} with identical
             *         fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Section) obj;
                return Objects.equals(this.name, that.name)
                        && this.type == that.type
                        && Objects.equals(this.bot, that.bot);
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(name, type, bot);
            }

            /**
             * Returns a debug-friendly representation of this section.
             *
             * <p>The format is intended for logging and is not part of the contract.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "SmaxBotBotListResponse.SuccessV2.Section[name=" + name
                        + ", type=" + type
                        + ", bot=" + bot + ']';
            }
        }

        /**
         * A single V2 bot entry within a section.
         */
        public static final class BotEntry {
            /**
             * The bot JID.
             */
            private final Jid jid;

            /**
             * The bot persona id.
             */
            private final String personaId;

            /**
             * The optional usage count.
             */
            private final Integer count;

            /**
             * The optional theme bundles.
             *
             * <p>V2 bot entries may carry up to two themes (one dark, one light) per the
             * WA Web schema bound.
             */
            private final List<ThemeBundle> theme;

            /**
             * Constructs a V2 bot entry.
             *
             * <p>Invoked by {@link #of(Stanza)} after a successful parse.
             *
             * @implNote
             * This implementation defensively copies the theme list and substitutes an
             * empty list for {@code null}.
             *
             * @param jid       the bot JID; never {@code null}
             * @param personaId the persona id; never {@code null}
             * @param count     the optional usage count; may be {@code null}
             * @param theme     the theme bundles; may be {@code null}, treated as empty
             * @throws NullPointerException if {@code jid} or {@code personaId} is
             *                              {@code null}
             */
            public BotEntry(Jid jid, String personaId, Integer count, List<ThemeBundle> theme) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.personaId = Objects.requireNonNull(personaId, "personaId cannot be null");
                this.count = count;
                this.theme = List.copyOf(Objects.requireNonNullElse(theme, List.of()));
            }

            /**
             * Returns the bot JID.
             *
             * @return the JID; never {@code null}
             */
            public Jid jid() {
                return jid;
            }

            /**
             * Returns the persona id.
             *
             * @return the persona id; never {@code null}
             */
            public String personaId() {
                return personaId;
            }

            /**
             * Returns the optional usage count.
             *
             * <p>Drives the bot-popularity sort hint in the directory UI.
             *
             * @return an {@link Optional} carrying the count, or {@link Optional#empty()}
             *         when omitted
             */
            public Optional<Integer> count() {
                return Optional.ofNullable(count);
            }

            /**
             * Returns the theme bundles.
             *
             * <p>Up to two entries; one dark, one light.
             *
             * @return an unmodifiable list; never {@code null}
             */
            public List<ThemeBundle> theme() {
                return theme;
            }

            /**
             * Parses a V2 bot entry from the given {@code <bot>} child.
             *
             * <p>Returns {@link Optional#empty()} for any deviation from the V2 entry
             * schema (invalid JID server, missing persona id, malformed count, more than
             * two themes).
             *
             * @param stanza the {@code <bot>} child
             * @return an {@link Optional} carrying the parsed entry
             */
            @WhatsAppWebExport(moduleName = "WASmaxInBotBotListResponseSuccessV2",
                    exports = "parseBotListResponseSuccessV2BotSectionBot",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<BotEntry> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("bot")) {
                    return Optional.empty();
                }
                var jid = stanza.getAttributeAsJid("jid").orElse(null);
                if (jid == null || !isUserJidServer(jid)) {
                    return Optional.empty();
                }
                var personaId = stanza.getAttributeAsString("persona_id").orElse(null);
                if (personaId == null) {
                    return Optional.empty();
                }
                Integer count = null;
                if (stanza.hasAttribute("count")) {
                    var countAttr = stanza.getAttributeAsInt("count");
                    if (countAttr.isEmpty()) {
                        return Optional.empty();
                    }
                    count = countAttr.getAsInt();
                }
                var themes = new ArrayList<ThemeBundle>();
                for (var themeNode : stanza.getChildren("theme")) {
                    var theme = ThemeBundle.of(themeNode).orElse(null);
                    if (theme == null) {
                        return Optional.empty();
                    }
                    themes.add(theme);
                }
                if (themes.size() > 2) {
                    return Optional.empty();
                }
                return Optional.of(new BotEntry(jid, personaId, count, themes));
            }

            /**
             * Compares this bot entry to another for value equality.
             *
             * @param obj the object to compare against
             * @return {@code true} when {@code obj} is a {@link BotEntry} with identical
             *         fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (BotEntry) obj;
                return Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.personaId, that.personaId)
                        && Objects.equals(this.count, that.count)
                        && Objects.equals(this.theme, that.theme);
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, personaId, count, theme);
            }

            /**
             * Returns a debug-friendly representation of this entry.
             *
             * <p>The format is intended for logging and is not part of the contract.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "SmaxBotBotListResponse.SuccessV2.BotEntry[jid=" + jid
                        + ", personaId=" + personaId
                        + ", count=" + count
                        + ", theme=" + theme + ']';
            }
        }

        /**
         * A V2 theme bundle: a mode discriminator plus the three colour element values.
         */
        public static final class ThemeBundle {
            /**
             * The theme-mode discriminator.
             */
            private final SmaxBotBotListThemeMode mode;

            /**
             * The {@code <background>} element value.
             */
            private final String backgroundElementValue;

            /**
             * The {@code <primary_text>} element value.
             */
            private final String primaryTextElementValue;

            /**
             * The {@code <secondary_text>} element value.
             */
            private final String secondaryTextElementValue;

            /**
             * Constructs a theme bundle.
             *
             * <p>Invoked by {@link #of(Stanza)} after a successful parse.
             *
             * @param mode                      the mode; never {@code null}
             * @param backgroundElementValue    the background value; never {@code null}
             * @param primaryTextElementValue   the primary-text value; never {@code null}
             * @param secondaryTextElementValue the secondary-text value; never
             *                                  {@code null}
             * @throws NullPointerException if any argument is {@code null}
             */
            public ThemeBundle(SmaxBotBotListThemeMode mode, String backgroundElementValue,
                               String primaryTextElementValue, String secondaryTextElementValue) {
                this.mode = Objects.requireNonNull(mode, "mode cannot be null");
                this.backgroundElementValue = Objects.requireNonNull(backgroundElementValue,
                        "backgroundElementValue cannot be null");
                this.primaryTextElementValue = Objects.requireNonNull(primaryTextElementValue,
                        "primaryTextElementValue cannot be null");
                this.secondaryTextElementValue = Objects.requireNonNull(secondaryTextElementValue,
                        "secondaryTextElementValue cannot be null");
            }

            /**
             * Returns the theme mode.
             *
             * @return the mode; never {@code null}
             */
            public SmaxBotBotListThemeMode mode() {
                return mode;
            }

            /**
             * Returns the background element value.
             *
             * @return the value; never {@code null}
             */
            public String backgroundElementValue() {
                return backgroundElementValue;
            }

            /**
             * Returns the primary-text element value.
             *
             * @return the value; never {@code null}
             */
            public String primaryTextElementValue() {
                return primaryTextElementValue;
            }

            /**
             * Returns the secondary-text element value.
             *
             * @return the value; never {@code null}
             */
            public String secondaryTextElementValue() {
                return secondaryTextElementValue;
            }

            /**
             * Parses a theme bundle from the given {@code <theme>} child.
             *
             * <p>Returns {@link Optional#empty()} for any deviation from the theme schema
             * (unknown mode literal, missing element children, empty element values).
             *
             * @param stanza the {@code <theme>} child
             * @return an {@link Optional} carrying the parsed bundle
             */
            @WhatsAppWebExport(moduleName = "WASmaxInBotBotListResponseSuccessV2",
                    exports = "parseBotListResponseSuccessV2BotSectionBotTheme",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<ThemeBundle> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("theme")) {
                    return Optional.empty();
                }
                var modeAttr = stanza.getAttributeAsString("mode").orElse(null);
                if (modeAttr == null) {
                    return Optional.empty();
                }
                var mode = SmaxBotBotListThemeMode.ofWire(modeAttr).orElse(null);
                if (mode == null) {
                    return Optional.empty();
                }
                var backgroundChild = stanza.getChild("background").orElse(null);
                if (backgroundChild == null) {
                    return Optional.empty();
                }
                var background = backgroundChild.toContentString().orElse(null);
                if (background == null) {
                    return Optional.empty();
                }
                var primaryChild = stanza.getChild("primary_text").orElse(null);
                if (primaryChild == null) {
                    return Optional.empty();
                }
                var primary = primaryChild.toContentString().orElse(null);
                if (primary == null) {
                    return Optional.empty();
                }
                var secondaryChild = stanza.getChild("secondary_text").orElse(null);
                if (secondaryChild == null) {
                    return Optional.empty();
                }
                var secondary = secondaryChild.toContentString().orElse(null);
                if (secondary == null) {
                    return Optional.empty();
                }
                return Optional.of(new ThemeBundle(mode, background, primary, secondary));
            }

            /**
             * Compares this theme bundle to another for value equality.
             *
             * @param obj the object to compare against
             * @return {@code true} when {@code obj} is a {@link ThemeBundle} with
             *         identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (ThemeBundle) obj;
                return this.mode == that.mode
                        && Objects.equals(this.backgroundElementValue, that.backgroundElementValue)
                        && Objects.equals(this.primaryTextElementValue, that.primaryTextElementValue)
                        && Objects.equals(this.secondaryTextElementValue, that.secondaryTextElementValue);
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(mode, backgroundElementValue,
                        primaryTextElementValue, secondaryTextElementValue);
            }

            /**
             * Returns a debug-friendly representation of this bundle.
             *
             * <p>The format is intended for logging and is not part of the contract.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "SmaxBotBotListResponse.SuccessV2.ThemeBundle[mode=" + mode
                        + ", backgroundElementValue=" + backgroundElementValue
                        + ", primaryTextElementValue=" + primaryTextElementValue
                        + ", secondaryTextElementValue=" + secondaryTextElementValue + ']';
            }
        }
    }

    /**
     * The current V3 directory reply, pinned by {@code v="3"} on the top-level
     * {@code <bot>} child.
     *
     * <p>Returned when the originating request did not pin a legacy revision; carries the
     * relay-side digest (for next-fetch short-circuiting), an optional bot-of-the-day
     * default, and a list of sections with display-type hints.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBotBotListResponseSuccessV3")
    @WhatsAppWebModule(moduleName = "WASmaxInBotIQResultResponseMixin")
    final class SuccessV3 implements SmaxBotBotListResponse {
        /**
         * The protocol revision; always the literal {@code "3"}.
         */
        private final String botV;

        /**
         * The relay-side directory digest.
         *
         * <p>Pass this back as the {@code botBhash} argument of the next
         * {@link SmaxBotBotListRequest} to short-circuit when the server-side directory
         * has not changed.
         */
        private final String botBhash;

        /**
         * The optional bot-of-the-day default entry.
         */
        private final DefaultEntry botDefault;

        /**
         * The directory sections; may be empty.
         */
        private final List<Section> botSection;

        /**
         * Constructs a V3 directory reply.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after a successful parse.
         *
         * @implNote
         * This implementation defensively copies the section list and substitutes an
         * empty list for {@code null}.
         *
         * @param botV       the protocol revision (always {@code "3"}); never
         *                   {@code null}
         * @param botBhash   the digest; never {@code null}
         * @param botDefault the optional default entry; may be {@code null}
         * @param botSection the section list; may be {@code null}, treated as empty
         * @throws NullPointerException if {@code botV} or {@code botBhash} is
         *                              {@code null}
         */
        public SuccessV3(String botV, String botBhash, DefaultEntry botDefault,
                         List<Section> botSection) {
            this.botV = Objects.requireNonNull(botV, "botV cannot be null");
            this.botBhash = Objects.requireNonNull(botBhash, "botBhash cannot be null");
            this.botDefault = botDefault;
            this.botSection = List.copyOf(Objects.requireNonNullElse(botSection, List.of()));
        }

        /**
         * Returns the protocol revision.
         *
         * @return the revision; always {@code "3"}; never {@code null}
         */
        public String botV() {
            return botV;
        }

        /**
         * Returns the relay-side directory digest.
         *
         * <p>Callers pass this as the {@code botBhash} input of the next refresh request.
         *
         * @return the digest; never {@code null}
         */
        public String botBhash() {
            return botBhash;
        }

        /**
         * Returns the optional default entry.
         *
         * <p>The relay omits the default when no bot is featured for the current cohort.
         *
         * @return an {@link Optional} carrying the entry, or {@link Optional#empty()}
         *         when omitted
         */
        public Optional<DefaultEntry> botDefault() {
            return Optional.ofNullable(botDefault);
        }

        /**
         * Returns the directory sections.
         *
         * <p>V3 admits an empty list, unlike V2 which requires at least one section.
         *
         * @return an unmodifiable list; never {@code null}
         */
        public List<Section> botSection() {
            return botSection;
        }

        /**
         * Parses a V3 reply from the given inbound stanza cross-checked against the
         * originating request.
         *
         * <p>Returns {@link Optional#empty()} for any deviation from the documented V3
         * schema (missing or wrong-version {@code <bot>}, missing digest, malformed
         * default entry, malformed section).
         *
         * @implNote
         * This implementation delegates IQ-envelope validation to
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}; the V3 schema admits an
         * empty section list, in contrast to V2.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound IQ stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBotBotListResponseSuccessV3",
                exports = "parseBotListResponseSuccessV3",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<SuccessV3> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var botChild = stanza.getChild("bot").orElse(null);
            if (botChild == null) {
                return Optional.empty();
            }
            if (!botChild.hasAttribute("v", "3")) {
                return Optional.empty();
            }
            var bhash = botChild.getAttributeAsString("bhash").orElse(null);
            if (bhash == null) {
                return Optional.empty();
            }
            DefaultEntry defaultEntry = null;
            var defaultChild = botChild.getChild("default").orElse(null);
            if (defaultChild != null) {
                defaultEntry = DefaultEntry.of(defaultChild).orElse(null);
                if (defaultEntry == null) {
                    return Optional.empty();
                }
            }
            var sections = new ArrayList<Section>();
            for (var sectionNode : botChild.getChildren("section")) {
                var section = Section.of(sectionNode).orElse(null);
                if (section == null) {
                    return Optional.empty();
                }
                sections.add(section);
            }
            return Optional.of(new SuccessV3("3", bhash, defaultEntry, sections));
        }

        /**
         * Compares this V3 reply to another for value equality.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link SuccessV3} with identical
         *         fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (SuccessV3) obj;
            return Objects.equals(this.botV, that.botV)
                    && Objects.equals(this.botBhash, that.botBhash)
                    && Objects.equals(this.botDefault, that.botDefault)
                    && Objects.equals(this.botSection, that.botSection);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(botV, botBhash, botDefault, botSection);
        }

        /**
         * Returns a debug-friendly representation of this V3 reply.
         *
         * <p>The format is intended for logging and is not part of the contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxBotBotListResponse.SuccessV3[botV=" + botV
                    + ", botBhash=" + botBhash
                    + ", botDefault=" + botDefault
                    + ", botSection=" + botSection + ']';
        }

        /**
         * The V3 bot-of-the-day default entry; carries the JID and persona id only.
         */
        public static final class DefaultEntry {
            /**
             * The bot JID.
             */
            private final Jid jid;

            /**
             * The bot persona id.
             */
            private final String personaId;

            /**
             * Constructs a V3 default entry.
             *
             * <p>Invoked by {@link #of(Stanza)} after a successful parse.
             *
             * @param jid       the JID; never {@code null}
             * @param personaId the persona id; never {@code null}
             * @throws NullPointerException if either argument is {@code null}
             */
            public DefaultEntry(Jid jid, String personaId) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.personaId = Objects.requireNonNull(personaId, "personaId cannot be null");
            }

            /**
             * Returns the JID.
             *
             * @return the JID; never {@code null}
             */
            public Jid jid() {
                return jid;
            }

            /**
             * Returns the persona id.
             *
             * @return the id; never {@code null}
             */
            public String personaId() {
                return personaId;
            }

            /**
             * Parses a default entry from the given {@code <default>} child.
             *
             * <p>Returns {@link Optional#empty()} for any deviation from the V3
             * default-entry schema.
             *
             * @param stanza the {@code <default>} child
             * @return an {@link Optional} carrying the parsed entry
             */
            @WhatsAppWebExport(moduleName = "WASmaxInBotBotListResponseSuccessV3",
                    exports = "parseBotListResponseSuccessV3BotDefault",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<DefaultEntry> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("default")) {
                    return Optional.empty();
                }
                var jid = stanza.getAttributeAsJid("jid").orElse(null);
                if (jid == null || !isUserJidServer(jid)) {
                    return Optional.empty();
                }
                var personaId = stanza.getAttributeAsString("persona_id").orElse(null);
                if (personaId == null) {
                    return Optional.empty();
                }
                return Optional.of(new DefaultEntry(jid, personaId));
            }

            /**
             * Compares this default entry to another for value equality.
             *
             * @param obj the object to compare against
             * @return {@code true} when {@code obj} is a {@link DefaultEntry} with
             *         identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (DefaultEntry) obj;
                return Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.personaId, that.personaId);
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, personaId);
            }

            /**
             * Returns a debug-friendly representation of this entry.
             *
             * <p>The format is intended for logging and is not part of the contract.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "SmaxBotBotListResponse.SuccessV3.DefaultEntry[jid=" + jid
                        + ", personaId=" + personaId + ']';
            }
        }

        /**
         * A V3 directory section, adding a {@link SmaxBotBotListSectionDisplayType} layout
         * hint to the V2 (name, type, bot list) shape.
         */
        public static final class Section {
            /**
             * The section name.
             */
            private final String name;

            /**
             * The section-type discriminator.
             */
            private final SmaxBotBotListSectionType type;

            /**
             * The render-mode discriminator.
             *
             * <p>Drives whether the UI shows this section as a list, a horizontal
             * scroller, or omits it entirely.
             */
            private final SmaxBotBotListSectionDisplayType displayType;

            /**
             * The bot entries; may be empty.
             */
            private final List<BotEntry> bot;

            /**
             * Constructs a V3 section.
             *
             * <p>Invoked by {@link #of(Stanza)} after a successful parse.
             *
             * @implNote
             * This implementation defensively copies the entry list and substitutes an
             * empty list for {@code null}.
             *
             * @param name        the section name; never {@code null}
             * @param type        the section type; never {@code null}
             * @param displayType the render mode; never {@code null}
             * @param bot         the bot entries; may be {@code null}, treated as empty
             * @throws NullPointerException if {@code name}, {@code type}, or
             *                              {@code displayType} is {@code null}
             */
            public Section(String name, SmaxBotBotListSectionType type, SmaxBotBotListSectionDisplayType displayType,
                           List<BotEntry> bot) {
                this.name = Objects.requireNonNull(name, "name cannot be null");
                this.type = Objects.requireNonNull(type, "type cannot be null");
                this.displayType = Objects.requireNonNull(displayType, "displayType cannot be null");
                this.bot = List.copyOf(Objects.requireNonNullElse(bot, List.of()));
            }

            /**
             * Returns the section name.
             *
             * @return the name; never {@code null}
             */
            public String name() {
                return name;
            }

            /**
             * Returns the section type.
             *
             * @return the type; never {@code null}
             */
            public SmaxBotBotListSectionType type() {
                return type;
            }

            /**
             * Returns the render-mode discriminator.
             *
             * @return the discriminator; never {@code null}
             */
            public SmaxBotBotListSectionDisplayType displayType() {
                return displayType;
            }

            /**
             * Returns the bot entries.
             *
             * <p>V3 admits an empty list, for instance a placeholder section with
             * {@code display_type="hidden"}.
             *
             * @return an unmodifiable list; never {@code null}
             */
            public List<BotEntry> bot() {
                return bot;
            }

            /**
             * Parses a V3 section from the given {@code <section>} child.
             *
             * <p>Returns {@link Optional#empty()} for any deviation from the V3 section
             * schema.
             *
             * @param stanza the {@code <section>} child
             * @return an {@link Optional} carrying the parsed section
             */
            @WhatsAppWebExport(moduleName = "WASmaxInBotBotListResponseSuccessV3",
                    exports = "parseBotListResponseSuccessV3BotSection",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<Section> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("section")) {
                    return Optional.empty();
                }
                var name = stanza.getAttributeAsString("name").orElse(null);
                if (name == null) {
                    return Optional.empty();
                }
                var typeAttr = stanza.getAttributeAsString("type").orElse(null);
                if (typeAttr == null) {
                    return Optional.empty();
                }
                var type = SmaxBotBotListSectionType.ofWire(typeAttr).orElse(null);
                if (type == null) {
                    return Optional.empty();
                }
                var displayTypeAttr = stanza.getAttributeAsString("display_type").orElse(null);
                if (displayTypeAttr == null) {
                    return Optional.empty();
                }
                var displayType = SmaxBotBotListSectionDisplayType.ofWire(displayTypeAttr).orElse(null);
                if (displayType == null) {
                    return Optional.empty();
                }
                var bots = new ArrayList<BotEntry>();
                for (var botNode : stanza.getChildren("bot")) {
                    var bot = BotEntry.of(botNode).orElse(null);
                    if (bot == null) {
                        return Optional.empty();
                    }
                    bots.add(bot);
                }
                return Optional.of(new Section(name, type, displayType, bots));
            }

            /**
             * Compares this section to another for value equality.
             *
             * @param obj the object to compare against
             * @return {@code true} when {@code obj} is a {@link Section} with identical
             *         fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Section) obj;
                return Objects.equals(this.name, that.name)
                        && this.type == that.type
                        && this.displayType == that.displayType
                        && Objects.equals(this.bot, that.bot);
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(name, type, displayType, bot);
            }

            /**
             * Returns a debug-friendly representation of this section.
             *
             * <p>The format is intended for logging and is not part of the contract.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "SmaxBotBotListResponse.SuccessV3.Section[name=" + name
                        + ", type=" + type
                        + ", displayType=" + displayType
                        + ", bot=" + bot + ']';
            }
        }

        /**
         * A V3 bot entry: JID, persona id, optional card title and usage count.
         *
         * <p>V3 drops V2's per-entry theme overrides.
         */
        public static final class BotEntry {
            /**
             * The bot JID.
             */
            private final Jid jid;

            /**
             * The bot persona id.
             */
            private final String personaId;

            /**
             * The optional card title.
             *
             * <p>Used by the directory UI as the headline label for the bot card;
             * {@code null} falls back to the persona name.
             */
            private final String cardTitle;

            /**
             * The optional usage count.
             */
            private final Integer count;

            /**
             * Constructs a V3 bot entry.
             *
             * <p>Invoked by {@link #of(Stanza)} after a successful parse.
             *
             * @param jid       the JID; never {@code null}
             * @param personaId the persona id; never {@code null}
             * @param cardTitle the optional card title; may be {@code null}
             * @param count     the optional usage count; may be {@code null}
             * @throws NullPointerException if {@code jid} or {@code personaId} is
             *                              {@code null}
             */
            public BotEntry(Jid jid, String personaId, String cardTitle, Integer count) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.personaId = Objects.requireNonNull(personaId, "personaId cannot be null");
                this.cardTitle = cardTitle;
                this.count = count;
            }

            /**
             * Returns the bot JID.
             *
             * @return the JID; never {@code null}
             */
            public Jid jid() {
                return jid;
            }

            /**
             * Returns the persona id.
             *
             * @return the id; never {@code null}
             */
            public String personaId() {
                return personaId;
            }

            /**
             * Returns the optional card title.
             *
             * @return an {@link Optional} carrying the title, or {@link Optional#empty()}
             *         when omitted
             */
            public Optional<String> cardTitle() {
                return Optional.ofNullable(cardTitle);
            }

            /**
             * Returns the optional usage count.
             *
             * @return an {@link Optional} carrying the count, or {@link Optional#empty()}
             *         when omitted
             */
            public Optional<Integer> count() {
                return Optional.ofNullable(count);
            }

            /**
             * Parses a V3 bot entry from the given {@code <bot>} child.
             *
             * <p>Returns {@link Optional#empty()} for any deviation from the V3 entry
             * schema.
             *
             * @param stanza the {@code <bot>} child
             * @return an {@link Optional} carrying the parsed entry
             */
            @WhatsAppWebExport(moduleName = "WASmaxInBotBotListResponseSuccessV3",
                    exports = "parseBotListResponseSuccessV3BotSectionBot",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<BotEntry> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("bot")) {
                    return Optional.empty();
                }
                var jid = stanza.getAttributeAsJid("jid").orElse(null);
                if (jid == null || !isUserJidServer(jid)) {
                    return Optional.empty();
                }
                var personaId = stanza.getAttributeAsString("persona_id").orElse(null);
                if (personaId == null) {
                    return Optional.empty();
                }
                var cardTitle = stanza.getAttributeAsString("card_title").orElse(null);
                Integer count = null;
                if (stanza.hasAttribute("count")) {
                    var countAttr = stanza.getAttributeAsInt("count");
                    if (countAttr.isEmpty()) {
                        return Optional.empty();
                    }
                    count = countAttr.getAsInt();
                }
                return Optional.of(new BotEntry(jid, personaId, cardTitle, count));
            }

            /**
             * Compares this bot entry to another for value equality.
             *
             * @param obj the object to compare against
             * @return {@code true} when {@code obj} is a {@link BotEntry} with identical
             *         fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (BotEntry) obj;
                return Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.personaId, that.personaId)
                        && Objects.equals(this.cardTitle, that.cardTitle)
                        && Objects.equals(this.count, that.count);
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, personaId, cardTitle, count);
            }

            /**
             * Returns a debug-friendly representation of this entry.
             *
             * <p>The format is intended for logging and is not part of the contract.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "SmaxBotBotListResponse.SuccessV3.BotEntry[jid=" + jid
                        + ", personaId=" + personaId
                        + ", cardTitle=" + cardTitle
                        + ", count=" + count + ']';
            }
        }
    }

    /**
     * The error reply variant carrying the rejection code-text pair.
     *
     * <p>Surfaced when the relay rejected the directory fetch. The bot domain admits four
     * documented variants ({@code internal-server-error/500}, {@code forbidden/403},
     * {@code bad-request/400}, {@code not-allowed/405}); this Cobalt model collapses them
     * into a single {@code (errorCode, errorText)} pair, leaving disambiguation to the
     * caller.
     *
     * @implNote
     * This implementation routes server (5xx) errors through
     * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} and client (4xx)
     * errors through {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)};
     * variant identity is preserved through the code-text pair without a separate
     * variant-name field.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBotBotListResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBotBotListErrors")
    final class Error implements SmaxBotBotListResponse {
        /**
         * The numeric error code.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text.
         */
        private final String errorText;

        /**
         * Constructs an error reply.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after a successful parse.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public Error(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * <p>One of {@code 400}, {@code 403}, {@code 405}, or {@code 500}.
         *
         * @return the code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when
         *         omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses an {@code Error} reply from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when neither the server-error nor the
         * client-error envelope matched.
         *
         * @implNote
         * This implementation tries the 5xx server-error envelope first, then falls
         * through to the 4xx client-error envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound IQ stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBotBotListResponseError",
                exports = "parseBotListResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBotBotListErrors",
                exports = "parseBotListErrors",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Error> of(Stanza stanza, Stanza request) {
            var serverEnvelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (serverEnvelope != null) {
                return Optional.of(new Error(serverEnvelope.code(), serverEnvelope.text()));
            }
            var clientEnvelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (clientEnvelope != null) {
                return Optional.of(new Error(clientEnvelope.code(), clientEnvelope.text()));
            }
            return Optional.empty();
        }

        /**
         * Compares this error reply to another for value equality on the code-text pair.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an {@link Error} with identical code
         *         and text
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Error) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug-friendly representation of this error reply.
         *
         * <p>The format is intended for logging and is not part of the contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxBotBotListResponse.Error[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
