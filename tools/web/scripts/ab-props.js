// noinspection JSUnusedGlobalSymbols

/**
 * WhatsApp Web AB Props Inspector
 *
 * Lists, gets, and sets A/B testing feature flags at runtime
 * by reading from WAWebABPropsConfigs (definitions) and
 * WAWebABPropsCache (live values).
 *
 * Usage: Paste into the browser console while WhatsApp Web is open.
 *
 * Configuration:
 *   window.abProps.list()                   - List all props with their current values
 *   window.abProps.list("ai")              - List props whose name contains the string
 *   window.abProps.list(/^web_/)           - List props whose name matches the regex
 *   window.abProps.get("prop_name")        - Get the current value of a single prop
 *   window.abProps.set("prop_name", value) - Override a prop's value in the live cache
 *   window.abProps.reset("prop_name")      - Reset a prop to its default value
 *   window.abProps.resetAll()              - Reset all props to their default values
 *   window.abProps.diff()                  - List only props whose value differs from default
 *   window.abProps.diff("ai")             - Filtered diff (same filter syntax as list)
 */
(function () {
    "use strict";

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

    const TYPE_COLORS = {
        bool: "#74c0fc",
        int: "#b197fc",
        float: "#f783ac",
        string: "#ffd43b"
    };

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

    function getCacheEntry(code) {
        return ABPropsCache.getAllABPropsMap().get(code);
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

        if (names.length === 0) {
            console.log("[ABProps] No props matching filter");
            return;
        }

        const rows = names.map(function (name) {
            const def = getDefinition(name);
            return {
                name: name,
                code: def.code,
                type: def.type,
                value: formatValue(getCurrentValue(name)),
                default: formatValue(def.defaultValue)
            };
        });

        console.groupCollapsed(
            "%c[ABProps]%c %d prop(s)" + (filter ? " matching " + filter : ""),
            "color:#74c0fc;font-weight:bold",
            "color:inherit",
            names.length
        );
        console.table(rows);
        console.groupEnd();

        return rows;
    }

    function get(name) {
        const def = getDefinition(name);
        if (def == null) {
            console.error("[ABProps] Unknown prop: " + name);
            return undefined;
        }

        const value = getCurrentValue(name);
        const entry = getCacheEntry(def.code);
        const typeColor = TYPE_COLORS[def.type] || "#868e96";

        console.log(
            "%c[ABProps]%c %s %c(%s)%c = %c%s%c  (default: %s)",
            "color:#74c0fc;font-weight:bold",
            "color:inherit;font-weight:bold",
            name,
            "color:" + typeColor + ";font-weight:normal",
            def.type,
            "color:inherit",
            "color:#51cf66;font-weight:bold",
            formatValue(value),
            "color:inherit;font-weight:normal",
            formatValue(def.defaultValue)
        );

        if (entry && entry.configExpoKey != null) {
            console.log("  Expo key:", entry.configExpoKey);
        }

        return value;
    }

    function set(name, value) {
        const def = getDefinition(name);
        if (def == null) {
            console.error("[ABProps] Unknown prop: " + name);
            return;
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

        console.log(
            "%c[ABProps]%c %s set to %c%s",
            "color:#74c0fc;font-weight:bold",
            "color:inherit;font-weight:bold",
            name,
            "color:#ffd43b;font-weight:bold",
            formatValue(coerced)
        );

        return coerced;
    }

    function reset(name) {
        const def = getDefinition(name);
        if (def == null) {
            console.error("[ABProps] Unknown prop: " + name);
            return;
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
                count++;
            }
        });

        console.log(
            "%c[ABProps]%c Reset %d prop(s) to defaults",
            "color:#74c0fc;font-weight:bold",
            "color:inherit",
            count
        );
    }

    function diff(filter) {
        const names = Object.keys(configs).filter(function (n) {
            return matchesFilter(n, filter);
        }).sort();

        const rows = [];
        names.forEach(function (name) {
            const def = getDefinition(name);
            const current = getCurrentValue(name);
            if (current !== def.defaultValue) {
                rows.push({
                    name:     name,
                    code:     def.code,
                    type:     def.type,
                    value:    formatValue(current),
                    default:  formatValue(def.defaultValue)
                });
            }
        });

        if (rows.length === 0) {
            console.log("[ABProps] All props match their defaults" + (filter ? " (filtered)" : ""));
            return rows;
        }

        console.groupCollapsed(
            "%c[ABProps]%c %d prop(s) differ from default" + (filter ? " matching " + filter : ""),
            "color:#74c0fc;font-weight:bold",
            "color:inherit",
            rows.length
        );
        console.table(rows);
        console.groupEnd();

        return rows;
    }

    window.abProps = {
        list:     list,
        get:      get,
        set:      set,
        reset:    reset,
        resetAll: resetAll,
        diff:     diff
    };

    console.log(
        "%c[ABProps]%c Installed \u2014 %d props available. Use window.abProps for controls.",
        "color:#74c0fc;font-weight:bold",
        "color:inherit",
        Object.keys(configs).length
    );
})();
