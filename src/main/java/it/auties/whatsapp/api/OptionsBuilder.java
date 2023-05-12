package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.listener.RegisterListener;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentReleaseChannel;
import it.auties.whatsapp.model.signal.auth.Version;
import lombok.NonNull;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;

@SuppressWarnings("unused")
public sealed class OptionsBuilder<T extends OptionsBuilder<T>> permits MobileOptionsBuilder, WebOptionsBuilder {
    protected Store store;
    protected Keys keys;

    public OptionsBuilder(UUID connectionUuid, ControllerSerializer serializer, ConnectionType connectionType, ClientType clientType){
        var uuid = getCorrectUuid(connectionUuid, serializer, connectionType, clientType);
        this.store = Store.of(uuid, null, clientType, serializer, connectionType == ConnectionType.KNOWN);
        this.keys = Keys.of(uuid, null, clientType, serializer, connectionType == ConnectionType.KNOWN);
    }

    public OptionsBuilder(long phoneNumber, ControllerSerializer serializer, ConnectionType connectionType, ClientType clientType) {
        var uuid = getCorrectUuid(null, serializer, connectionType, clientType);
        this.store = Store.of(uuid, phoneNumber, clientType, serializer, connectionType == ConnectionType.KNOWN);
        this.keys = Keys.of(uuid, phoneNumber, clientType, serializer, connectionType == ConnectionType.KNOWN);
    }

    private static UUID getCorrectUuid(UUID uuid, ControllerSerializer serializer, ConnectionType connectionType, ClientType clientType) {
        return switch (connectionType){
            case NEW -> Objects.requireNonNullElseGet(uuid, UUID::randomUUID);
            case KNOWN -> uuid;
            case FIRST -> Objects.requireNonNullElseGet(serializer.listIds(clientType).peekFirst(), () -> Objects.requireNonNullElseGet(uuid, UUID::randomUUID));
            case LAST -> Objects.requireNonNullElseGet(serializer.listIds(clientType).peekLast(), () -> Objects.requireNonNullElseGet(uuid, UUID::randomUUID));
        };
    }

    /**
     * Sets the name to provide to Whatsapp during the authentication process
     * The web api will display this name in the devices section, while the mobile api will show it to the people you send messages to
     * By default, this value will be set to this library's name
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T name(@NonNull String name) {
        store.name(name);
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
    public T version(@NonNull Version version) {
        store.version(version);
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
        store.autodetectListeners(autodetectListeners);
        return (T) this;
    }

    /**
     * Sets whether a preview should be automatically generated and attached to text messages that contain links
     * By default, it's enabled with inference
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T textPreviewSetting(@NonNull TextPreviewSetting textPreviewSetting) {
        store.textPreviewSetting(textPreviewSetting);
        return (T) this;
    }

    /**
     * Sets the error handler for this session
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T errorHandler(@NonNull ErrorHandler errorHandler) {
        store.errorHandler(errorHandler);
        return (T) this;
    }


    /**
     * Sets the executor to use for the socket
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T socketExecutor(@NonNull Executor socketExecutor) {
        store.socketExecutor(socketExecutor);
        return (T) this;
    }

    /**
     * Sets the proxy to use for the socket
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T proxy(@NonNull URI proxy) {
        store.proxy(proxy);
        return (T) this;
    }

    /**
     * Sets the release channel
     *
     * @return the same instance for chaining
     */
    @SuppressWarnings("unchecked")
    public T proxy(@NonNull UserAgentReleaseChannel releaseChannel) {
        store.releaseChannel(releaseChannel);
        return (T) this;
    }
}