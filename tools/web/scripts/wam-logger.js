// noinspection JSUnusedGlobalSymbols

/**
 * WhatsApp Web WAM (WhatsApp Analytics/Metrics) Event Logger
 *
 * Intercepts all WAM telemetry events by monkey-patching
 * WamEvent.prototype.commit and commitAndWaitForFlush.
 *
 * Usage: Paste into the browser console while WhatsApp Web is open.
 *
 * Configuration:
 *   window.wamLogger.pause()              - Pause logging
 *   window.wamLogger.resume()             - Resume logging
 *   window.wamLogger.filter = null        - Log all events (default)
 *   window.wamLogger.filter = "MessageSend" - Log events whose class name contains the string
 *   window.wamLogger.filter = /^Wam/      - Log events whose class name matches the regex
 */
(function () {
    "use strict";

    // Resolve modules via Metro bundler's global require
    const WamEventModule = self.require("WAWebWamCodegenWamEvent");
    if (!WamEventModule || typeof WamEventModule.WamEvent !== "function") {
        console.error("[WamLogger] Could not find WAWebWamCodegenWamEvent module");
        return;
    }

    const FormatterModule = self.require("WAWebWamEnumFormatter");

    const WamEvent = WamEventModule.WamEvent;
    const formatEvent = FormatterModule
        ? FormatterModule.formatWamEventForLogging
        : null;

    const state = {
        paused: false,
        filter: null,  // null = all, string = substring match on $className, RegExp = regex
        count: 0
    };

    function matchesFilter(name) {
        if (state.filter == null) return true;
        if (typeof state.filter === "string") return name.indexOf(state.filter) !== -1;
        if (state.filter instanceof RegExp) return state.filter.test(name);
        return true;
    }

    function ts() {
        return new Date().toISOString().slice(11, 23);
    }

    // Build a clean representation of the event's properties
    function formatProperties(event) {
        const props = formatEvent ? formatEvent(event.all, event) : event.all;
        // Strip null/undefined entries for readability
        const clean = {};
        for (let key in props) {
            if (props[key] != null) {
                clean[key] = props[key];
            }
        }
        return clean;
    }

    const CHANNEL_COLORS = {
        regular: "#74c0fc",
        realtime: "#f783ac",
        private: "#b197fc"
    };

    function logEvent(event) {
        if (state.paused) return;

        const name = event.$className || "Unknown";
        if (!matchesFilter(name)) return;

        state.count++;

        const channel = event.wamChannel || "regular";
        const channelColor = CHANNEL_COLORS[channel] || "#868e96";
        const props = formatProperties(event);

        console.groupCollapsed(
            "%c[WAM %s]%c %s %c%s",
            "color:#ffd43b;font-weight:bold",
            ts(),
            "color:inherit;font-weight:bold",
            name,
            "color:" + channelColor + ";font-weight:normal",
            "[" + channel + "]"
        );
        console.log("Event ID:", event.id);
        console.log("Weight:", event.weight);
        console.log("Channel:", channel);
        console.table(props);
        console.groupEnd();
    }

    const origCommit = WamEvent.prototype.commit;
    WamEvent.prototype.commit = function () {
        logEvent(this);
        return origCommit.apply(this, arguments);
    };

    const origCommitFlush = WamEvent.prototype.commitAndWaitForFlush;
    WamEvent.prototype.commitAndWaitForFlush = function () {
        logEvent(this);
        return origCommitFlush.apply(this, arguments);
    };

    window.wamLogger = {
        get filter() { return state.filter; },
        set filter(v) { state.filter = v; },
        pause:  function () { state.paused = true;  console.log("[WamLogger] Paused"); },
        resume: function () { state.paused = false; console.log("[WamLogger] Resumed"); },
        stats:  function () { console.log("[WamLogger] Events logged: %d", state.count); },
        destroy: function () {
            WamEvent.prototype.commit = origCommit;
            WamEvent.prototype.commitAndWaitForFlush = origCommitFlush;
            delete window.wamLogger;
            console.log("[WamLogger] Removed");
        }
    };

    console.log(
        "%c[WamLogger]%c Installed \u2014 logging all WAM events. Use window.wamLogger for controls.",
        "color:#ffd43b;font-weight:bold",
        "color:inherit"
    );
})();
