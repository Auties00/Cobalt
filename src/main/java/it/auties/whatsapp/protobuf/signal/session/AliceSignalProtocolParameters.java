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
public class AliceSignalProtocolParameters {
    private byte[] ourIdentityKey;
    private byte[] ourBaseKey;
    private byte[] theirIdentityKey;
    private byte[] theirSignedPreKey;
    private byte[] theirRatchetKey;
    private int theirOneTimePreKey;
}
