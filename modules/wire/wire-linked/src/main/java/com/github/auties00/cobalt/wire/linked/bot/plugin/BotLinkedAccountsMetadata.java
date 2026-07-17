package com.github.auties00.cobalt.wire.linked.bot.plugin;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Carries the state of the account-linking flow for the Meta AI bot, including
 * the list of external accounts currently linked, authentication tokens, and
 * any error information from the linking process.
 *
 * <p>Account linking enables the bot to access first-party Meta services on
 * behalf of the user. This metadata is included in the bot message to convey
 * which accounts are active, provide opaque authentication tokens for the
 * linking handshake, and report any errors that occurred during the process.
 *
 * @see BotLinkedAccount
 */
@ProtobufMessage(name = "BotLinkedAccountsMetadata")
public final class BotLinkedAccountsMetadata {
    /**
     * The external accounts currently linked to the bot, each identifying a
     * service the bot can access on the user's behalf.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<BotLinkedAccount> accounts;

    /**
     * Opaque authentication token data exchanged during the account-linking
     * handshake. The format and content of these bytes are determined by the
     * account-linking service and should be treated as opaque.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] acAuthTokens;

    /**
     * An error code returned by the account-linking service. A {@code null}
     * value indicates that no error information is present. The specific
     * meaning of each code is defined by the account-linking service.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    Integer acErrorCode;

    /**
     * Constructs a new {@code BotLinkedAccountsMetadata} with the specified
     * values.
     *
     * @param accounts     the list of linked accounts, or {@code null} if none
     * @param acAuthTokens the opaque authentication token bytes, or
     *                     {@code null} if not present
     * @param acErrorCode  the error code from the linking service, or
     *                     {@code null} if no error occurred
     */
    BotLinkedAccountsMetadata(List<BotLinkedAccount> accounts, byte[] acAuthTokens, Integer acErrorCode) {
        this.accounts = accounts;
        this.acAuthTokens = acAuthTokens;
        this.acErrorCode = acErrorCode;
    }

    /**
     * Returns the external accounts currently linked to the bot.
     *
     * @return an unmodifiable list of {@link BotLinkedAccount} entries, or an
     *         empty list if no accounts are linked
     */
    public List<BotLinkedAccount> accounts() {
        return accounts == null ? List.of() : Collections.unmodifiableList(accounts);
    }

    /**
     * Returns the opaque authentication token data for the account-linking
     * handshake.
     *
     * @return an {@link Optional} describing the auth token bytes, or an empty
     *         {@code Optional} if not present
     */
    public Optional<byte[]> acAuthTokens() {
        return Optional.ofNullable(acAuthTokens);
    }

    /**
     * Returns the error code from the account-linking service, if any error
     * occurred during the linking process.
     *
     * @return an {@link OptionalInt} describing the error code, or an empty
     *         {@code OptionalInt} if no error information is present
     */
    public OptionalInt acErrorCode() {
        return acErrorCode == null ? OptionalInt.empty() : OptionalInt.of(acErrorCode);
    }

    /**
     * Sets the list of external accounts linked to the bot.
     *
     * @param accounts the new list of linked accounts, or {@code null} to
     *                 clear all linked accounts
     */
    public void setAccounts(List<BotLinkedAccount> accounts) {
        this.accounts = accounts;
    }

    /**
     * Sets the opaque authentication token data for the account-linking
     * handshake.
     *
     * @param acAuthTokens the new auth token bytes, or {@code null} to clear
     */
    public void setAcAuthTokens(byte[] acAuthTokens) {
        this.acAuthTokens = acAuthTokens;
    }

    /**
     * Sets the error code from the account-linking service.
     *
     * @param acErrorCode the new error code, or {@code null} to clear
     */
    public void setAcErrorCode(Integer acErrorCode) {
        this.acErrorCode = acErrorCode;
    }
}
