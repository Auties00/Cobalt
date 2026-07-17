package com.github.auties00.cobalt.wire.linked.sync.action.business;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A sync action carrying customer relationship management (CRM) data associated with a chat.
 *
 * <p>Business accounts can annotate each chat with structured customer information such as
 * contact classification, email and alternative phone numbers, birthday, postal address,
 * how the customer was acquired, the current lead stage in the sales funnel, and the time of
 * their most recent order. These fields are replicated to linked devices through this action
 * so that the business sees consistent customer records regardless of which device they are
 * working from.
 *
 * <p>This action is transported in the {@code REGULAR_LOW} sync collection because the data
 * is operator-curated and not latency-sensitive, and keyed by chat JID through
 * {@link CustomerDataActionArgs}.
 */
@ProtobufMessage(name = "SyncActionValue.CustomerDataAction")
public final class CustomerDataAction implements SyncAction<CustomerDataActionArgs> {
    /**
     * The canonical action name used when encoding this action inside a sync patch index.
     */
    public static final String ACTION_NAME = "customer_data";

    /**
     * The action version negotiated with the server for this action type.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * The sync patch collection that carries this action between devices.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * The JID of the chat this customer record is attached to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String chatJid;

    /**
     * The integer code classifying the type of contact (for example lead versus customer).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    final Integer contactType;

    /**
     * The customer's email address.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String email;

    /**
     * The customer's alternative phone numbers, encoded as a single string.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String altPhoneNumbers;

    /**
     * The customer's birthday.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant birthday;

    /**
     * The customer's postal address.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String address;

    /**
     * The integer code identifying how this customer was acquired.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT32)
    final Integer acquisitionSource;

    /**
     * The integer code describing the customer's current position in the sales funnel.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.INT32)
    final Integer leadStage;

    /**
     * The timestamp of the customer's most recent order.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant lastOrder;

    /**
     * The timestamp at which this customer record was originally created.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant createdAt;

    /**
     * The timestamp at which this customer record was last modified.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant modifiedAt;

    /**
     * Creates a customer data action with all CRM fields populated.
     *
     * @param chatJid           the associated chat JID, or {@code null}
     * @param contactType       the contact type code, or {@code null}
     * @param email             the email address, or {@code null}
     * @param altPhoneNumbers   the alternative phone numbers string, or {@code null}
     * @param birthday          the birthday, or {@code null}
     * @param address           the postal address, or {@code null}
     * @param acquisitionSource the acquisition source code, or {@code null}
     * @param leadStage         the lead stage code, or {@code null}
     * @param lastOrder         the time of the most recent order, or {@code null}
     * @param createdAt         the creation time of the record, or {@code null}
     * @param modifiedAt        the last modification time of the record, or {@code null}
     */
    CustomerDataAction(String chatJid, Integer contactType, String email, String altPhoneNumbers,
                       Instant birthday, String address, Integer acquisitionSource, Integer leadStage,
                       Instant lastOrder, Instant createdAt, Instant modifiedAt) {
        this.chatJid = chatJid;
        this.contactType = contactType;
        this.email = email;
        this.altPhoneNumbers = altPhoneNumbers;
        this.birthday = birthday;
        this.address = address;
        this.acquisitionSource = acquisitionSource;
        this.leadStage = leadStage;
        this.lastOrder = lastOrder;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the action name
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version for this action type.
     *
     * @return the action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * Returns the JID of the chat associated with this customer data.
     *
     * @return the chat JID, or {@link Optional#empty()} if not set
     */
    public Optional<String> chatJid() {
        return Optional.ofNullable(chatJid);
    }

    /**
     * Returns the contact type code for this customer.
     *
     * @return the contact type code, or {@link OptionalInt#empty()} if not set
     */
    public OptionalInt contactType() {
        return contactType == null ? OptionalInt.empty() : OptionalInt.of(contactType);
    }

    /**
     * Returns the customer's email address.
     *
     * @return the email, or {@link Optional#empty()} if not set
     */
    public Optional<String> email() {
        return Optional.ofNullable(email);
    }

    /**
     * Returns the customer's alternative phone numbers, as stored by the business.
     *
     * @return the alternative phone numbers, or {@link Optional#empty()} if not set
     */
    public Optional<String> altPhoneNumbers() {
        return Optional.ofNullable(altPhoneNumbers);
    }

    /**
     * Returns the customer's birthday.
     *
     * @return the birthday, or {@link Optional#empty()} if not set
     */
    public Optional<Instant> birthday() {
        return Optional.ofNullable(birthday);
    }

    /**
     * Returns the customer's postal address.
     *
     * @return the address, or {@link Optional#empty()} if not set
     */
    public Optional<String> address() {
        return Optional.ofNullable(address);
    }

    /**
     * Returns the acquisition source code for this customer.
     *
     * @return the acquisition source code, or {@link OptionalInt#empty()} if not set
     */
    public OptionalInt acquisitionSource() {
        return acquisitionSource == null ? OptionalInt.empty() : OptionalInt.of(acquisitionSource);
    }

    /**
     * Returns the lead stage code describing the customer's position in the sales funnel.
     *
     * @return the lead stage code, or {@link OptionalInt#empty()} if not set
     */
    public OptionalInt leadStage() {
        return leadStage == null ? OptionalInt.empty() : OptionalInt.of(leadStage);
    }

    /**
     * Returns the timestamp of the customer's most recent order.
     *
     * @return the last order time, or {@link Optional#empty()} if not set
     */
    public Optional<Instant> lastOrder() {
        return Optional.ofNullable(lastOrder);
    }

    /**
     * Returns the timestamp at which this customer record was originally created.
     *
     * @return the creation time, or {@link Optional#empty()} if not set
     */
    public Optional<Instant> createdAt() {
        return Optional.ofNullable(createdAt);
    }

    /**
     * Returns the timestamp at which this customer record was last modified.
     *
     * @return the modification time, or {@link Optional#empty()} if not set
     */
    public Optional<Instant> modifiedAt() {
        return Optional.ofNullable(modifiedAt);
    }
}
