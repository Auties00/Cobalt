package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One auto-reply behaviour rule of a WhatsApp Business AI agent.
 *
 * <p>Beyond the knowledge it answers from, the merchant's auto-reply
 * assistant can be shaped by a set of behaviour rules that tune how it talks
 * to customers: a rule may set a free-form instruction the assistant must
 * follow, control how liberally it uses emoji, or govern whether it volunteers
 * pricing in its replies. This model is one such rule.
 *
 * <p>{@link #ruleType()} discriminates which behaviour the rule governs.
 * {@link #customRule()} carries the merchant's free-form instruction when the
 * rule is an instruction rule. {@link #emojiFrequency()} and
 * {@link #priceSharing()} carry the tuning markers for the emoji-usage and
 * price-sharing rules respectively; each is populated only for the rule kind
 * it belongs to.
 */
@ProtobufMessage(name = "BusinessAiRule")
public final class BusinessAiRule {
    /**
     * Server-issued identifier of this rule. This is the handle used to update
     * or remove the rule; it is not a WhatsApp address. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Server-defined marker discriminating which behaviour the rule governs
     * (free-form instruction, emoji usage, or price sharing). The full value
     * set is not recoverable from the WhatsApp client, so the raw marker is
     * exposed as a string. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String ruleType;

    /**
     * Merchant's free-form instruction the assistant must follow. Populated
     * only for instruction rules. Empty otherwise.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String customRule;

    /**
     * Server-defined marker describing how liberally the assistant uses emoji
     * in its replies. The full value set is not recoverable from the WhatsApp
     * client, so the raw marker is exposed as a string. Populated only for the
     * emoji-usage rule. Empty otherwise.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String emojiFrequency;

    /**
     * Server-defined marker describing whether and how the assistant
     * volunteers pricing in its replies. The full value set is not recoverable
     * from the WhatsApp client, so the raw marker is exposed as a string.
     * Populated only for the price-sharing rule. Empty otherwise.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String priceSharing;

    /**
     * Constructs a new {@code BusinessAiRule}. Every argument may be
     * {@code null} when the server omitted the corresponding field; the
     * kind-specific arguments are {@code null} for rules of a different kind.
     *
     * @param id             the rule identifier, or {@code null}
     * @param ruleType       the rule-kind marker, or {@code null}
     * @param customRule     the free-form instruction, or {@code null}
     * @param emojiFrequency the emoji-usage marker, or {@code null}
     * @param priceSharing   the price-sharing marker, or {@code null}
     */
    BusinessAiRule(String id, String ruleType, String customRule, String emojiFrequency, String priceSharing) {
        this.id = id;
        this.ruleType = ruleType;
        this.customRule = customRule;
        this.emojiFrequency = emojiFrequency;
        this.priceSharing = priceSharing;
    }

    /**
     * Returns the server-issued identifier of this rule.
     *
     * @return the rule id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the marker discriminating which behaviour the rule governs.
     *
     * @return the rule-kind marker, or empty when the server omitted it
     */
    public Optional<String> ruleType() {
        return Optional.ofNullable(ruleType);
    }

    /**
     * Returns the merchant's free-form instruction the assistant must follow.
     *
     * @return the free-form instruction, or empty when the rule is not an
     *         instruction rule or the server omitted it
     */
    public Optional<String> customRule() {
        return Optional.ofNullable(customRule);
    }

    /**
     * Returns the marker describing how liberally the assistant uses emoji.
     *
     * @return the emoji-usage marker, or empty when the rule is not the
     *         emoji-usage rule or the server omitted it
     */
    public Optional<String> emojiFrequency() {
        return Optional.ofNullable(emojiFrequency);
    }

    /**
     * Returns the marker describing whether the assistant volunteers pricing.
     *
     * @return the price-sharing marker, or empty when the rule is not the
     *         price-sharing rule or the server omitted it
     */
    public Optional<String> priceSharing() {
        return Optional.ofNullable(priceSharing);
    }
}
