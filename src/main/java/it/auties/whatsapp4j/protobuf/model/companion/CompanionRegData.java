package it.auties.whatsapp4j.protobuf.model.companion;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class CompanionRegData {
  @JsonProperty(value = "8")
  private byte[] companionProps;

  @JsonProperty(value = "7")
  private byte[] buildHash;

  @JsonProperty(value = "6")
  private byte[] eSkeySig;

  @JsonProperty(value = "5")
  private byte[] eSkeyVal;

  @JsonProperty(value = "4")
  private byte[] eSkeyId;

  @JsonProperty(value = "3")
  private byte[] eIdent;

  @JsonProperty(value = "2")
  private byte[] eKeytype;

  @JsonProperty(value = "1")
  private byte[] eRegid;
}
