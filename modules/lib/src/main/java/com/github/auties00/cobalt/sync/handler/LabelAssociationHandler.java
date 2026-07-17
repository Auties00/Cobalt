package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.preference.Label;
import com.github.auties00.cobalt.wire.linked.preference.LabelBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.LabelAssociationAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppContactStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code label_jid} app-state sync action that pins or unpins a
 * chat or contact under a given chat label.
 *
 * <p>The association fans out across the {@link SyncPatchType#REGULAR}
 * collection so companion devices show the same label badge. The mutation
 * index encodes the label id and the target JID, formatted as
 * {@snippet :
 *     ["label_jid", labelId, chatJid]
 * }
 *
 * @implNote
 * This implementation stores associations directly on the
 * {@link Label#assignments()} set rather than in a separate label-association
 * table, and resolves the target JID chat-table-first then through a
 * LID-to-phone fallback. The auto-label system notification for predefined
 * labels is not modelled.
 */
@WhatsAppWebModule(moduleName = "WAWebLabelJidSync")
public final class LabelAssociationHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link LabelAssociationHandler}.
     */
    private static final System.Logger LOGGER = Log.get(LabelAssociationHandler.class);

    /**
     * The position of the target JID inside the parsed mutation index array.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelJidSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    private static final int CHAT_JID_INDEX = 2;

    /**
     * Constructs a new singleton {@link LabelAssociationHandler}.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelJidSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public LabelAssociationHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLabelJidSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return LabelAssociationAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLabelJidSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return LabelAssociationAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLabelJidSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return LabelAssociationAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Rejects non-{@link SyncdOperation#SET} operations as
     * {@link MutationApplicationResult#unsupported()}, a missing label id or
     * target JID as malformed, and an unparseable target JID as malformed. The
     * target JID is resolved chat-table-first, then through
     * {@link LinkedWhatsAppContactStore#findPhoneByLid(Jid)}
     * when it carries the LID server. When {@link LabelAssociationAction#labeled()}
     * is set the resolved JID is added via {@link Label#addAssignment(Jid)};
     * otherwise it is removed via {@link Label#removeAssignment(Jid)}.
     *
     * @implNote
     * This implementation classifies a missing index slot as
     * {@link MutationApplicationResult#malformed()} before parsing so the outer
     * try/catch does not turn it into {@link MutationApplicationResult#failed()}
     * via an {@code IndexOutOfBoundsException} from {@code JSON.parseArray}.
     * Because {@link LabelAssociationAction#labeled()} coalesces a {@code null}
     * protobuf field to {@code false}, a {@code null} flag is treated as a
     * removal rather than a malformed value. When the label is not yet known a
     * stub {@link Label} with empty name and zero colour is created so the
     * association can be recorded ahead of the corresponding label edit.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLabelJidSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        try {
            if (mutation.operation() != SyncdOperation.SET) {
                return MutationApplicationResult.unsupported();
            }

            var indexArray = JSON.parseArray(mutation.index());
            if (indexArray.size() <= CHAT_JID_INDEX) {
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }
            var labelId = indexArray.getString(1);
            var targetJidString = indexArray.getString(CHAT_JID_INDEX);
            if (labelId == null || labelId.isEmpty() || targetJidString == null || targetJidString.isEmpty()) {
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof LabelAssociationAction action)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "label association mutation has malformed action value, label={0}", labelId);
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            var labeled = action.labeled();

            Jid targetJid;
            try {
                targetJid = Jid.of(targetJidString);
            } catch (Exception jidError) {
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }
            if (targetJid == null) {
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            var resolvedTargetJid = targetJid;
            var resolvedChat = client.store().chatStore().findChatByJid(targetJid);
            if (resolvedChat.isPresent()) {
                resolvedTargetJid = resolvedChat.get().toJid();
            } else if (targetJid.hasLidServer()) {
                var phoneJid = client.store().contactStore().findPhoneByLid(targetJid);
                if (phoneJid.isPresent()) {
                    resolvedTargetJid = phoneJid.get();
                }
            }

            var label = client.store().settingsStore().findLabel(labelId)
                    .orElseGet(() -> {
                        var newLabel = new LabelBuilder()
                                .id(labelId)
                                .name("")
                                .color(0)
                                .build();
                        client.store().settingsStore().addLabel(newLabel);
                        return newLabel;
                    });

            if (labeled) {
                label.addAssignment(resolvedTargetJid);
            } else {
                label.removeAssignment(resolvedTargetJid);
            }

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "label association applied, label={0} target={1} labeled={2}", labelId, resolvedTargetJid, labeled);
            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "label association mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

}
