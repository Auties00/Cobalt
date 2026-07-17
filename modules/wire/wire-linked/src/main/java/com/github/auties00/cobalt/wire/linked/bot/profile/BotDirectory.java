package com.github.auties00.cobalt.wire.linked.bot.profile;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries the parsed bot-directory listing returned by the relay.
 *
 * <p>WhatsApp ships two protocol revisions of the directory wire format
 * (V2 and V3). This carrier flattens both revisions into a caller-facing
 * shape: the protocol revision is preserved on
 * {@link #protocolVersion()}, the V3-only digest is exposed via
 * {@link #digest()}, and the directory body is split between the
 * optional "bot of the day" {@link #defaultBot() default entry} and the
 * curated list of {@link #sections() sections}.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it surfaces
 * the parsed reply to caller code and never travels on the wire.
 */
public final class BotDirectory {
    /**
     * The protocol revision string ({@code "2"} or {@code "3"}).
     */
    private final String protocolVersion;

    /**
     * The relay-side directory digest. Always present on V3 entries,
     * {@code null} for V2 entries.
     */
    private final String digest;

    /**
     * The optional "bot of the day" default entry.
     */
    private final DefaultBot defaultBot;

    /**
     * The directory sections.
     */
    private final List<Section> sections;

    /**
     * Constructs a new bot directory.
     *
     * @param protocolVersion the protocol revision; never {@code null}
     * @param digest          the optional digest; may be {@code null}
     * @param defaultBot      the optional default entry; may be
     *                        {@code null}
     * @param sections        the directory sections; never {@code null}
     * @throws NullPointerException if {@code protocolVersion} or
     *                              {@code sections} are {@code null}
     */
    public BotDirectory(String protocolVersion, String digest,
                        DefaultBot defaultBot, List<Section> sections) {
        this.protocolVersion = Objects.requireNonNull(protocolVersion, "protocolVersion cannot be null");
        this.digest = digest;
        this.defaultBot = defaultBot;
        Objects.requireNonNull(sections, "sections cannot be null");
        this.sections = List.copyOf(sections);
    }

    /**
     * Returns the protocol revision (typically {@code "2"} or
     * {@code "3"}).
     *
     * @return the revision; never {@code null}
     */
    public String protocolVersion() {
        return protocolVersion;
    }

    /**
     * Returns the directory digest. Present only on V3 entries.
     *
     * @return an {@link Optional} carrying the digest
     */
    public Optional<String> digest() {
        return Optional.ofNullable(digest);
    }

    /**
     * Returns the optional "bot of the day" default entry.
     *
     * @return an {@link Optional} carrying the default entry
     */
    public Optional<DefaultBot> defaultBot() {
        return Optional.ofNullable(defaultBot);
    }

    /**
     * Returns the directory sections.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<Section> sections() {
        return sections;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (BotDirectory) obj;
        return Objects.equals(this.protocolVersion, that.protocolVersion)
                && Objects.equals(this.digest, that.digest)
                && Objects.equals(this.defaultBot, that.defaultBot)
                && Objects.equals(this.sections, that.sections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocolVersion, digest, defaultBot, sections);
    }

    @Override
    public String toString() {
        return "BotDirectory[protocolVersion=" + protocolVersion
                + ", digest=" + digest
                + ", defaultBot=" + defaultBot
                + ", sections=" + sections + ']';
    }

    /**
     * The "bot of the day" default entry. Carries the bot JID and
     * persona id only.
     */
    public static final class DefaultBot {
        /**
         * The default-entry bot JID.
         */
        private final Jid jid;

        /**
         * The default-entry persona id.
         */
        private final String personaId;

        /**
         * Constructs a new default entry.
         *
         * @param jid       the bot JID; never {@code null}
         * @param personaId the persona id; never {@code null}
         * @throws NullPointerException if any argument is {@code null}
         */
        public DefaultBot(Jid jid, String personaId) {
            this.jid = Objects.requireNonNull(jid, "jid cannot be null");
            this.personaId = Objects.requireNonNull(personaId, "personaId cannot be null");
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

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (DefaultBot) obj;
            return Objects.equals(this.jid, that.jid)
                    && Objects.equals(this.personaId, that.personaId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jid, personaId);
        }

        @Override
        public String toString() {
            return "BotDirectory.DefaultBot[jid=" + jid
                    + ", personaId=" + personaId + ']';
        }
    }

    /**
     * A single directory section. Carries the section name plus the
     * curated list of {@link BotEntry bot entries}.
     */
    public static final class Section {
        /**
         * The section name (free-form, displayed verbatim in the
         * directory UI).
         */
        private final String name;

        /**
         * The wire-format section type discriminator (e.g.
         * {@code "all"}, {@code "category"}, {@code "featured"}).
         */
        private final String type;

        /**
         * The optional V3 display-type discriminator (e.g.
         * {@code "hscroll"}, {@code "list_view"}). Always {@code null}
         * on V2 sections.
         */
        private final String displayType;

        /**
         * The bot entries in this section.
         */
        private final List<BotEntry> bots;

        /**
         * Constructs a new section.
         *
         * @param name        the section name; never {@code null}
         * @param type        the section type; never {@code null}
         * @param displayType the optional display type; may be
         *                    {@code null}
         * @param bots        the bot entries; never {@code null}
         * @throws NullPointerException if {@code name}, {@code type},
         *                              or {@code bots} are {@code null}
         */
        public Section(String name, String type, String displayType, List<BotEntry> bots) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            this.type = Objects.requireNonNull(type, "type cannot be null");
            this.displayType = displayType;
            Objects.requireNonNull(bots, "bots cannot be null");
            this.bots = List.copyOf(bots);
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
         * Returns the section type discriminator.
         *
         * @return the type; never {@code null}
         */
        public String type() {
            return type;
        }

        /**
         * Returns the optional display-type discriminator.
         *
         * @return an {@link Optional} carrying the display type
         */
        public Optional<String> displayType() {
            return Optional.ofNullable(displayType);
        }

        /**
         * Returns the bot entries in this section.
         *
         * @return an unmodifiable list; never {@code null}
         */
        public List<BotEntry> bots() {
            return bots;
        }

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
                    && Objects.equals(this.type, that.type)
                    && Objects.equals(this.displayType, that.displayType)
                    && Objects.equals(this.bots, that.bots);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, displayType, bots);
        }

        @Override
        public String toString() {
            return "BotDirectory.Section[name=" + name
                    + ", type=" + type
                    + ", displayType=" + displayType
                    + ", bots=" + bots + ']';
        }
    }

    /**
     * A single bot entry in a section. Carries the bot JID, persona
     * id, optional V3 card title, and optional usage count.
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
         * The optional V3-only card title.
         */
        private final String cardTitle;

        /**
         * The optional usage count.
         */
        private final Integer usageCount;

        /**
         * Constructs a new bot entry.
         *
         * @param jid        the bot JID; never {@code null}
         * @param personaId  the persona id; never {@code null}
         * @param cardTitle  the optional card title; may be
         *                   {@code null}
         * @param usageCount the optional usage count; may be
         *                   {@code null}
         * @throws NullPointerException if {@code jid} or
         *                              {@code personaId} are
         *                              {@code null}
         */
        public BotEntry(Jid jid, String personaId, String cardTitle, Integer usageCount) {
            this.jid = Objects.requireNonNull(jid, "jid cannot be null");
            this.personaId = Objects.requireNonNull(personaId, "personaId cannot be null");
            this.cardTitle = cardTitle;
            this.usageCount = usageCount;
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
         * Returns the optional card title.
         *
         * @return an {@link Optional} carrying the card title
         */
        public Optional<String> cardTitle() {
            return Optional.ofNullable(cardTitle);
        }

        /**
         * Returns the optional usage count.
         *
         * @return an {@link Optional} carrying the usage count
         */
        public Optional<Integer> usageCount() {
            return Optional.ofNullable(usageCount);
        }

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
                    && Objects.equals(this.usageCount, that.usageCount);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jid, personaId, cardTitle, usageCount);
        }

        @Override
        public String toString() {
            return "BotDirectory.BotEntry[jid=" + jid
                    + ", personaId=" + personaId
                    + ", cardTitle=" + cardTitle
                    + ", usageCount=" + usageCount + ']';
        }
    }
}
