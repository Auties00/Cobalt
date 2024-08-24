package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage(name = "ClientPayload.DevicePairingRegistrationData")
public record CompanionRegistrationData(@ProtobufProperty(index = 1, type = ProtobufType.BYTES) byte[] eRegid,
                                        @ProtobufProperty(index = 2, type = ProtobufType.BYTES) byte[] eKeytype,
                                        @ProtobufProperty(index = 3, type = ProtobufType.BYTES) byte[] eIdent,
                                        @ProtobufProperty(index = 4, type = ProtobufType.BYTES) byte[] eSkeyId,
                                        @ProtobufProperty(index = 5, type = ProtobufType.BYTES) byte[] eSkeyVal,
                                        @ProtobufProperty(index = 6, type = ProtobufType.BYTES) byte[] eSkeySig,
                                        @ProtobufProperty(index = 7, type = ProtobufType.BYTES) byte[] buildHash,
                                        @ProtobufProperty(index = 8, type = ProtobufType.BYTES) byte[] companionProps) {
}
