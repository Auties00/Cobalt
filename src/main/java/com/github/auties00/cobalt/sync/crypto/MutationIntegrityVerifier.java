package com.github.auties00.cobalt.sync.crypto;

import com.github.auties00.cobalt.exception.WebAppStateFatalSyncException;
import com.github.auties00.cobalt.sync.exchange.MutationSyncResponse;
import com.github.auties00.cobalt.model.sync.PatchSync;
import com.github.auties00.cobalt.model.sync.PatchType;
import com.github.auties00.cobalt.store.WhatsAppStore;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

public final class MutationIntegrityVerifier {
    private final WhatsAppStore store;

    public MutationIntegrityVerifier(WhatsAppStore store) {
        this.store = store;
    }

    public void verifyIntegrity(MutationSyncResponse response, byte[] expectedHash) {
        if (!store.checkPatchMacs()) {
            return;
        }

        if (response.isSnapshot()) {
            verifySnapshotMac(response, expectedHash);
        }

        for (var patch : response.patches()) {
            verifyPatchMac(response.collectionName(), patch, expectedHash);
        }
    }

    private void verifySnapshotMac(MutationSyncResponse response, byte[] expectedHash) {
        var snapshot = response.snapshot();
        if (snapshot == null || snapshot.mac() == null) {
            return;  // No MAC to verify
        }

        var keyId = snapshot.keyId();
        if (keyId == null || keyId.id() == null) {
            throw new InternalError("Snapshot missing key ID");
        }

        // Get sync key
        var syncKey = store.findWebAppStateKeyById(keyId.id())
                .orElseThrow(() -> new InternalError("Unknown sync key for snapshot"));

        var keyData = syncKey.keyData();
        if (keyData == null || keyData.keyData() == null) {
            throw new InternalError("Sync key has no data");
        }

        // Derive keys to get snapshot MAC key
        try (var keys = MutationKeys.ofSyncKey(keyData.keyData())) {
            var expectedMac = computeMac(response.collectionName(), response.version(), expectedHash, keys.snapshotMacKey());
            if (!MessageDigest.isEqual(snapshot.mac(), expectedMac)) {
                throw new WebAppStateFatalSyncException("Snapshot MAC mismatch");
            }
        }
    }

    private void verifyPatchMac(PatchType type, PatchSync patch, byte[] expectedHash) {
        if (patch.patchMac() == null) {
            return;  // No MAC to verify
        }

        var keyId = patch.keyId();
        if (keyId == null || keyId.id() == null) {
            throw new InternalError("Patch missing key ID");
        }

        // Get sync key
        var syncKey = store.findWebAppStateKeyById(keyId.id())
                .orElseThrow(() -> new InternalError("Unknown sync key for patch"));

        var keyData = syncKey.keyData();
        if (keyData == null || keyData.keyData() == null) {
            throw new InternalError("Sync key has no data");
        }

        // Derive keys to get patch MAC key
        try (var keys = MutationKeys.ofSyncKey(keyData.keyData())) {
            var expectedMac = computeMac(type, patch.encodedVersion(), expectedHash, keys.patchMacKey());
            if (!MessageDigest.isEqual(patch.patchMac(), expectedMac)) {
                throw new WebAppStateFatalSyncException("Patch MAC mismatch");
            }
        }
    }

    private byte[] computeMac(PatchType type, long version, byte[] expectedHash, SecretKeySpec secretKey) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);

            mac.update((byte) (version >> 56));
            mac.update((byte) (version >> 48));
            mac.update((byte) (version >> 40));
            mac.update((byte) (version >> 32));
            mac.update((byte) (version >> 24));
            mac.update((byte) (version >> 16));
            mac.update((byte) (version >> 8));
            mac.update((byte) version);

            mac.update(type.toBytes());

            mac.update(expectedHash);

            return mac.doFinal();
        }catch (GeneralSecurityException exception) {
            throw new WebAppStateFatalSyncException("Failed to compute MAC", exception);
        }
    }
}
