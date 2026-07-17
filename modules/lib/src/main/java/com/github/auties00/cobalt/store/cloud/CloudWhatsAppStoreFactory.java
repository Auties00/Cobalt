package com.github.auties00.cobalt.store.cloud;

import com.github.auties00.cobalt.store.WhatsAppStoreFactory;
import com.github.auties00.cobalt.store.cloud.protobuf.PersistentCloudWhatsAppStoreFactory;
import com.github.auties00.cobalt.store.cloud.protobuf.TemporaryCloudWhatsAppStoreFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The factory contract for constructing and loading {@link CloudWhatsAppStore} instances.
 *
 * <p>This is the Cloud branch of the sealed {@link WhatsAppStoreFactory} hierarchy; its Linked
 * counterpart is {@link com.github.auties00.cobalt.store.linked.LinkedWhatsAppStoreFactory}. A Cloud
 * session is keyed by its phone number id, so the {@link #load(String) load} and {@link #create create}
 * methods address sessions by that id rather than by the client type and UUID the Linked factory uses.
 *
 * @apiNote
 * Embedders pick a storage strategy through one of the static factory methods on this interface and use
 * the returned {@link CloudWhatsAppStoreFactory} to either {@link #create create} a fresh session from
 * its credentials or {@link #load load} an existing one. The supplied implementations cover the RAM-only
 * and on-disk-snapshot variants; embedders should not depend on the concrete factory types directly.
 *
 * @implSpec
 * Implementations must produce stores that conform to the {@link CloudWhatsAppStore} contract. The
 * {@code load} methods must return {@link Optional#empty()} when no session matches the argument; the
 * {@code create} method must always return a fresh, non-{@code null} store seeded with the supplied
 * credentials and ready to be configured by the rest of the client.
 *
 * @see CloudWhatsAppStore
 * @see WhatsAppStoreFactory
 */
public non-sealed interface CloudWhatsAppStoreFactory extends WhatsAppStoreFactory {
    /**
     * Returns a factory that keeps the entire store in RAM and never touches disk.
     *
     * @apiNote
     * Suitable for tests, ephemeral bots and scratch programs. Restarting the program loses the
     * credentials and the per-chat read markers.
     *
     * @return a stateless RAM-only factory
     */
    static CloudWhatsAppStoreFactory temporary() {
        return TemporaryCloudWhatsAppStoreFactory.INSTANCE;
    }

    /**
     * Returns a factory that snapshots each session to a {@code store.proto} file under the default
     * Cobalt session directory.
     *
     * @apiNote
     * The default directory is {@code $HOME/.cobalt/proto/cloud}; embedders that need a custom location
     * pick {@link #persistent(Path)} instead.
     *
     * @return a persistent factory using the default storage directory
     */
    static CloudWhatsAppStoreFactory persistent() {
        return new PersistentCloudWhatsAppStoreFactory();
    }

    /**
     * Returns a factory that snapshots each session to a {@code store.proto} file under the given
     * directory.
     *
     * @apiNote
     * Each session lives under {@code <directory>/<phoneNumberId>/} so multiple Cloud sessions under the
     * same root never collide.
     *
     * @param directory the root directory under which per-session folders are created
     * @return a persistent factory using the given storage directory
     */
    static CloudWhatsAppStoreFactory persistent(Path directory) {
        return new PersistentCloudWhatsAppStoreFactory(directory);
    }

    /**
     * Loads an existing session store identified by its phone number id.
     *
     * @implSpec
     * Implementations must not return {@code null}; a missing session is signalled with
     * {@link Optional#empty()}. Implementations may perform IO and propagate {@link IOException} when
     * the on-disk state is malformed or unreadable.
     *
     * @param phoneNumberId the phone number id the session was created with
     * @return the loaded store, or {@link Optional#empty()} if no session exists for that id
     * @throws IOException if the store file cannot be read or decoded
     */
    Optional<CloudWhatsAppStore> load(String phoneNumberId) throws IOException;

    /**
     * Loads the most recently created or loaded session.
     *
     * @apiNote
     * Useful for resume flows where the embedder does not track the phone number id externally. Returns
     * {@link Optional#empty()} when no session has been recorded yet.
     *
     * @implSpec
     * Implementations must not return {@code null}; a missing session is signalled with
     * {@link Optional#empty()}. Implementations may perform IO and propagate {@link IOException} when
     * the on-disk state is malformed or unreadable.
     *
     * @return the most recent store, or {@link Optional#empty()} if no session has been recorded
     * @throws IOException if the store file cannot be read or decoded
     */
    Optional<CloudWhatsAppStore> loadLatest() throws IOException;

    /**
     * Creates a new session store seeded with the given credentials.
     *
     * @apiNote
     * The returned store carries only the two required credentials plus the default graph API version;
     * the remaining Graph and webhook configuration is applied by the client builder before the client
     * is materialised.
     *
     * @implSpec
     * Implementations must return a freshly built, non-{@code null} store; persistent implementations
     * must additionally record the session so a subsequent {@link #load(String)} or {@link #loadLatest()}
     * resolves it.
     *
     * @param accessToken   the system-user access token
     * @param phoneNumberId the operating phone number id
     * @return the newly created store
     * @throws IOException if the session directory cannot be created
     */
    CloudWhatsAppStore create(String accessToken, String phoneNumberId) throws IOException;
}
