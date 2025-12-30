package com.github.auties00.cobalt.device.hash;

import com.github.auties00.cobalt.model.jid.Jid;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Helper class for calculating participant hashes (phash)
 */
public final class DevicePhashEncoder {
    private static final String PHASH_PREFIX = "2:";

    private DevicePhashEncoder() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Calculates the participant hash (phash) for a list of device JIDs.
     * The phash is used by the server to verify that the client has the correct device list.
     * JIDs are sorted before hashing to ensure consistent results.
     *
     * @param deviceJids the list of device JIDs to hash
     * @return the phash string in format "2:base64(...)"
     */
    public static String calculatePhash(Collection<? extends Jid> deviceJids) {
        Objects.requireNonNull(deviceJids, "deviceJids cannot be null");
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            deviceJids.stream()
                    .map(Jid::toString)
                    .sorted()  // Sort JIDs to ensure consistent hash regardless of collection order
                    .forEachOrdered(entry -> digest.update(entry.getBytes(StandardCharsets.UTF_8)));
            var hash = digest.digest();
            var truncatedHash = Arrays.copyOf(hash, 6);
            var base64 = Base64.getEncoder().encodeToString(truncatedHash);
            return PHASH_PREFIX + base64;
        } catch (NoSuchAlgorithmException _) {
            throw new InternalError("No SHA-256 algorithm available");
        }
    }

    /**
     * Calculates the participant hash for a group message.
     *
     * @param groupJid     the JID of the group (unused, kept for API consistency)
     * @param senderDevice the JID of the sender's device
     * @param allDevices   the list of all device JIDs that will receive the message
     * @return the phash string
     */
    public static String calculateGroupPhash(Jid groupJid, Jid senderDevice, Collection<? extends Jid> allDevices) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(senderDevice, "senderDevice cannot be null");
        Objects.requireNonNull(allDevices, "allDevices cannot be null");

        var devicesWithSender = new HashSet<Jid>(allDevices);
        devicesWithSender.add(senderDevice);
        return calculatePhash(devicesWithSender);
    }
}
