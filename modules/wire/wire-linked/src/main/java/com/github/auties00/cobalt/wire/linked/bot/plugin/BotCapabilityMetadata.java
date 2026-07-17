package com.github.auties00.cobalt.wire.linked.bot.plugin;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Describes the rendering capabilities that a WhatsApp client advertises to the
 * Meta AI bot server, so that the server can tailor its rich responses to match
 * what the client is able to display.
 *
 * <p>When sending a message to a Meta AI bot, the client includes a list of
 * {@link BotCapabilityType} values in this metadata. Each value declares
 * support for a specific rich-response feature such as tables, code blocks,
 * LaTeX rendering, inline images, or interactive maps. The server inspects
 * these capabilities and omits or downgrades any content the client cannot
 * render. This metadata is carried inside the parent {@code BotMetadata}
 * protobuf attached to the message.
 *
 * @see BotPluginMetadata
 */
@ProtobufMessage(name = "BotCapabilityMetadata")
public final class BotCapabilityMetadata {
    /**
     * The list of rendering capabilities that the client declares support for.
     * Each entry identifies a specific rich-response feature the client can
     * display, such as tables, code blocks, or inline images.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    List<BotCapabilityType> capabilities;

    /**
     * Constructs a new {@code BotCapabilityMetadata} with the specified list
     * of capabilities.
     *
     * @param capabilities the list of rendering capabilities the client
     *                     supports, or {@code null} if none are declared
     */
    BotCapabilityMetadata(List<BotCapabilityType> capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * Returns the list of rendering capabilities the client supports.
     *
     * @return an unmodifiable list of {@link BotCapabilityType} values, or an
     *         empty list if none are declared
     */
    public List<BotCapabilityType> capabilities() {
        return capabilities == null ? List.of() : Collections.unmodifiableList(capabilities);
    }

    /**
     * Sets the list of rendering capabilities the client supports.
     *
     * @param capabilities the new list of capabilities, or {@code null} to
     *                     clear all declared capabilities
     */
    public void setCapabilities(List<BotCapabilityType> capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * Enumerates the rendering capabilities that a WhatsApp client can
     * advertise to the Meta AI bot server.
     *
     * <p>Each constant represents a specific rich-response feature. The client
     * includes the relevant constants in a {@link BotCapabilityMetadata} to
     * inform the server which content formats it can display. Capabilities are
     * organized into several functional areas:
     * <ul>
     * <li>Core rendering: {@link #PROGRESS_INDICATOR},
     *     {@link #RICH_RESPONSE_HEADING}, {@link #RICH_RESPONSE_SUB_HEADING},
     *     {@link #RICH_RESPONSE_NESTED_LIST}
     * <li>Media: {@link #RICH_RESPONSE_INLINE_IMAGE},
     *     {@link #RICH_RESPONSE_GRID_IMAGE},
     *     {@link #RICH_RESPONSE_GRID_IMAGE_3P},
     *     {@link #RICH_RESPONSE_INLINE_REELS}
     * <li>Structured content: {@link #RICH_RESPONSE_TABLE},
     *     {@link #RICH_RESPONSE_CODE}, {@link #RICH_RESPONSE_LATEX},
     *     {@link #RICH_RESPONSE_LATEX_INLINE}, {@link #RICH_RESPONSE_MAPS}
     * <li>AI features: {@link #AI_MEMORY}, {@link #AI_SHARED_MEMORY},
     *     {@link #AI_STUDIO_UGC_MEMORY}, {@link #AGENTIC_PLANNING},
     *     {@link #QUERY_PLAN}
     * <li>Unified response: {@link #RICH_RESPONSE_UNIFIED_RESPONSE},
     *     {@link #RICH_RESPONSE_UNIFIED_TEXT_COMPONENT},
     *     {@link #RICH_RESPONSE_UNIFIED_SOURCES},
     *     {@link #RICH_RESPONSE_UNIFIED_DOMAIN_CITATIONS}
     * <li>Account and session: {@link #ACCOUNT_LINKING},
     *     {@link #SESSION_TRANSPARENCY_SYSTEM_MESSAGE},
     *     {@link #AI_RESPONSE_MODEL_BRANDING}
     * </ul>
     */
    @ProtobufEnum(name = "BotCapabilityMetadata.BotCapabilityType")
    public static enum BotCapabilityType {
        /**
         * An unknown or unrecognized capability.
         */
        UNKNOWN(0),

        /**
         * Support for rendering a typing/progress indicator while the bot
         * generates its response.
         */
        PROGRESS_INDICATOR(1),

        /**
         * Support for rendering headings in rich responses.
         */
        RICH_RESPONSE_HEADING(2),

        /**
         * Support for rendering nested (indented) lists in rich responses.
         */
        RICH_RESPONSE_NESTED_LIST(3),

        /**
         * Support for the AI personalization memory feature.
         */
        AI_MEMORY(4),

        /**
         * Support for surfing/navigating through previous AI conversation
         * threads.
         */
        RICH_RESPONSE_THREAD_SURFING(5),

        /**
         * Support for rendering tables in rich responses.
         */
        RICH_RESPONSE_TABLE(6),

        /**
         * Support for rendering syntax-highlighted code blocks in rich
         * responses.
         */
        RICH_RESPONSE_CODE(7),

        /**
         * Support for rendering structured (formatted) responses.
         */
        RICH_RESPONSE_STRUCTURED_RESPONSE(8),

        /**
         * Support for rendering inline images in rich responses.
         */
        RICH_RESPONSE_INLINE_IMAGE(9),

        /**
         * Control flag for WhatsApp/Instagram first-party plugin ranking.
         */
        WA_IG_1P_PLUGIN_RANKING_CONTROL(10),

        /**
         * Plugin ranking update slot 1.
         */
        WA_IG_1P_PLUGIN_RANKING_UPDATE_1(11),

        /**
         * Plugin ranking update slot 2.
         */
        WA_IG_1P_PLUGIN_RANKING_UPDATE_2(12),

        /**
         * Plugin ranking update slot 3.
         */
        WA_IG_1P_PLUGIN_RANKING_UPDATE_3(13),

        /**
         * Plugin ranking update slot 4.
         */
        WA_IG_1P_PLUGIN_RANKING_UPDATE_4(14),

        /**
         * Plugin ranking update slot 5.
         */
        WA_IG_1P_PLUGIN_RANKING_UPDATE_5(15),

        /**
         * Plugin ranking update slot 6.
         */
        WA_IG_1P_PLUGIN_RANKING_UPDATE_6(16),

        /**
         * Plugin ranking update slot 7.
         */
        WA_IG_1P_PLUGIN_RANKING_UPDATE_7(17),

        /**
         * Plugin ranking update slot 8.
         */
        WA_IG_1P_PLUGIN_RANKING_UPDATE_8(18),

        /**
         * Plugin ranking update slot 9.
         */
        WA_IG_1P_PLUGIN_RANKING_UPDATE_9(19),

        /**
         * Plugin ranking update slot 10.
         */
        WA_IG_1P_PLUGIN_RANKING_UPDATE_10(20),

        /**
         * Support for rendering sub-headings in rich responses.
         */
        RICH_RESPONSE_SUB_HEADING(21),

        /**
         * Support for rendering image grids in rich responses.
         */
        RICH_RESPONSE_GRID_IMAGE(22),

        /**
         * Support for the AI Studio user-generated content memory feature.
         */
        AI_STUDIO_UGC_MEMORY(23),

        /**
         * Support for rendering LaTeX mathematical expressions in rich
         * responses.
         */
        RICH_RESPONSE_LATEX(24),

        /**
         * Support for rendering interactive maps in rich responses.
         */
        RICH_RESPONSE_MAPS(25),

        /**
         * Support for rendering inline Instagram Reels in rich responses.
         */
        RICH_RESPONSE_INLINE_REELS(26),

        /**
         * Support for agentic planning (multi-step task execution with status
         * updates).
         */
        AGENTIC_PLANNING(27),

        /**
         * Support for external account linking within the bot UI.
         */
        ACCOUNT_LINKING(28),

        /**
         * Support for streaming disaggregation (breaking a streaming response
         * into discrete parts).
         */
        STREAMING_DISAGGREGATION(29),

        /**
         * Support for rendering third-party image grids in rich responses.
         */
        RICH_RESPONSE_GRID_IMAGE_3P(30),

        /**
         * Support for rendering inline LaTeX expressions within text.
         */
        RICH_RESPONSE_LATEX_INLINE(31),

        /**
         * Support for displaying a query plan (the bot's intended steps to
         * answer a query).
         */
        QUERY_PLAN(32),

        /**
         * Support for bot-initiated proactive messages.
         */
        PROACTIVE_MESSAGE(33),

        /**
         * Support for the unified rich response rendering format.
         */
        RICH_RESPONSE_UNIFIED_RESPONSE(34),

        /**
         * Support for promotional messages within the bot UI.
         */
        PROMOTION_MESSAGE(35),

        /**
         * Support for the simplified bot profile page.
         */
        SIMPLIFIED_PROFILE_PAGE(36),

        /**
         * Support for rendering source citations inline in bot messages.
         */
        RICH_RESPONSE_SOURCES_IN_MESSAGE(37),

        /**
         * Support for side-by-side comparison surveys in rich responses.
         */
        RICH_RESPONSE_SIDE_BY_SIDE_SURVEY(38),

        /**
         * Support for unified text components in rich responses.
         */
        RICH_RESPONSE_UNIFIED_TEXT_COMPONENT(39),

        /**
         * Support for the AI shared memory feature (memory visible across
         * conversations).
         */
        AI_SHARED_MEMORY(40),

        /**
         * Support for rendering unified source citations in rich responses.
         */
        RICH_RESPONSE_UNIFIED_SOURCES(41),

        /**
         * Support for rendering domain-level citations in unified rich
         * responses.
         */
        RICH_RESPONSE_UNIFIED_DOMAIN_CITATIONS(42),

        /**
         * Support for rendering inline Reels in unified responses.
         */
        RICH_RESPONSE_UR_INLINE_REELS_ENABLED(43),

        /**
         * Support for rendering media grids in unified responses.
         */
        RICH_RESPONSE_UR_MEDIA_GRID_ENABLED(44),

        /**
         * Support for timestamp placeholders in unified responses.
         */
        RICH_RESPONSE_UR_TIMESTAMP_PLACEHOLDER(45),

        /**
         * Support for in-app surveys embedded in rich responses.
         */
        RICH_RESPONSE_IN_APP_SURVEY(46),

        /**
         * Support for displaying AI model branding in responses.
         */
        AI_RESPONSE_MODEL_BRANDING(47),

        /**
         * Support for session transparency system messages (disclosing that
         * the user is chatting with AI).
         */
        SESSION_TRANSPARENCY_SYSTEM_MESSAGE(48),

        /**
         * Support for rendering reasoning/thinking steps in unified responses.
         */
        RICH_RESPONSE_UR_REASONING(49),

        /**
         * Support for zeitgeist (trending topic) citations in unified
         * responses.
         */
        RICH_RESPONSE_UR_ZEITGEIST_CITATIONS(50),

        /**
         * Support for zeitgeist (trending topic) carousel in unified
         * responses.
         */
        RICH_RESPONSE_UR_ZEITGEIST_CAROUSEL(51),

        /**
         * Support for the AI Imagine loading indicator animation.
         */
        AI_IMAGINE_LOADING_INDICATOR(52),

        /**
         * Support for rendering Imagine (image generation) results in unified
         * responses.
         */
        RICH_RESPONSE_UR_IMAGINE(53),

        /**
         * Support for the transition loading indicator from unified response
         * Imagine to native Imagine.
         */
        AI_IMAGINE_UR_TO_NATIVE_LOADING_INDICATOR(54),

        /**
         * Support for Bloks (Meta's UI framework) rendering in unified
         * responses.
         */
        RICH_RESPONSE_UR_BLOKS_ENABLED(55),

        /**
         * Support for inline hyperlinks in rich responses.
         */
        RICH_RESPONSE_INLINE_LINKS_ENABLED(56);

        /**
         * Constructs a new capability type constant.
         *
         * @param index the protobuf-assigned numeric index for this constant
         */
        BotCapabilityType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf-assigned numeric index for this constant.
         */
        final int index;

        /**
         * Returns the protobuf-assigned numeric index for this constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }
}
