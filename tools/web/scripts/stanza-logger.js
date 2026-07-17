// noinspection JSUnusedGlobalSymbols

/**
 * WhatsApp Web Stanza Logger
 *
 * Intercepts and logs all incoming and outgoing binary XML stanzas
 * by monkey-patching WAWap.encodeStanza (outgoing) and
 * WAWap.decodeStanza (incoming).
 *
 * Usage: Paste into the browser console while WhatsApp Web is open.
 *
 * Configuration:
 *   window.stanzaLogger.pause()        - Pause logging
 *   window.stanzaLogger.resume()       - Resume logging
 *   window.stanzaLogger.filter = null  - Log all stanzas (default)
 *   window.stanzaLogger.filter = "iq"  - Log only stanzas with tag "iq"
 *   window.stanzaLogger.filter = /msg/ - Log stanzas whose tag matches regex
 */
(function () {
    "use strict";

    // Resolve the WAWap module via Metro bundler's global require
    const WAWap = self.require("WAWap");
    if (!WAWap || typeof WAWap.encodeStanza !== "function") {
        console.error("[StanzaLogger] Could not find WAWap module");
        return;
    }

    // Enable the built-in XML pretty-printer so WapNode.toString() is readable
    if (typeof WAWap.enableXMLFormat === "function") {
        WAWap.enableXMLFormat();
    }

    const state = {
        paused: false,
        filter: null,  // null = all, string = exact tag match, RegExp = tag regex
        outCount: 0,
        inCount: 0
    };

    function matchesFilter(tag) {
        if (state.filter == null) return true;
        if (typeof state.filter === "string") return tag === state.filter;
        if (state.filter instanceof RegExp) return state.filter.test(tag);
        return true;
    }

    function ts() {
        return new Date().toISOString().slice(11, 23);
    }

    function summarizeContent(node) {
        const content = node.content;
        if (content == null) return null;
        if (content instanceof Uint8Array) return "<binary " + content.byteLength + " bytes>";
        if (Array.isArray(content)) return content.length + " child node(s)";
        return String(content);
    }

    const origEncode = WAWap.encodeStanza;
    WAWap.encodeStanza = function (stanza) {
        if (!state.paused && stanza && stanza.tag && matchesFilter(stanza.tag)) {
            state.outCount++;
            const id = stanza.attrs && stanza.attrs.id ? " id=" + stanza.attrs.id : "";
            console.groupCollapsed(
                "%c[OUT %s] %s%s",
                "color:#ff6b6b;font-weight:bold",
                ts(),
                stanza.tag,
                id
            );
            console.log(stanza.toString());
            console.log("Attrs:", Object.assign({}, stanza.attrs));
            const summary = summarizeContent(stanza);
            if (summary) console.log("Content:", summary);
            console.groupEnd();
        }
        return origEncode.apply(this, arguments);
    };

    const origDecode = WAWap.decodeStanza;
    WAWap.decodeStanza = function () {
        const result = origDecode.apply(this, arguments);
        return result.then(function (stanza) {
            if (!state.paused && stanza && stanza.tag && matchesFilter(stanza.tag)) {
                state.inCount++;
                const id = stanza.attrs && stanza.attrs.id ? " id=" + stanza.attrs.id : "";
                console.groupCollapsed(
                    "%c[IN  %s] %s%s",
                    "color:#51cf66;font-weight:bold",
                    ts(),
                    stanza.tag,
                    id
                );
                console.log(stanza.toString());
                console.log("Attrs:", Object.assign({}, stanza.attrs));
                const summary = summarizeContent(stanza);
                if (summary) console.log("Content:", summary);
                console.groupEnd();
            }
            return stanza;
        });
    };

    window.stanzaLogger = {
        get filter() { return state.filter; },
        set filter(v) { state.filter = v; },
        pause:  function () { state.paused = true;  console.log("[StanzaLogger] Paused"); },
        resume: function () { state.paused = false; console.log("[StanzaLogger] Resumed"); },
        stats:  function () {
            console.log("[StanzaLogger] OUT: %d  IN: %d", state.outCount, state.inCount);
        },
        destroy: function () {
            WAWap.encodeStanza = origEncode;
            WAWap.decodeStanza = origDecode;
            delete window.stanzaLogger;
            console.log("[StanzaLogger] Removed");
        }
    };

    console.log(
        "%c[StanzaLogger]%c Installed \u2014 logging all stanzas. Use window.stanzaLogger for controls.",
        "color:#ff6b6b;font-weight:bold",
        "color:inherit"
    );
})();
