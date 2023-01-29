package it.auties.whatsapp.model.business;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;
import static it.auties.protobuf.base.ProtobufType.UINT64;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that holds a payload about a business link info.
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("BizAccountLinkInfo")
public class BusinessAccountLinkInfo
    implements ProtobufMessage {

  /**
   * The business id for this link
   */
  @ProtobufProperty(index = 1, name = "whatsappBizAcctFbid", type = UINT64)
  private long businessId;

  /**
   * The phone number of this link
   */
  @ProtobufProperty(index = 2, name = "whatsappAcctNumber", type = STRING)
  private String phoneNumber;

  /**
   * The issue time of this link
   */
  @ProtobufProperty(index = 3, name = "issueTime", type = UINT64)
  private long issueTime;

  /**
   * The type of storage
   */
  @ProtobufProperty(index = 4, name = "hostStorage", type = MESSAGE)
  private BusinessStorageType hostStorage;

  /**
   * The type of account
   */
  @ProtobufProperty(index = 5, name = "accountType", type = MESSAGE)
  private BusinessAccountType accountType;
}