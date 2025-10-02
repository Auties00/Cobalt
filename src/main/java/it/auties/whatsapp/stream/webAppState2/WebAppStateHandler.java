package it.auties.whatsapp.stream.webAppState2;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.exception.WebAppStatePushException;
import it.auties.whatsapp.io.node.NodeBuilder;
import it.auties.whatsapp.io.state.WebAppStateEncoder;
import it.auties.whatsapp.model.sync.AppStateSyncHash;
import it.auties.whatsapp.model.sync.PatchType;

import java.util.List;
import java.util.Objects;

public final class WebAppStateHandler extends AbstractHandler {
    public WebAppStateHandler(Whatsapp whatsapp) {
        super(whatsapp);
    }

    public synchronized void push(PatchType type, List<WebAppStatePatch> patches) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(patches, "patches cannot be null");
        if(patches.isEmpty()) {
            throw new IllegalArgumentException("patches cannot be empty");
        }
        
        var appStateSyncKeys = whatsapp.keys().appStateKeys();
        if(appStateSyncKeys.isEmpty()) {
            throw new WebAppStatePushException("No app state keys found");
        }
        var appStateSyncKey = appStateSyncKeys.getLast();

        var oldAppStateSyncHash = whatsapp.keys()
                .findWebAppHashStateByName(type)
                .orElse(null);
        var newAppStateSyncHash = oldAppStateSyncHash != null
                ? oldAppStateSyncHash.clone()
                : new AppStateSyncHash(type);

        var encodedPatch = WebAppStateEncoder.encode(appStateSyncKey, newAppStateSyncHash, patches);

        var patchBody = new NodeBuilder()
                .description("patch")
                .content(encodedPatch)
                .build();
        var syncBody = new NodeBuilder()
                .description("collection")
                .attribute("name", type.name())
                .attribute("version", oldAppStateSyncHash != null ? oldAppStateSyncHash.version() : 0)
                .attribute("return_snapshot", false)
                .content(patchBody)
                .build();
        var queryRequestBody = new NodeBuilder()
                .description("sync")
                .content(syncBody)
                .build();
        var queryRequest = new NodeBuilder()
                .description("iq")
                .attribute("method", "set")
                .attribute("xmlns", "w:sync:app:state")
                .content(queryRequestBody)
                .build();
        var queryResponse = whatsapp.sendNode(queryRequest);
        var queryResponseBody = queryResponse
                .findChildByDescription("sync", "collection")
                .orElseThrow(() -> new WebAppStatePushException("Unexpected response: " + queryResponse));
        var queryResponseType = queryResponseBody.getOptionalAttribute("type");
        if(queryResponseType.isPresent() && Objects.equals(queryResponseType.get().toString(), "error")) {
            throw new WebAppStatePushException("Failed to push app state: " + queryResponseBody);
        }

        pull(type);
    }

    public synchronized void pull(PatchType type) {
        Objects.requireNonNull(type, "type cannot be null");

        var oldAppStateSyncHash = whatsapp.keys()
                .findWebAppHashStateByName(type)
                .orElse(null);
        var newAppStateSyncHash = oldAppStateSyncHash != null
                ? oldAppStateSyncHash.clone()
                : new AppStateSyncHash(type);

        var hasMore = true;
        var wantSnapshot = oldAppStateSyncHash == null;
        while(hasMore) {
            var syncBody = new NodeBuilder()
                    .description("collection")
                    .attribute("name", type.name())
                    .attribute("version", newAppStateSyncHash.version(), !wantSnapshot)
                    .attribute("return_snapshot", wantSnapshot)
                    .build();
            var queryRequestBody = new NodeBuilder()
                    .description("sync")
                    .content(syncBody)
                    .build();
            var queryRequest = new NodeBuilder()
                    .description("iq")
                    .attribute("method", "set")
                    .attribute("xmlns", "w:sync:app:state")
                    .content(queryRequestBody)
                    .build();
            var queryResponse = whatsapp.sendNode(queryRequest);

            // hasMore = ...


        }

        //	for hasMore {
        //		patches, err := cli.fetchAppStatePatches(ctx, name, state.Version, wantSnapshot)
        //		wantSnapshot = false
        //		if err != nil {
        //			return fmt.Errorf("failed to fetch app state %s patches: %w", name, err)
        //		}
        //		hasMore = patches.HasMorePatches
        //
        //		mutations, newState, err := cli.appStateProc.DecodePatches(ctx, patches, state, true)
        //		if err != nil {
        //			if errors.Is(err, appstate.ErrKeyNotFound) {
        //				go cli.requestMissingAppStateKeys(context.WithoutCancel(ctx), patches)
        //			}
        //			return fmt.Errorf("failed to decode app state %s patches: %w", name, err)
        //		}
        //		wasFullSync := state.Version == 0 && patches.Snapshot != nil
        //		state = newState
        //		if name == appstate.WAPatchCriticalUnblockLow && wasFullSync && !cli.EmitAppStateEventsOnFullSync {
        //			var contacts []store.ContactEntry
        //			mutations, contacts = cli.filterContacts(mutations)
        //			cli.Log.Debugf("Mass inserting app state snapshot with %d contacts into the store", len(contacts))
        //			err = cli.Store.Contacts.PutAllContactNames(ctx, contacts)
        //			if err != nil {
        //				// This is a fairly serious failure, so just abort the whole thing
        //				return fmt.Errorf("failed to update contact store with data from snapshot: %v", err)
        //			}
        //		}
        //		for _, mutation := range mutations {
        //			cli.dispatchAppState(ctx, mutation, fullSync, cli.EmitAppStateEventsOnFullSync)
        //		}
        //	}
        //	if fullSync {
        //		cli.Log.Debugf("Full sync of app state %s completed. Current version: %d", name, state.Version)
        //		cli.dispatchEvent(&events.AppStateSyncComplete{Name: name})
        //	} else {
        //		cli.Log.Debugf("Synced app state %s from version %d to %d", name, version, state.Version)
        //	}
        //	return nil
    }

    public synchronized void pull() {

    }

    @Override
    public void dispose() {

    }
}
