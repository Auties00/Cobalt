package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Configured state of a WhatsApp Business AI agent.
 *
 * <p>The WhatsApp Business AI agent is the auto-reply assistant a merchant
 * attaches to their business account: it answers incoming chats from the
 * knowledge the merchant has taught it. This model is the agent's "home"
 * view, gathering everything that describes how the assistant is set up.
 *
 * <p>The view has several facets, and a given read populates only the facet
 * it requested:
 * <ul>
 *   <li>{@link #abilities()} lists the capabilities the assistant can offer
 *       and whether each is available to the account.</li>
 *   <li>{@link #knowledgeEntries()} lists the ordered knowledge the
 *       assistant answers from (free-text statements and
 *       frequently-asked-question entries), {@link #websiteKnowledge()}
 *       carries the website-backed material and featured products, and
 *       {@link #productInfo()} lists the structured products the assistant
 *       can describe; {@link #productInfoEligible()} reports whether the
 *       product-information feature is unlocked for the account.</li>
 *   <li>{@link #knowledgeSources()} lists the configured sources the
 *       assistant learns from (chat history, websites, uploaded files), and
 *       {@link #chatHistoryExportStatus()} reports the progress of exporting
 *       the account's chat history into the assistant's knowledge.</li>
 * </ul>
 *
 * <p>Every facet not populated by a given read is empty rather than absent,
 * so a caller can read the same model shape regardless of which view it
 * requested.
 */
@ProtobufMessage(name = "BusinessAiAgentHome")
public final class BusinessAiAgentHome {
    /**
     * Capabilities the assistant can offer, each paired with its
     * availability to the account. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<BusinessAiAbility> abilities;

    /**
     * Ordered knowledge the assistant answers from (free-text statements and
     * frequently-asked-question entries). Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<BusinessAiKnowledgeEntry> knowledgeEntries;

    /**
     * Website-backed material the assistant ingests and the products it
     * features. Empty when the read did not request this facet or the
     * server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final BusinessAiWebsiteKnowledge websiteKnowledge;

    /**
     * Structured products the assistant can describe to customers. Never
     * {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final List<BusinessAiProductInfo> productInfo;

    /**
     * Whether the product-information feature is unlocked for the account.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    final boolean productInfoEligible;

    /**
     * Configured sources the assistant learns from (chat history, websites,
     * uploaded files). Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final List<BusinessAiKnowledgeSource> knowledgeSources;

    /**
     * Server-defined marker reporting the progress of exporting the
     * account's chat history into the assistant's knowledge. The full value
     * set is not recoverable from the WhatsApp client, so the raw marker is
     * exposed as a string. Empty when the read did not request this facet or
     * the server omitted it.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String chatHistoryExportStatus;

    /**
     * Constructs a new {@code BusinessAiAgentHome}. Each {@code null} list
     * argument is coerced to an empty list, and the scalar reference
     * arguments may be {@code null} when the read did not populate them.
     *
     * @param abilities               the offered capabilities; {@code null} treated as empty
     * @param knowledgeEntries        the ordered knowledge entries; {@code null} treated as empty
     * @param websiteKnowledge        the website-backed material, or {@code null}
     * @param productInfo             the structured products; {@code null} treated as empty
     * @param productInfoEligible     whether the product-information feature is unlocked
     * @param knowledgeSources        the configured learning sources; {@code null} treated as empty
     * @param chatHistoryExportStatus the chat-history export progress marker, or {@code null}
     */
    BusinessAiAgentHome(List<BusinessAiAbility> abilities,
                        List<BusinessAiKnowledgeEntry> knowledgeEntries,
                        BusinessAiWebsiteKnowledge websiteKnowledge,
                        List<BusinessAiProductInfo> productInfo,
                        boolean productInfoEligible,
                        List<BusinessAiKnowledgeSource> knowledgeSources,
                        String chatHistoryExportStatus) {
        this.abilities = abilities == null ? List.of() : abilities;
        this.knowledgeEntries = knowledgeEntries == null ? List.of() : knowledgeEntries;
        this.websiteKnowledge = websiteKnowledge;
        this.productInfo = productInfo == null ? List.of() : productInfo;
        this.productInfoEligible = productInfoEligible;
        this.knowledgeSources = knowledgeSources == null ? List.of() : knowledgeSources;
        this.chatHistoryExportStatus = chatHistoryExportStatus;
    }

    /**
     * Returns the capabilities the assistant can offer.
     *
     * @return an unmodifiable view of the capabilities; never {@code null},
     *         possibly empty
     */
    public List<BusinessAiAbility> abilities() {
        return Collections.unmodifiableList(abilities);
    }

    /**
     * Returns the ordered knowledge the assistant answers from.
     *
     * @return an unmodifiable view of the knowledge entries; never
     *         {@code null}, possibly empty
     */
    public List<BusinessAiKnowledgeEntry> knowledgeEntries() {
        return Collections.unmodifiableList(knowledgeEntries);
    }

    /**
     * Returns the website-backed material the assistant ingests.
     *
     * @return the website-backed material, or empty when the read did not
     *         populate it
     */
    public Optional<BusinessAiWebsiteKnowledge> websiteKnowledge() {
        return Optional.ofNullable(websiteKnowledge);
    }

    /**
     * Returns the structured products the assistant can describe.
     *
     * @return an unmodifiable view of the products; never {@code null},
     *         possibly empty
     */
    public List<BusinessAiProductInfo> productInfo() {
        return Collections.unmodifiableList(productInfo);
    }

    /**
     * Returns whether the product-information feature is unlocked for the
     * account.
     *
     * @return {@code true} when the account may use product-information
     *         knowledge, {@code false} otherwise
     */
    public boolean productInfoEligible() {
        return productInfoEligible;
    }

    /**
     * Returns the configured sources the assistant learns from.
     *
     * @return an unmodifiable view of the learning sources; never
     *         {@code null}, possibly empty
     */
    public List<BusinessAiKnowledgeSource> knowledgeSources() {
        return Collections.unmodifiableList(knowledgeSources);
    }

    /**
     * Returns the marker reporting the progress of exporting the account's
     * chat history into the assistant's knowledge.
     *
     * @return the chat-history export progress marker, or empty when the
     *         read did not populate it
     */
    public Optional<String> chatHistoryExportStatus() {
        return Optional.ofNullable(chatHistoryExportStatus);
    }
}
