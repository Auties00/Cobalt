package it.auties.whatsapp.model._generated;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.UINT32;
import static it.auties.protobuf.base.ProtobufType.UINT64;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.business.BusinessLocalizedName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("CertChain")
public class CertChain implements ProtobufMessage {
  @ProtobufProperty(index = 1, name = "leaf", type = MESSAGE)
  private NoiseCertificate leaf;

  @ProtobufProperty(index = 2, name = "intermediate", type = MESSAGE)
  private NoiseCertificate intermediate;

  @AllArgsConstructor
  @Data
  @Jacksonized
  @Builder
  @ProtobufName("NoiseCertificate")
  public static class NoiseCertificate implements ProtobufMessage {
    @ProtobufProperty(index = 1, name = "details", type = BYTES)
    private byte[] details;

    @ProtobufProperty(index = 2, name = "signature", type = BYTES)
    private byte[] signature;

    @AllArgsConstructor
    @Data
    @Jacksonized
    @Builder
    @ProtobufName("Details")
    public static class Details implements ProtobufMessage {
      @ProtobufProperty(index = 1, name = "serial", type = UINT32)
      private Integer serial;

      @ProtobufProperty(index = 2, name = "issuerSerial", type = UINT32)
      private Integer issuerSerial;

      @ProtobufProperty(index = 3, name = "key", type = BYTES)
      private byte[] key;

      @ProtobufProperty(index = 4, name = "notBefore", type = UINT64)
      private Long notBefore;

      @ProtobufProperty(index = 5, name = "notAfter", type = UINT64)
      private Long notAfter;

      @ProtobufProperty(implementation = BusinessLocalizedName.class, index = 8, name = "localizedNames", repeated = true, type = MESSAGE)
      private List<BusinessLocalizedName> localizedNames;

      @ProtobufProperty(index = 10, name = "issueTime", type = UINT64)
      private Long issueTime;
    }
  }
}