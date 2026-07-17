export const AB_PROPS_SCRIPT = String.raw`
// noinspection JSUnusedGlobalSymbols

(function () {
    "use strict";

    if (window.abProps && window.abProps.__webMcpInstalled) {
        return;
    }

    const ABPropsConfigs = self.require("WAWebABPropsConfigs");
    if (!ABPropsConfigs || !ABPropsConfigs.ABPropConfigs) {
        console.error("[ABProps] Could not find WAWebABPropsConfigs module");
        return;
    }

    const ABProps = self.require("WAWebABProps");
    if (!ABProps || typeof ABProps.getABPropConfigValue !== "function") {
        console.error("[ABProps] Could not find WAWebABProps module");
        return;
    }

    const ABPropsCache = self.require("WAWebABPropsCache");
    if (!ABPropsCache || typeof ABPropsCache.getAllABPropsMap !== "function") {
        console.error("[ABProps] Could not find WAWebABPropsCache module");
        return;
    }

    const ApiAbPropConfig = self.require("WAWebApiAbPropConfig");
    if (!ApiAbPropConfig || typeof ApiAbPropConfig.parseConfigValue !== "function") {
        console.error("[ABProps] Could not find WAWebApiAbPropConfig module");
        return;
    }

    const configs = ABPropsConfigs.ABPropConfigs;

    function matchesFilter(name, filter) {
        if (filter == null) return true;
        if (typeof filter === "string") return name.indexOf(filter) !== -1;
        if (filter instanceof RegExp) return filter.test(name);
        return true;
    }

    function getDefinition(name) {
        const def = configs[name];
        if (def == null) return null;
        return { code: def[0], type: def[1], defaultValue: def[2], debugDefault: def[3] };
    }

    function getCurrentValue(name) {
        return ABProps.getABPropConfigValue(name);
    }

    function formatValue(value) {
        if (value === true) return "true";
        if (value === false) return "false";
        if (value == null) return "null";
        return String(value);
    }

    function list(filter) {
        const names = Object.keys(configs).filter(function (n) {
            return matchesFilter(n, filter);
        }).sort();

        return names.map(function (name) {
            const def = getDefinition(name);
            return {
                name: name,
                code: def.code,
                type: def.type,
                value: formatValue(getCurrentValue(name)),
                default: formatValue(def.defaultValue)
            };
        });
    }

    function get(name) {
        const def = getDefinition(name);
        if (def == null) {
            return undefined;
        }
        return getCurrentValue(name);
    }

    function set(name, value) {
        const def = getDefinition(name);
        if (def == null) {
            throw new Error("Unknown AB prop: " + name);
        }

        const cacheMap = ABPropsCache.getAllABPropsMap();
        const entry = cacheMap.get(def.code);
        const coerced = ApiAbPropConfig.parseConfigValue(String(value), def.type, def.defaultValue);

        if (entry) {
            entry.configValue = coerced;
        } else {
            cacheMap.set(def.code, {
                configCode:            def.code,
                configValue:           coerced,
                configExpoKey:         null,
                hasAccessed:           false,
                overriddenConfigValue: null
            });
        }

        return coerced;
    }

    function reset(name) {
        const def = getDefinition(name);
        if (def == null) {
            throw new Error("Unknown AB prop: " + name);
        }
        return set(name, def.defaultValue);
    }

    function resetAll() {
        const cacheMap = ABPropsCache.getAllABPropsMap();
        let count = 0;

        Object.keys(configs).forEach(function (name) {
            const def = getDefinition(name);
            const entry = cacheMap.get(def.code);
            if (entry) {
                entry.configValue = def.defaultValue;
                count += 1;
            }
        });

        return { resetCount: count };
    }

    function diff(filter) {
        const rows = [];
        Object.keys(configs).sort().forEach(function (name) {
            if (!matchesFilter(name, filter)) return;
            const def = getDefinition(name);
            const current = getCurrentValue(name);
            if (current !== def.defaultValue) {
                rows.push({
                    name: name,
                    code: def.code,
                    type: def.type,
                    value: formatValue(current),
                    default: formatValue(def.defaultValue)
                });
            }
        });
        return rows;
    }

    function definitions(filter) {
        return Object.keys(configs)
            .filter(function (name) { return matchesFilter(name, filter); })
            .sort()
            .map(function (name) {
                const def = getDefinition(name);
                return {
                    name: name,
                    code: def.code,
                    type: def.type,
                    defaultValue: def.defaultValue,
                    debugDefault: def.debugDefault
                };
            });
    }

    function query(options) {
        const opts = options || {};
        const filter = opts.filter;
        const diffOnly = opts.diffOnly === true;
        const limit = typeof opts.limit === "number" ? Math.max(1, opts.limit) : null;
        const rows = diffOnly ? diff(filter) : list(filter);
        if (limit == null) return rows;
        return rows.slice(0, limit);
    }

    window.abProps = {
        __webMcpInstalled: true,
        list: list,
        query: query,
        definitions: definitions,
        get: get,
        set: set,
        reset: reset,
        resetAll: resetAll,
        diff: diff
    };
})();

`;

export const STANZA_LOGGER_SCRIPT = String.raw`
// noinspection JSUnusedGlobalSymbols

(function () {
    "use strict";

    if (window.stanzaLogger && window.stanzaLogger.__webMcpInstalled) {
        return;
    }

    const WAWap = self.require("WAWap");
    if (!WAWap || typeof WAWap.encodeStanza !== "function") {
        console.error("[StanzaLogger] Could not find WAWap module");
        return;
    }

    const DeprecatedSendIq = self.require("WADeprecatedSendIq");
    if (!DeprecatedSendIq || typeof DeprecatedSendIq.deprecatedCastStanza !== "function") {
        console.error("[StanzaLogger] Could not find WADeprecatedSendIq module");
        return;
    }

    if (typeof WAWap.enableXMLFormat === "function") {
        WAWap.enableXMLFormat();
    }

    const state = {
        paused: false,
        filter: null,
        outCount: 0,
        inCount: 0,
        events: [],
        history: []
    };

    function matchesFilter(tag) {
        if (state.filter == null) return true;
        if (typeof state.filter === "string") return tag === state.filter;
        if (state.filter instanceof RegExp) return state.filter.test(tag);
        return true;
    }

    function ts() {
        return new Date().toISOString();
    }

    /** (bytes: Uint8Array, maxLen: number) -> hex string of the first maxLen bytes. */
    function hexPreviewOfUint8Array(bytes, maxLen) {
        const limit = Math.min(bytes.byteLength, maxLen || 256);
        const parts = new Array(limit);
        for (let i = 0; i < limit; i++) {
            parts[i] = ("0" + bytes[i].toString(16)).slice(-2);
        }
        return parts.join("");
    }

    /** (bytes: Uint8Array) -> binary content descriptor via native Uint8Array#toBase64. */
    function describeBinaryUint8ArrayNative(bytes) {
        return {
            kind: "binary",
            byteLength: bytes.byteLength,
            hexPreview: hexPreviewOfUint8Array(bytes, 256),
            base64: bytes.toBase64()
        };
    }

    /** (bytes: Uint8Array) -> binary content descriptor via TextDecoder("latin1") + btoa. */
    function describeBinaryUint8ArrayDecoder(bytes) {
        return {
            kind: "binary",
            byteLength: bytes.byteLength,
            hexPreview: hexPreviewOfUint8Array(bytes, 256),
            base64: btoa(new TextDecoder("latin1").decode(bytes))
        };
    }

    const describeBinaryUint8Array =
        typeof Uint8Array.prototype.toBase64 === "function"
            ? describeBinaryUint8ArrayNative
            : describeBinaryUint8ArrayDecoder;

    function summarizeContent(node) {
        const content = node.content;
        if (content == null) return null;
        if (content instanceof Uint8Array) return describeBinaryUint8Array(content);
        if (Array.isArray(content)) return content.length + " child node(s)";
        return String(content);
    }

    function pushEvent(event) {
        state.events.push(event);
        state.history.push(event);
    }

    function copyAttrs(attrs) {
        const out = {};
        if (!attrs || typeof attrs !== "object") return out;
        Object.keys(attrs).forEach(function (key) {
            out[key] = attrs[key];
        });
        return out;
    }

    function toPlainNode(node, depth) {
        const level = typeof depth === "number" ? depth : 0;
        if (node == null || typeof node !== "object") return null;
        if (level > 10) {
            return { truncated: true };
        }

        const attrs = copyAttrs(node.attrs);
        let content = null;
        if (node.content instanceof Uint8Array) {
            content = describeBinaryUint8Array(node.content);
        } else if (Array.isArray(node.content)) {
            content = node.content.map(function (item) {
                if (item && typeof item === "object" && typeof item.tag === "string") {
                    return toPlainNode(item, level + 1);
                }
                if (item instanceof Uint8Array) return describeBinaryUint8Array(item);
                if (item == null) return null;
                return String(item);
            });
        } else if (node.content != null) {
            content = String(node.content);
        }

        return {
            tag: typeof node.tag === "string" ? node.tag : "",
            attrs: attrs,
            content: content
        };
    }

    function recordStanza(direction, stanza) {
        const attrs = copyAttrs(stanza.attrs);
        const xml = typeof stanza.toString === "function" ? stanza.toString() : null;
        const event = {
            index: state.events.length,
            direction: direction,
            ts: ts(),
            tag: stanza.tag || "",
            id: attrs.id != null ? String(attrs.id) : null,
            from: attrs.from != null ? String(attrs.from) : null,
            to: attrs.to != null ? String(attrs.to) : null,
            attrs: attrs,
            summary: summarizeContent(stanza),
            xml: xml,
            node: toPlainNode(stanza, 0)
        };
        pushEvent(event);
        return event;
    }

    function matchesQuery(event, query) {
        const q = query || {};
        if (q.direction && q.direction !== "any" && event.direction !== q.direction) return false;
        if (q.tag && event.tag !== q.tag) return false;
        if (q.id && event.id !== q.id) return false;
        if (q.from && event.from !== q.from) return false;
        if (q.to && event.to !== q.to) return false;
        if (q.query) {
            const haystack = [event.tag, event.id, event.from, event.to, event.xml].join(" ").toLowerCase();
            if (haystack.indexOf(String(q.query).toLowerCase()) === -1) return false;
        }
        if (q.attrs && typeof q.attrs === "object") {
            for (const key in q.attrs) {
                const expected = q.attrs[key];
                const actual = event.attrs[key];
                if (String(actual) !== String(expected)) return false;
            }
        }
        return true;
    }

    function getEvents(query) {
        const q = query || {};
        const limit = q && typeof q.limit === "number" ? Math.max(1, q.limit) : 100;
        const includeHistory = q.history !== false;
        const source = includeHistory ? state.history : state.events;
        const filtered = source.filter(function (event) {
            return matchesQuery(event, query);
        });
        if (filtered.length <= limit) return filtered;
        return filtered.slice(filtered.length - limit);
    }

    function clearEvents() {
        const removed = state.events.length;
        state.events = [];
        return { cleared: removed, historyCount: state.history.length };
    }

    function clearHistory() {
        const removed = state.history.length;
        state.history = [];
        return { cleared: removed };
    }

    function decodeBase64ToBytes(base64) {
        const raw = atob(base64);
        const out = new Uint8Array(raw.length);
        for (let i = 0; i < raw.length; i += 1) {
            out[i] = raw.charCodeAt(i);
        }
        return out;
    }

    function buildContentItem(item) {
        if (item == null) return null;
        if (typeof item === "string" || typeof item === "number" || typeof item === "boolean") {
            return String(item);
        }
        if (item instanceof Uint8Array) {
            return item;
        }
        if (Array.isArray(item)) {
            return item.map(buildContentItem);
        }
        if (typeof item === "object") {
            if (typeof item.tag === "string") {
                return buildNode(item);
            }
            if (item.type === "binary" && typeof item.base64 === "string") {
                return decodeBase64ToBytes(item.base64);
            }
            if (typeof item.text === "string") {
                return item.text;
            }
        }
        return String(item);
    }

    function buildChildren(content) {
        if (content == null) return [];
        if (!Array.isArray(content)) {
            return [buildContentItem(content)].filter(function (x) { return x != null; });
        }
        return content.map(buildContentItem).filter(function (x) { return x != null; });
    }

    function buildNode(spec) {
        if (!spec || typeof spec !== "object") {
            throw new Error("Node spec must be an object");
        }
        if (typeof spec.tag !== "string" || spec.tag.length === 0) {
            throw new Error("Node spec requires a non-empty tag");
        }
        const attrs = spec.attrs && typeof spec.attrs === "object" ? spec.attrs : null;
        const children = buildChildren(spec.content);
        const args = [spec.tag, attrs].concat(children);
        return WAWap.wap.apply(null, args);
    }

    function sendNode(spec) {
        try {
            const node = buildNode(spec);
            DeprecatedSendIq.deprecatedCastStanza(node);
            return {
                success: true,
                tag: node.tag || null,
                id: node.attrs && node.attrs.id != null ? String(node.attrs.id) : null
            };
        } catch (error) {
            return {
                success: false,
                error: error instanceof Error ? error.message : String(error)
            };
        }
    }

    const origEncode = WAWap.encodeStanza;
    WAWap.encodeStanza = function (stanza) {
        if (!state.paused && stanza && stanza.tag && matchesFilter(stanza.tag)) {
            state.outCount += 1;
            recordStanza("out", stanza);
        }
        return origEncode.apply(this, arguments);
    };

    const origDecode = WAWap.decodeStanza;
    WAWap.decodeStanza = function () {
        const result = origDecode.apply(this, arguments);
        return result.then(function (stanza) {
            if (!state.paused && stanza && stanza.tag && matchesFilter(stanza.tag)) {
                state.inCount += 1;
                recordStanza("in", stanza);
            }
            return stanza;
        });
    };

    window.stanzaLogger = {
        __webMcpInstalled: true,
        get filter() { return state.filter; },
        set filter(v) { state.filter = v; },
        pause: function () {
            state.paused = true;
            return { paused: true };
        },
        resume: function () {
            state.paused = false;
            return { paused: false };
        },
        stats: function () {
            return {
                outCount: state.outCount,
                inCount: state.inCount,
                buffered: state.events.length,
                historyCount: state.history.length,
                paused: state.paused
            };
        },
        getEvents: getEvents,
        query: getEvents,
        clearEvents: clearEvents,
        clearHistory: clearHistory,
        sendNode: sendNode,
        destroy: function () {
            WAWap.encodeStanza = origEncode;
            WAWap.decodeStanza = origDecode;
            delete window.stanzaLogger;
            return { removed: true };
        }
    };
})();

`;

export const WAM_LOGGER_SCRIPT = String.raw`
// noinspection JSUnusedGlobalSymbols

(function () {
    "use strict";

    if (window.wamLogger && window.wamLogger.__webMcpInstalled) {
        return;
    }

    const WamEventModule = self.require("WAWebWamCodegenWamEvent");
    if (!WamEventModule || typeof WamEventModule.WamEvent !== "function") {
        console.error("[WamLogger] Could not find WAWebWamCodegenWamEvent module");
        return;
    }

    const FormatterModule = self.require("WAWebWamEnumFormatter");
    const CodegenUtils = self.require("WAWebWamCodegenUtils");

    const WamEvent = WamEventModule.WamEvent;
    const formatEvent = FormatterModule
        ? FormatterModule.formatWamEventForLogging
        : null;

    const state = {
        paused: false,
        filter: null,
        count: 0,
        events: [],
        history: []
    };

    function matchesFilter(name) {
        if (state.filter == null) return true;
        if (typeof state.filter === "string") return name.indexOf(state.filter) !== -1;
        if (state.filter instanceof RegExp) return state.filter.test(name);
        return true;
    }

    function ts() {
        return new Date().toISOString();
    }

    function formatProperties(event) {
        const props = formatEvent ? formatEvent(event.all, event) : event.all;
        const clean = {};
        if (!props || typeof props !== "object") {
            return clean;
        }
        for (const key in props) {
            if (props[key] != null) {
                clean[key] = props[key];
            }
        }
        return clean;
    }

    function pushEvent(record) {
        state.events.push(record);
        state.history.push(record);
    }

    function recordEvent(event) {
        const name = event.$className || "Unknown";
        const channel = event.wamChannel || "regular";
        const props = formatProperties(event);
        const record = {
            index: state.events.length,
            ts: ts(),
            name: name,
            id: typeof event.id === "number" ? event.id : null,
            weight: typeof event.weight === "number" ? event.weight : null,
            channel: channel,
            props: props
        };
        pushEvent(record);
        return record;
    }

    function matchesQuery(event, query) {
        const q = query || {};
        if (q.channel && q.channel !== event.channel) return false;
        if (q.name && event.name !== q.name) return false;
        if (q.query) {
            const haystack = (event.name + " " + JSON.stringify(event.props || {})).toLowerCase();
            if (haystack.indexOf(String(q.query).toLowerCase()) === -1) return false;
        }
        return true;
    }

    function getEvents(query) {
        const q = query || {};
        const limit = q && typeof q.limit === "number" ? Math.max(1, q.limit) : 100;
        const includeHistory = q.history !== false;
        const source = includeHistory ? state.history : state.events;
        const filtered = source.filter(function (event) {
            return matchesQuery(event, query);
        });
        if (filtered.length <= limit) return filtered;
        return filtered.slice(filtered.length - limit);
    }

    function clearEvents() {
        const removed = state.events.length;
        state.events = [];
        return { cleared: removed, historyCount: state.history.length };
    }

    function clearHistory() {
        const removed = state.history.length;
        state.history = [];
        return { cleared: removed };
    }

    function getEventRegistry() {
        if (!CodegenUtils || typeof CodegenUtils !== "object") return {};
        if (!CodegenUtils.events || typeof CodegenUtils.events !== "object") return {};
        return CodegenUtils.events;
    }

    function getMetricRegistry() {
        if (!CodegenUtils || typeof CodegenUtils !== "object") return null;
        if (!CodegenUtils.metrics || typeof CodegenUtils.metrics !== "object") return null;
        const map = CodegenUtils.metrics.$1;
        if (!map || typeof map !== "object") return null;
        return map;
    }

    function getEventDefinitions(options) {
        const opts = options || {};
        const filter = typeof opts.filter === "string" ? opts.filter.toLowerCase() : null;
        const limit = typeof opts.limit === "number" ? Math.max(1, opts.limit) : 300;
        const eventRegistry = getEventRegistry();
        const metricRegistry = getMetricRegistry();

        const names = Object.keys(eventRegistry).sort();
        const rows = [];

        names.forEach(function (name) {
            if (filter && name.toLowerCase().indexOf(filter) === -1) return;
            const Ctor = eventRegistry[name];
            const proto = Ctor && Ctor.prototype ? Ctor.prototype : {};
            const validators = proto.validators && typeof proto.validators === "object"
                ? proto.validators
                : {};
            const propNames = Object.keys(validators).sort();
            const props = propNames.map(function (propName) {
                const metricKey = name + "::" + propName;
                const metric = metricRegistry && metricRegistry[metricKey] ? metricRegistry[metricKey] : null;
                return {
                    name: propName,
                    id: metric && typeof metric.id === "number" ? metric.id : null,
                    type: metric && metric.type != null ? String(metric.type) : null
                };
            });

            rows.push({
                name: name,
                id: typeof proto.id === "number" ? proto.id : null,
                weight: typeof proto.weight === "number" ? proto.weight : null,
                channel: proto.wamChannel != null ? String(proto.wamChannel) : null,
                privateStatsIdInt: typeof proto.privateStatsIdInt === "number" ? proto.privateStatsIdInt : null,
                properties: props
            });
        });

        if (rows.length <= limit) return rows;
        return rows.slice(0, limit);
    }

    async function sendCustomEvent(payload) {
        const data = payload || {};
        const name = typeof data.name === "string" ? data.name : "";
        if (!name) {
            return { success: false, error: "Missing event name" };
        }

        const eventRegistry = getEventRegistry();
        const EventCtor = eventRegistry[name];
        if (typeof EventCtor !== "function") {
            const sample = Object.keys(eventRegistry).sort().slice(0, 20);
            return {
                success: false,
                error: "Unknown WAM event constructor: " + name,
                sampleEventNames: sample
            };
        }

        const props = data.props && typeof data.props === "object" ? data.props : {};
        const flush = data.flush === true;

        try {
            const instance = new EventCtor(props);
            for (const key in props) {
                instance[key] = props[key];
            }
            if (flush) {
                if (typeof instance.commitAndWaitForFlush !== "function") {
                    return { success: false, error: "Event does not support commitAndWaitForFlush" };
                }
                await instance.commitAndWaitForFlush(true);
            } else {
                if (typeof instance.commit !== "function") {
                    return { success: false, error: "Event does not support commit" };
                }
                instance.commit();
            }
            return { success: true, name: name, flush: flush };
        } catch (error) {
            return {
                success: false,
                error: error instanceof Error ? error.message : String(error)
            };
        }
    }

    function logEvent(event) {
        if (state.paused) return;

        const name = event.$className || "Unknown";
        if (!matchesFilter(name)) return;

        state.count += 1;
        recordEvent(event);
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
        __webMcpInstalled: true,
        get filter() { return state.filter; },
        set filter(v) { state.filter = v; },
        pause: function () {
            state.paused = true;
            return { paused: true };
        },
        resume: function () {
            state.paused = false;
            return { paused: false };
        },
        stats: function () {
            return {
                count: state.count,
                buffered: state.events.length,
                historyCount: state.history.length,
                paused: state.paused
            };
        },
        getEvents: getEvents,
        query: getEvents,
        clearEvents: clearEvents,
        clearHistory: clearHistory,
        getEventDefinitions: getEventDefinitions,
        queryDefinitions: getEventDefinitions,
        sendCustomEvent: sendCustomEvent,
        destroy: function () {
            WamEvent.prototype.commit = origCommit;
            WamEvent.prototype.commitAndWaitForFlush = origCommitFlush;
            delete window.wamLogger;
            return { removed: true };
        }
    };
})();

`;

const AUTO_INJECT_SCRIPTS = [
  AB_PROPS_SCRIPT,
  STANZA_LOGGER_SCRIPT,
  WAM_LOGGER_SCRIPT,
];

export function loadAutoInjectScripts(): string[] {
  return [...AUTO_INJECT_SCRIPTS];
}
