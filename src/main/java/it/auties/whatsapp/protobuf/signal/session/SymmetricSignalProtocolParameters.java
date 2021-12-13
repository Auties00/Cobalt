package it.auties.whatsapp.protobuf.signal.session;

import it.auties.whatsapp.protobuf.signal.key.SignalKeyPair;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(fluent = true)
public class SymmetricSignalProtocolParameters {
    private byte[] ourIdentityKey;
    private byte[] ourBaseKey;
    private byte[] ourRatchetKey;
    private byte[] theirIdentityKey;
    private byte[] theirBaseKey;
    private byte[] theirRatchetKey;
}
