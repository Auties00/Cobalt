package it.auties.whatsapp.model.business;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
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

  @ProtobufProperty(index = 1, name = "whatsappBizAcctFbid", type = ProtobufType.UINT64)
  private long whatsappBizAccountFacebookId;

  @ProtobufProperty(index = 2, name = "whatsappAcctNumber", type = ProtobufType.STRING)
  private String whatsappAcctNumber;

  @ProtobufProperty(index = 3, name = "issueTime", type = ProtobufType.UINT64)
  private long issueTime;

  @ProtobufProperty(index = 4, name = "hostStorage", type = ProtobufType.MESSAGE)
  private HostStorageType hostStorage;

  @ProtobufProperty(index = 5, name = "accountType", type = ProtobufType.MESSAGE)
  private AccountType accountType;

  @AllArgsConstructor
  public enum AccountType
      implements ProtobufMessage {
    ENTERPRISE(0);
    @Getter
    private final int index;
  }

  @AllArgsConstructor
  public enum HostStorageType
      implements ProtobufMessage {
    ON_PREMISE(0),
    FACEBOOK(1);
    @Getter
    private final int index;
  }
}