package it.auties.whatsapp.model._generated;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
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
  @ProtobufProperty(index = 1, name = "leaf", type = ProtobufType.MESSAGE)
  private NoiseCertificate leaf;

  @ProtobufProperty(index = 2, name = "intermediate", type = ProtobufType.MESSAGE)
  private NoiseCertificate intermediate;

  @AllArgsConstructor
  @Data
  @Jacksonized
  @Builder
  @ProtobufName("NoiseCertificate")
  public static class NoiseCertificate implements ProtobufMessage {
    @ProtobufProperty(index = 1, name = "details", type = ProtobufType.BYTES)
    private byte[] details;

    @ProtobufProperty(index = 2, name = "signature", type = ProtobufType.BYTES)
    private byte[] signature;

    @AllArgsConstructor
    @Data
    @Jacksonized
    @Builder
    @ProtobufName("Details")
    public static class Details implements ProtobufMessage {
      @ProtobufProperty(index = 1, name = "serial", type = ProtobufType.UINT32)
      private Integer serial;

      @ProtobufProperty(index = 2, name = "issuerSerial", type = ProtobufType.UINT32)
      private Integer issuerSerial;

      @ProtobufProperty(index = 3, name = "key", type = ProtobufType.BYTES)
      private byte[] key;

      @ProtobufProperty(index = 4, name = "notBefore", type = ProtobufType.UINT64)
      private Long notBefore;

      @ProtobufProperty(index = 5, name = "notAfter", type = ProtobufType.UINT64)
      private Long notAfter;

      @ProtobufProperty(implementation = BusinessLocalizedName.class, index = 8, name = "localizedNames", repeated = true, type = ProtobufType.MESSAGE)
      private List<BusinessLocalizedName> localizedNames;

      @ProtobufProperty(index = 10, name = "issueTime", type = ProtobufType.UINT64)
      private Long issueTime;
    }
  }
}