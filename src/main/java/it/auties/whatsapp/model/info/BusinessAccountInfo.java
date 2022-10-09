package it.auties.whatsapp.model.info;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.business.BusinessAccountType;
import it.auties.whatsapp.model.business.BusinessStorageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that holds the information related to a business account.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class BusinessAccountInfo implements Info {
    /**
     * The facebook jid
     */
    @ProtobufProperty(index = 1, type = UINT64)
    private long facebookId;

    /**
     * The account phone number
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String accountNumber;

    /**
     * The timestamp of the account
     */
    @ProtobufProperty(index = 3, type = UINT64)
    private long timestamp;

    /**
     * Indicates here this account is hosted
     */
    @ProtobufProperty(index = 4, type = MESSAGE, implementation = BusinessStorageType.class)
    private BusinessStorageType hostStorage;

    /**
     * The type of this account
     */
    @ProtobufProperty(index = 5, type = MESSAGE, implementation = BusinessAccountType.class)
    private BusinessAccountType accountType;
}
