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
public class BobSignalProtocolParameters {
    private SignalKeyPair ourIdentityKey;
    private SignalKeyPair ourSignedPreKey;
    private SignalKeyPair ourRatchetKey;
    private SignalKeyPair ourOneTimePreKey;
    private byte[] theirIdentityKey;
    private byte[] theirBaseKey;
}
