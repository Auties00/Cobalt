package it.auties.whatsapp.socket.message.signal;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.model.signal.SignalAddress;
import it.auties.whatsapp.model.signal.message.SignalPreKeySignalMessage;
import it.auties.whatsapp.model.signal.state.SignalPreKeyBundle;
import it.auties.whatsapp.model.signal.key.SignalKeyDirection;
import it.auties.whatsapp.model.signal.key.SignalKeyPair;
import it.auties.whatsapp.model.signal.parameters.AliceSignalProtocolParametersBuilder;
import it.auties.whatsapp.model.signal.parameters.BobSignalProtocolParametersBuilder;
import it.auties.whatsapp.model.signal.state.SessionPendingPreKeyBuilder;
import it.auties.whatsapp.model.signal.state.SignalSessionRecord;

import java.util.OptionalInt;

public final class SignalSessionBuilder {
    private final Keys keys;
    private final SignalAddress remoteAddress;

    public SignalSessionBuilder(Keys keys, SignalAddress remoteAddress) {
        this.keys = keys;
        this.remoteAddress = remoteAddress;
    }

    OptionalInt process(SignalSessionRecord sessionRecord, SignalPreKeySignalMessage message) {
        var theirIdentityKey = message.identityKey();
        if (!keys.hasTrust(remoteAddress, theirIdentityKey, SignalKeyDirection.INCOMING)) {
            throw new SecurityException("The identity key of the incoming message is not trusted");
        }

        return processV3(sessionRecord, message);
    }

    private OptionalInt processV3(SignalSessionRecord sessionRecord, SignalPreKeySignalMessage message) {
        if (sessionRecord.hasSessionState(message.version(), message.baseKey().serialized())) {
            return OptionalInt.empty();
        }

        var ourSignedPreKey = keys.findSignedKeyPairById(message.signedPreKeyId())
                .orElseThrow(() -> new IllegalStateException("No prekey found with id " + message.signedPreKeyId()));
        var parameters = new BobSignalProtocolParametersBuilder()
                .theirBaseKey(message.baseKey())
                .theirIdentityKey(message.identityKey())
                .ourIdentityKey(keys.identityKeyPair())
                .ourSignedPreKey(ourSignedPreKey.keyPair())
                .ourRatchetKey(ourSignedPreKey.keyPair());

        var preKeyId = message.preKeyId();
        if (preKeyId != null) {
            var preKey = keys.findPreKeyById(preKeyId)
                    .orElseThrow(() -> new IllegalStateException("No prekey found with id " + preKeyId));
            parameters.ourOneTimePreKey(preKey.keyPair());
        }

        if (!sessionRecord.isFresh()) {
            sessionRecord.archiveCurrentState();
        }

        SignalRatchetingSession.initializeSession(sessionRecord.sessionState(), parameters.build());

        sessionRecord.sessionState()
                .setLocalRegistrationId(keys.registrationId());
        sessionRecord.sessionState()
                .setRemoteRegistrationId(message.registrationId());
        sessionRecord.sessionState()
                .setBaseKey(message.baseKey().serialized());

        return preKeyId == null ? OptionalInt.empty() : OptionalInt.of(preKeyId);
    }

    public void process(SignalPreKeyBundle preKey) {
        if (!keys.hasTrust(remoteAddress, preKey.identityKey(), SignalKeyDirection.OUTGOING)) {
            throw new SecurityException("The identity key of the incoming message is not trusted");
        }

        var theirSignedPreKey = preKey.signedPreKeyPublic();
        if (preKey.signedPreKeyPublic() == null) {
            throw new SecurityException("No signed prekey!");
        }

        if (!Curve25519.verifySignature(preKey.identityKey().encodedPoint(),
                theirSignedPreKey.serialized(),
                preKey.signedPreKeySignature())) {
            throw new SecurityException("Invalid signature on device key!");
        }

        var sessionRecord = keys.findSessionByAddress(remoteAddress).orElseGet(() -> {
            var record = new SignalSessionRecord();
            keys.addSession(remoteAddress, record);
            return record;
        });

        var ourBaseKey = SignalKeyPair.random();

        var theirOneTimePreKey = preKey.preKeyPublic();
        var theirOneTimePreKeyId = theirOneTimePreKey != null ? preKey.preKeyId() : null;

        var parameters = new AliceSignalProtocolParametersBuilder()
                .ourBaseKey(ourBaseKey)
                .ourIdentityKey(keys.identityKeyPair())
                .theirIdentityKey(preKey.identityKey())
                .theirSignedPreKey(theirSignedPreKey)
                .theirRatchetKey(theirSignedPreKey)
                .theirOneTimePreKey(theirOneTimePreKey);

        if (!sessionRecord.isFresh()) {
            sessionRecord.archiveCurrentState();
        }

        SignalRatchetingSession.initializeSession(sessionRecord.sessionState(), parameters.build());

        var pendingPreKey = new SessionPendingPreKeyBuilder()
                .preKeyId(theirOneTimePreKeyId)
                .signedKeyId(preKey.signedPreKeyId())
                .baseKey(ourBaseKey.publicKey())
                .build();

        sessionRecord.sessionState()
                .setPendingPreKey(pendingPreKey);
        sessionRecord.sessionState()
                .setLocalRegistrationId(keys.registrationId());
        sessionRecord.sessionState()
                .setRemoteRegistrationId(preKey.registrationId());
        sessionRecord.sessionState()
                .setBaseKey(ourBaseKey.publicKey().serialized());
    }
}
