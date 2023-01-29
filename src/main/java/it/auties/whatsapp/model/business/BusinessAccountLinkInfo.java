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

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("BizAccountLinkInfo")
public class BusinessAccountLinkInfo
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, name = "whatsappBizAcctFbid", type = UINT64)
  private long whatsappBizAccountFacebookId;

  @ProtobufProperty(index = 2, name = "whatsappAcctNumber", type = STRING)
  private String whatsappAcctNumber;

  @ProtobufProperty(index = 3, name = "issueTime", type = UINT64)
  private long issueTime;

  @ProtobufProperty(index = 4, name = "hostStorage", type = MESSAGE)
  private BusinessStorageType hostStorage;

  @ProtobufProperty(index = 5, name = "accountType", type = MESSAGE)
  private BusinessAccountType accountType;
}