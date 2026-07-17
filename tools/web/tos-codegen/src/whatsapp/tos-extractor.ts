import { withForceLoadedBundle } from "./runtime.js";
import type { TosNoticeDef } from "../parser/types.js";

interface RawNotice {
  name: string;
  defaultId: string | null;
  webProp: string | null;
  smbProp: string | null;
  multiValued: boolean;
  source: string;
}

interface RawResult {
  notices: RawNotice[];
  /** All notice-id-shaped AB-prop names found in WAWebABPropsConfigs (for reconciliation). */
  candidateProps: string[];
}

/**
 * Runs inside the WhatsApp Web page. Parses the captured TOS-module factory
 * sources (window.__waModuleSources) for notice-id producing exports without
 * calling the getters, which can have side effects.
 *
 * Discovery is driven three ways for robustness: (1) the TosManager registry in
 * WAWebTos, whose getter references (o("WAWebX").getY()) are resolved regardless
 * of the getter's name; (2) a vocabulary name filter (Tos/TOS/Notice/Nux) as a
 * fallback; (3) a reconciliation list of every notice-id-shaped AB-prop in
 * WAWebABPropsConfigs so callers can flag any prop that is not emitted.
 */
function interceptInPage(): RawResult {
  const sources: Record<string, string> = (window as any).__waModuleSources || {};
  const req = (window as any).require as (name: string) => any;

  const escapeRe = (s: string): string => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

  const isNoticeExportName = (name: string): boolean => {
    // Case-sensitive tokens: "Tos"/"TOS" (terms-of-service), "Notice", "Nux" - avoids matching
    // incidental substrings like "ToSend". Nux notices share the same TosManager acceptance machinery.
    if (!/Tos|TOS|Notice|Nux/.test(name)) return false;
    if (/^(is|should|has|can|accept|reset|debug|refresh|sync|set|use|init|clear|maybe|on)/.test(name)) return false;
    if (/(Enabled|Emitter|DebugInfo|State|Accepted|Available|Required|Job|Config|Supported|Date|Url|Count|Limit)s?$/i.test(name)) return false;
    return true;
  };

  // A purely AB-prop-driven id is only credible when the prop name is notice-id shaped.
  const isNoticeIdProp = (prop: string): boolean => /(notice_id|notice_ids|tos_id|_notices|_nux_id)$/.test(prop);

  const buildIdVarMap = (src: string): Record<string, string> => {
    const map: Record<string, string> = {};
    const re = /([A-Za-z_$][\w$]*)\s*=\s*"(\d{8})"/g;
    let m: RegExpExecArray | null;
    while ((m = re.exec(src)) !== null) map[m[1]] = m[2];
    return map;
  };

  const fnBody = (src: string, sym: string): string | null => {
    const sig = new RegExp("function\\s+" + escapeRe(sym) + "\\s*\\(");
    const sm = sig.exec(src);
    if (!sm) return null;
    const open = src.indexOf("{", sm.index);
    if (open < 0) return null;
    let depth = 0;
    for (let j = open; j < src.length; j++) {
      const ch = src[j];
      if (ch === "{") depth++;
      else if (ch === "}") {
        depth--;
        if (depth === 0) return src.slice(open, j + 1);
      }
    }
    return null;
  };

  // Detects a module's exports object variable as the most-assigned simple object,
  // so a minifier rename of the conventional `l` exports param does not blind the module.
  const findExportsVar = (src: string): string | null => {
    const deny = new Set([
      "this", "window", "document", "babelHelpers", "globalThis",
      "navigator", "console", "Object", "Array", "Math", "JSON",
    ]);
    // Count only `<var>.<name>=<identifier>` (the export block), not `<proto>.<m>=function...`
    // prototype assignments, so a class-heavy module (e.g. WAWebTos's TosManager) is not mistaken.
    const counts: Record<string, number> = {};
    const re = /\b([A-Za-z_$][\w$]*)\.[A-Za-z_$][\w$]*\s*=\s*[A-Za-z_$][\w$]*\s*[,;}]/g;
    let m: RegExpExecArray | null;
    while ((m = re.exec(src)) !== null) {
      if (!deny.has(m[1])) counts[m[1]] = (counts[m[1]] || 0) + 1;
    }
    let best: string | null = null;
    let bestCount = 0;
    for (const v of Object.keys(counts)) {
      if (counts[v] > bestCount) {
        best = v;
        bestCount = counts[v];
      }
    }
    return bestCount >= 1 ? best : null;
  };

  const deriveName = (name: string): string => {
    if (/^TOS_\d+_ID$/.test(name)) return name.replace(/_ID$/, "");
    let s = name.replace(/^get/, "");
    // Strip the trailing TOS/notice-id token; keep "Nux" so nux notices stay distinct from their Tos siblings.
    s = s.replace(/(TosNoticeId|TosId|NoticeIds|NoticeId|Tos)$/, "");
    s = s.replace(/_TOS_ID$/, "");
    s = s.replace(/([a-z0-9])([A-Z])/g, "$1_$2").replace(/([A-Z]+)([A-Z][a-z])/g, "$1_$2");
    return s.toUpperCase().replace(/__+/g, "_").replace(/^_|_$/g, "");
  };

  const resolve = (
    src: string,
    idVars: Record<string, string>,
    localSym: string,
  ): { defaultId: string | null; webProp: string | null; smbProp: string | null; multiValued: boolean } | null => {
    if (idVars[localSym] !== undefined) {
      return { defaultId: idVars[localSym], webProp: null, smbProp: null, multiValued: false };
    }
    const body = fnBody(src, localSym);
    if (body === null) return null;

    const props = Array.from(body.matchAll(/getABPropConfigValue\("([^"]+)"\)/g)).map((m) => m[1]);
    const multiValued = body.indexOf('.split(",")') !== -1;

    let webProp: string | null = null;
    let smbProp: string | null = null;
    if (props.length >= 2 && body.indexOf("isSMB()") !== -1) {
      const m = body.match(
        /isSMB\(\)\s*\?[\s\S]*?getABPropConfigValue\("([^"]+)"\)[\s\S]*?:[\s\S]*?getABPropConfigValue\("([^"]+)"\)/,
      );
      if (m) {
        smbProp = m[1];
        webProp = m[2];
      } else {
        webProp = props[0];
      }
    } else if (props.length >= 1) {
      webProp = props[0];
    }

    let defaultId: string | null = null;
    const lit = body.match(/"(\d{8})"/);
    if (lit) {
      defaultId = lit[1];
    } else {
      const localVars = new Set<string>();
      const lv = /(?:\bvar\s+|[,(]\s*)([A-Za-z_$][\w$]*)\s*=/g;
      let lm: RegExpExecArray | null;
      while ((lm = lv.exec(body)) !== null) localVars.add(lm[1]);
      for (const v of Object.keys(idVars)) {
        if (localVars.has(v)) continue;
        if (new RegExp("[^\\w$]" + escapeRe(v) + "[^\\w$]").test(body)) {
          defaultId = idVars[v];
          break;
        }
      }
    }

    if (defaultId === null && webProp === null) return null;
    return { defaultId, webProp, smbProp, multiValued };
  };

  // (1) Registry seed: notice getters TosManager calls, keyed "Module.export", resolved even when
  // the getter name would not pass the vocabulary filter.
  const registryExports = new Set<string>();
  const collectRefs = (src: string): void => {
    for (const m of src.matchAll(/o\("(WAWeb\w+)"\)\.([A-Za-z_$][\w$]*)\(/g)) registryExports.add(m[1] + "." + m[2]);
  };
  if (sources["WAWebTos"]) collectRefs(sources["WAWebTos"]);

  const out: RawNotice[] = [];
  const byKey = new Map<string, RawNotice>();

  const consider = (moduleName: string, src: string, idVars: Record<string, string>, exportName: string, localSym: string): void => {
    const resolved = resolve(src, idVars, localSym);
    if (resolved === null) return;
    if (resolved.defaultId === null && (resolved.webProp === null || !isNoticeIdProp(resolved.webProp))) return;
    const key = resolved.defaultId !== null ? "id:" + resolved.defaultId : "prop:" + resolved.webProp;
    const notice: RawNotice = {
      name: deriveName(exportName),
      defaultId: resolved.defaultId,
      webProp: resolved.webProp,
      smbProp: resolved.smbProp,
      multiValued: resolved.multiValued,
      source: moduleName + "." + exportName,
    };
    const existing = byKey.get(key);
    if (existing) {
      if (existing.webProp === null && notice.webProp !== null) {
        const idx = out.indexOf(existing);
        if (idx >= 0) out[idx] = notice;
        byKey.set(key, notice);
      }
      return;
    }
    byKey.set(key, notice);
    out.push(notice);
  };

  for (const moduleName of Object.keys(sources)) {
    const src = sources[moduleName];
    const idVars = buildIdVarMap(src);
    const exportsVar = findExportsVar(src);
    if (exportsVar === null) continue;
    const exportRe = new RegExp("\\b" + escapeRe(exportsVar) + "\\.([A-Za-z_$][\\w$]*)\\s*=\\s*([A-Za-z_$][\\w$]*)", "g");
    let em: RegExpExecArray | null;
    while ((em = exportRe.exec(src)) !== null) {
      const exportName = em[1];
      const localSym = em[2];
      if (!isNoticeExportName(exportName) && !registryExports.has(moduleName + "." + exportName)) continue;
      consider(moduleName, src, idVars, exportName, localSym);
    }
  }

  // (2) Reconciliation: every notice-id-shaped AB-prop, so callers can flag any not emitted.
  const candidateProps: string[] = [];
  try {
    const cfg = req("WAWebABPropsConfigs").ABPropConfigs;
    if (cfg && typeof cfg === "object") {
      for (const name of Object.keys(cfg)) {
        if (isNoticeIdProp(name)) candidateProps.push(name);
      }
    }
  } catch (e) {}

  return { notices: out, candidateProps };
}

export interface ExtractResult {
  notices: TosNoticeDef[];
  /** Notice-id-shaped AB-props that no extracted notice uses (candidates to investigate). */
  unmatchedProps: string[];
}

export async function extractTosNotices(): Promise<ExtractResult> {
  const raw = await withForceLoadedBundle(interceptInPage);

  // Ensure constant names are unique; disambiguate any collision with a suffix.
  const seen = new Set<string>();
  for (const n of raw.notices) {
    let name = n.name;
    let i = 2;
    while (seen.has(name)) name = `${n.name}_${i++}`;
    seen.add(name);
    n.name = name;
  }

  const emitted = new Set<string>();
  for (const n of raw.notices) {
    if (n.webProp) emitted.add(n.webProp);
    if (n.smbProp) emitted.add(n.smbProp);
  }
  const unmatchedProps = raw.candidateProps.filter((p) => !emitted.has(p)).sort();

  return {
    notices: raw.notices.map((n) => ({
      name: n.name,
      defaultId: n.defaultId,
      webProp: n.webProp,
      smbProp: n.smbProp,
      multiValued: n.multiValued,
      source: n.source,
    })),
    unmatchedProps,
  };
}
