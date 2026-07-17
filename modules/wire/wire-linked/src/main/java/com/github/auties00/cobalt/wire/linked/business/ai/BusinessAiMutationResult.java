package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Outcome of a status-only change to a WhatsApp Business AI agent's
 * configuration.
 *
 * <p>The WhatsApp Business AI agent is the auto-reply assistant a merchant
 * can attach to their business account: it answers incoming chats from the
 * knowledge the merchant has taught it (frequently-asked-question entries,
 * uploaded files, websites, chat history, and product information) and can
 * be scoped to fire only for certain chats and only during certain hours.
 *
 * <p>Many of the agent's configuration edits report only whether the change
 * took effect, without echoing the entity that changed: seeding the agent
 * from chat history, removing example responses, approving or discarding
 * inferred knowledge, removing a knowledge source (chat history, a website,
 * or an uploaded file), kicking off knowledge extraction from an uploaded
 * file, changing which chats trigger the assistant, changing the assistant's
 * active hours, and toggling automatic re-engagement all fall into this
 * group. This model collapses those outcomes into one shape so a caller
 * checks {@link #success()} regardless of which edit it ran.
 *
 * <p>When the server reports which entities the edit touched (for example a
 * created knowledge-source id), they are surfaced through
 * {@link #affectedIds()}; otherwise that list is empty. A human-readable
 * failure or extraction-error message, when the server provides one, is
 * carried by {@link #errorMessage()}.
 */
@ProtobufMessage(name = "BusinessAiMutationResult")
public final class BusinessAiMutationResult {
    /**
     * Whether the edit took effect. The server reports this as the sole
     * outcome of a status-only AI-agent change; {@code false} both when the
     * server explicitly reported failure and when it omitted the success
     * marker entirely.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean success;

    /**
     * Identifiers of the AI-agent entities the edit affected, when the
     * server reports them (for example the id of a knowledge source the
     * edit created). Never {@code null}, possibly empty when the server
     * reports no ids.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final List<String> affectedIds;

    /**
     * Human-readable reason the edit failed, or the extraction error the
     * server attached. Empty when the edit succeeded or the server did not
     * attach a message.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String errorMessage;

    /**
     * Constructs a new {@code BusinessAiMutationResult}. A {@code null}
     * {@code affectedIds} is coerced to an empty list, and
     * {@code errorMessage} may be {@code null} when the server attached no
     * failure message.
     *
     * @param success      whether the edit took effect
     * @param affectedIds  the affected entity ids; {@code null} treated as empty
     * @param errorMessage the failure message, or {@code null}
     */
    BusinessAiMutationResult(boolean success, List<String> affectedIds, String errorMessage) {
        this.success = success;
        this.affectedIds = affectedIds == null ? List.of() : affectedIds;
        this.errorMessage = errorMessage;
    }

    /**
     * Returns whether the edit took effect.
     *
     * @return {@code true} when the server reported the change applied
     */
    public boolean success() {
        return success;
    }

    /**
     * Returns the identifiers of the AI-agent entities the edit affected.
     *
     * @return an unmodifiable view of the affected ids; never {@code null},
     *         possibly empty
     */
    public List<String> affectedIds() {
        return Collections.unmodifiableList(affectedIds);
    }

    /**
     * Returns the human-readable reason the edit failed.
     *
     * @return an {@code Optional} carrying the failure message, or empty
     *         when the edit succeeded or carried no message
     */
    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }
}
