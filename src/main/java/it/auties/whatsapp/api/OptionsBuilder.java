package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.signal.auth.UserAgent.ReleaseChannel;

import java.net.URI;

@SuppressWarnings("unused")
public sealed class OptionsBuilder<T extends OptionsBuilder<T>> permits MobileOptionsBuilder, WebOptionsBuilder {
    Store store;
    Keys keys;
    ErrorHandler errorHandler;

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
        store.setName(name);
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
        store.setTextPreviewSetting(textPreviewSetting);
        return (T) this;
    }

    /**
     * Sets whether the provided proxy should be used for media uploads/downloads
     * By default, the proxy is used for both uploads and downloads
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T mediaProxySetting(MediaProxySetting mediaProxySetting) {
        store.setMediaProxySetting(mediaProxySetting);
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
     * Sets the release channel
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T releaseChannel(ReleaseChannel releaseChannel) {
        store.setReleaseChannel(releaseChannel);
        return (T) this;
    }

    /**
     * Sets the proxy to use for the socket
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T proxy(URI proxy) {
        store.setProxy(proxy);
        return (T) this;
    }

    /**
     * Whether presence updates should be handled automatically
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T automaticPresenceUpdates(boolean automaticPresenceUpdates) {
        store.setAutomaticPresenceUpdates(automaticPresenceUpdates);
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
        store.setCheckPatchMacs(checkPatchMacs);
        return (T) this;
    }
}