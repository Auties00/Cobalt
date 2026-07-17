package com.github.auties00.cobalt.wire.linked.bot.plugin;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a single external account that has been linked to the Meta AI
 * bot, enabling it to access additional services on behalf of the user.
 *
 * <p>Account linking allows the bot to interact with first-party Meta
 * services (such as Instagram or Facebook) using the user's credentials.
 * Each linked account entry carries a {@link BotLinkedAccountType} that
 * identifies the kind of linkage. Multiple linked accounts are aggregated
 * inside a {@link BotLinkedAccountsMetadata} container.
 *
 * @see BotLinkedAccountsMetadata
 */
@ProtobufMessage(name = "BotLinkedAccount")
public final class BotLinkedAccount {
    /**
     * The kind of linkage this account represents, identifying which external
     * service the bot can access through it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    BotLinkedAccountType type;

    /**
     * Constructs a new {@code BotLinkedAccount} with the specified linkage type.
     *
     * @param type the linked account type, or {@code null} if unknown
     */
    BotLinkedAccount(BotLinkedAccountType type) {
        this.type = type;
    }

    /**
     * Returns the kind of linkage this account represents.
     *
     * @return an {@link Optional} describing the linked account type, or an
     *         empty {@code Optional} if not set
     */
    public Optional<BotLinkedAccountType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Sets the kind of linkage this account represents.
     *
     * @param type the new linked account type, or {@code null} to clear
     */
    public void setType(BotLinkedAccountType type) {
        this.type = type;
    }

    /**
     * Enumerates the types of external account linkages that a bot can have,
     * identifying which service category the linked account belongs to.
     */
    @ProtobufEnum(name = "BotLinkedAccount.BotLinkedAccountType")
    public static enum BotLinkedAccountType {
        /**
         * A first-party linked account, granting the bot access to Meta's own
         * services such as Instagram or Facebook on behalf of the user.
         */
        BOT_LINKED_ACCOUNT_TYPE_1P(0);

        /**
         * Constructs a new linked account type constant.
         *
         * @param index the protobuf-assigned numeric index for this constant
         */
        BotLinkedAccountType(@ProtobufEnumIndex int index) {
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
