package it.auties.whatsapp.protobuf.signal.session;

import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(fluent = true)
public class AliceSignalProtocolParameters {
    private SignalKeyPair ourIdentityKey;
    private SignalKeyPair ourBaseKey;
    private byte[] theirIdentityKey;
    private byte[] theirRatchetKey;
    private byte[] theirOneTimePreKey;
    private byte[] theirSignedPreKey;
}
