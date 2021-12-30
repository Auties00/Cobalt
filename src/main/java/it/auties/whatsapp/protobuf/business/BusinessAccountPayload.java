package it.auties.whatsapp.protobuf.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.protobuf.info.BusinessAccountInfo;
import it.auties.whatsapp.util.BytesDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.IOException;

/**
 * A model class that holds a payload about a business account.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class BusinessAccountPayload {
  /**
   * The certificate of this account
   */
  @JsonProperty("1")
  @JsonPropertyDescription("VerifiedNameCertificate")
  private BusinessCertificate certificate;

  /**
   * The info about this account
   */
  @JsonProperty("2")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private BusinessAccountInfo info;

  /**
   * Constructs a new BusinessAccountPayload from an encoded payload
   *
   * @param certificate the certificate of this account
   * @param encodedInfo the encoded info of this account
   * @throws IllegalArgumentException if the payload cannot be decoded
   */
  @JsonCreator
  public BusinessAccountPayload(BusinessCertificate certificate, byte[] encodedInfo){
    try {
      this.certificate = certificate;
      this.info = ProtobufDecoder.forType(BusinessAccountInfo.class).decode(encodedInfo);
    }catch (IOException exception){
      throw new IllegalArgumentException("Cannot construct BusinessAccountPayload from %s".formatted(BinaryArray.of(encodedInfo).toBase64()));
    }
  }
}
