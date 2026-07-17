package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Input model describing a WhatsApp Business AI agent rule to create or update.
 *
 * <p>A rule steers how the merchant's auto-reply assistant behaves: it carries a
 * {@link #ruleType() rule type} selecting the behaviour being configured, an
 * optional {@link #customRule() free-text instruction}, and two optional
 * structured knobs, the {@link #emojisConfig() emoji-usage} and
 * {@link #priceConfig() price-sharing} configurations. When the rule already
 * exists, its {@link #ruleId() server-assigned identifier} is present; a
 * creation leaves it unset.
 */
@ProtobufMessage(name = "AiRuleInput")
public final class AiRuleInput {
    /**
     * Free-text instruction the merchant wrote for this rule. Empty when the
     * rule carries no custom instruction.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String customRule;

    /**
     * Server-defined discriminator selecting which assistant behaviour this
     * rule configures. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String ruleType;

    /**
     * Emoji-usage configuration for this rule. Empty when the rule does not
     * tune emoji usage.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final AiEmojisConfig emojisConfig;

    /**
     * Price-sharing configuration for this rule. Empty when the rule does not
     * tune price sharing.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final AiPriceConfig priceConfig;

    /**
     * Server-assigned identifier of the rule being updated. Empty on a
     * creation, present on an update.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String ruleId;

    /**
     * Constructs a new {@code AiRuleInput}. Every argument may be {@code null}
     * to leave the corresponding field unset.
     *
     * @param customRule   the free-text instruction, or {@code null}
     * @param ruleType     the rule-type discriminator, or {@code null}
     * @param emojisConfig the emoji-usage configuration, or {@code null}
     * @param priceConfig  the price-sharing configuration, or {@code null}
     * @param ruleId       the rule identifier, or {@code null} on a creation
     */
    AiRuleInput(String customRule, String ruleType, AiEmojisConfig emojisConfig, AiPriceConfig priceConfig, String ruleId) {
        this.customRule = customRule;
        this.ruleType = ruleType;
        this.emojisConfig = emojisConfig;
        this.priceConfig = priceConfig;
        this.ruleId = ruleId;
    }

    /**
     * Returns the free-text instruction the merchant wrote for this rule.
     *
     * @return an {@link Optional} carrying the instruction, or empty when unset
     */
    public Optional<String> customRule() {
        return Optional.ofNullable(customRule);
    }

    /**
     * Returns the discriminator selecting which assistant behaviour this rule
     * configures.
     *
     * @return an {@link Optional} carrying the rule type, or empty when unset
     */
    public Optional<String> ruleType() {
        return Optional.ofNullable(ruleType);
    }

    /**
     * Returns the emoji-usage configuration for this rule.
     *
     * @return an {@link Optional} carrying the emoji-usage configuration, or
     *         empty when unset
     */
    public Optional<AiEmojisConfig> emojisConfig() {
        return Optional.ofNullable(emojisConfig);
    }

    /**
     * Returns the price-sharing configuration for this rule.
     *
     * @return an {@link Optional} carrying the price-sharing configuration, or
     *         empty when unset
     */
    public Optional<AiPriceConfig> priceConfig() {
        return Optional.ofNullable(priceConfig);
    }

    /**
     * Returns the server-assigned identifier of the rule being updated.
     *
     * @return an {@link Optional} carrying the rule id, or empty on a creation
     */
    public Optional<String> ruleId() {
        return Optional.ofNullable(ruleId);
    }
}
