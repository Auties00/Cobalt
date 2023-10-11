package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.KeysBuilder;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.controller.StoreBuilder;
import it.auties.whatsapp.listener.RegisterListener;
import it.auties.whatsapp.model.signal.auth.UserAgent.ReleaseChannel;
import it.auties.whatsapp.model.signal.auth.Version;

import java.net.URI;
import java.util.concurrent.Executor;

@SuppressWarnings("unused")
public sealed class OptionsBuilder<T extends OptionsBuilder<T>> permits MobileOptionsBuilder, WebOptionsBuilder {
    Store store;
    Keys keys;
    StoreBuilder storeBuilder;
    KeysBuilder keysBuilder;
    ErrorHandler errorHandler;
    Executor socketExecutor;

    OptionsBuilder(StoreBuilder storeBuilder, KeysBuilder keysBuilder) {
        this.storeBuilder = storeBuilder;
        this.keysBuilder = keysBuilder;
    }

    OptionsBuilder(Store store, Keys keys) {
        this.store = store;
        this.keys = keys;
    }

    /**
     * Sets the name to provide to Whatsapp during the authentication process
     * The web api will display this name in the devices section, while the mobile api will show it to the people you send messages to
     * By default, this value will be set to this library's name
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T name(String name) {
        if (store != null) {
            store.setName(name);
        } else {
            storeBuilder.name(name);
        }
        return (T) this;
    }

    /**
     * Sets the version of Whatsapp to use
     * If the version is too outdated, the server will refuse to connect
     * If you are using the mobile api and the version doesn't match the hash, the server will refuse to connect
     * By default the latest stable version will be used
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T version(Version version) {
        if (store != null) {
            store.setVersion(version);
        } else {
            storeBuilder.version(version);
        }
        return (T) this;
    }

    /**
     * Sets whether listeners marked with the {@link RegisterListener} annotation should be automatically detected and registered
     * By default, this option is enabled
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T autodetectListeners(boolean autodetectListeners) {
        if (store != null) {
            store.setAutodetectListeners(autodetectListeners);
        } else {
            storeBuilder.autodetectListeners(autodetectListeners);
        }
        return (T) this;
    }

    /**
     * Sets whether a preview should be automatically generated and attached to text messages that contain links
     * By default, it's enabled with inference
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T textPreviewSetting(TextPreviewSetting textPreviewSetting) {
        if (store != null) {
            store.setTextPreviewSetting(textPreviewSetting);
        } else {
            storeBuilder.textPreviewSetting(textPreviewSetting);
        }
        return (T) this;
    }

    /**
     * Sets the error handler for this session
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T errorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return (T) this;
    }


    /**
     * Sets the executor to use for the socket
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T socketExecutor(Executor socketExecutor) {
        this.socketExecutor = socketExecutor;
        return (T) this;
    }

    /**
     * Sets the release channel
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T releaseChannel(ReleaseChannel releaseChannel) {
        if (store != null) {
            store.setReleaseChannel(releaseChannel);
        } else {
            storeBuilder.releaseChannel(releaseChannel);
        }
        return (T) this;
    }

    /**
     * Sets the proxy to use for the socket
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T proxy(URI proxy) {
        if (store != null) {
            store.setProxy(proxy);
        } else {
            storeBuilder.proxy(proxy);
        }
        return (T) this;
    }

    /**
     * Whether presence updates should be handled automatically
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T automaticPresenceUpdates(boolean automaticPresenceUpdates) {
        if (store != null) {
            store.setAutomaticPresenceUpdates(automaticPresenceUpdates);
        } else {
            storeBuilder.automaticPresenceUpdates(automaticPresenceUpdates);
        }
        return (T) this;
    }

    /**
     * Sets whether the mac of every app state patch should be validated or not
     * By default, it's set to false
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T checkPatchMacks(boolean checkPatchMacs) {
        if (store != null) {
            store.setCheckPatchMacs(checkPatchMacs);
        } else {
            storeBuilder.checkPatchMacs(checkPatchMacs);
        }
        return (T) this;
    }
}