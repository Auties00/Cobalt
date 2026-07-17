import { withForceLoadedBundle } from "./runtime.js";
import type { MexOperation, MexPageResult, Transport } from "../parser/types.js";

/**
 * Runs inside the WhatsApp Web page after the bundle is force-loaded.
 *
 * Walks every defined module whose name ends in ".graphql", requires it, and keeps only the
 * compiled Relay ConcreteRequest artifacts (kind === "Request"); Fragment-only modules and any
 * module that fails to require are skipped. For each operation it captures the persisted-query id,
 * operation name and kind, the request variables, a flattened response selection tree, and the
 * dispatch transport resolved from the module dependency graph.
 *
 * Transport resolution inspects, for each module that imports the artifact (directly or through a
 * same-named thin re-export wrapper), whether that consumer directly imports a MEX client, the
 * imperative Relay client, or Comet; it also reads the artifact's own dependencies, treating a
 * dependency on {@code relay-runtime} as a preloadable Comet query. The artifact is tagged with the
 * set of transports observed. Direct imports are used rather than transitive reachability because a
 * large consumer transitively pulls in unrelated clients, which would mislabel HTTP queries as MEX.
 *
 * Self-contained by necessity: Playwright serializes this function to the page, so it cannot close
 * over imports or module-scope bindings. The sentinel lists, the Relay-AST flattener, and the
 * resolver are all defined locally.
 */
function interceptMexInPage(): MexPageResult {
  const mexSentinels = [
    "WAWebMexClient",
    "WAWebMexNativeClient",
    "WAWebMexRelayEnvironment",
  ];
  const httpRelaySentinels = ["WAWebRelayClient", "WAWebRelayEnvironment"];
  const cometSentinels = ["CometRelay"];

  const names: string[] = Array.from(
    new Set((window as any).__waDefinedModules || []),
  ) as string[];
  const req = (window as any).require as (name: string) => any;
  const deps: Record<string, string[]> = (window as any).__waModuleDeps || {};

  const consumers = new Map<string, string[]>();
  for (const mod of Object.keys(deps)) {
    for (const dep of deps[mod]) {
      const list = consumers.get(dep);
      if (list) list.push(mod);
      else consumers.set(dep, [mod]);
    }
  }

  function resolveTransports(moduleName: string): Transport[] {
    const found = new Set<Transport>();

    // Consumers that import the artifact directly, plus consumers of a same-named thin re-export
    // wrapper (the artifact's direct consumer is often a wrapper that only re-exports it, with the
    // real dispatcher one hop up importing the wrapper).
    const candidates = [...(consumers.get(moduleName) || [])];
    const wrapper = moduleName.replace(/\.graphql$/, "");
    if (wrapper !== moduleName && (deps[wrapper] || []).includes(moduleName)) {
      for (const consumer of consumers.get(wrapper) || []) candidates.push(consumer);
    }

    for (const consumer of candidates) {
      const consumerDeps = deps[consumer] || [];
      if (consumerDeps.some((d) => mexSentinels.includes(d))) found.add("stanza_mex");
      if (consumerDeps.some((d) => httpRelaySentinels.includes(d))) found.add("http_relay");
      if (consumerDeps.some((d) => cometSentinels.includes(d))) found.add("http_comet");
    }

    // Preloadable Comet queries self-register with relay-runtime's PreloadableQueryRegistry; the
    // artifact's own dependency on relay-runtime is the clean discriminator (imperative Relay
    // queries reference only a _facebookRelayOperation module, never relay-runtime).
    if ((deps[moduleName] || []).includes("relay-runtime")) found.add("http_comet");

    return found.size > 0 ? [...found] : ["unknown"];
  }

  function normalizeSelections(selections: any[]): any[] {
    const out: any[] = [];
    for (const sel of selections || []) {
      switch (sel.kind) {
        case "ScalarField": {
          const node: any = { kind: "scalar", name: sel.name };
          if (sel.alias && sel.alias !== sel.name) node.alias = sel.alias;
          out.push(node);
          break;
        }
        case "LinkedField": {
          const node: any = {
            kind: "linked",
            name: sel.name,
            type: sel.concreteType ?? null,
            plural: !!sel.plural,
            selections: normalizeSelections(sel.selections),
          };
          if (sel.alias && sel.alias !== sel.name) node.alias = sel.alias;
          out.push(node);
          break;
        }
        case "InlineFragment": {
          out.push({
            kind: "inlineFragment",
            onType: sel.type ?? null,
            selections: normalizeSelections(sel.selections),
          });
          break;
        }
        case "Condition": {
          out.push({
            kind: "condition",
            variable: sel.condition,
            passingValue: !!sel.passingValue,
            selections: normalizeSelections(sel.selections),
          });
          break;
        }
        case "ClientExtension": {
          for (const child of normalizeSelections(sel.selections)) out.push(child);
          break;
        }
        default: {
          const node: any = { kind: sel.kind };
          if (sel.name) node.name = sel.name;
          if (Array.isArray(sel.selections)) {
            node.selections = normalizeSelections(sel.selections);
          }
          out.push(node);
        }
      }
    }
    return out;
  }

  const operations: MexOperation[] = [];
  for (const name of names) {
    if (!/\.graphql$/.test(name)) continue;
    let mod: any;
    try {
      mod = req(name);
    } catch {
      continue;
    }
    const cr = mod && mod.default ? mod.default : mod;
    if (!cr || cr.kind !== "Request" || !cr.params || !cr.operation) continue;
    const argDefs: any[] = cr.operation.argumentDefinitions || [];
    operations.push({
      module: name,
      id: cr.params.id ?? null,
      name: cr.params.name ?? null,
      operationKind: cr.params.operationKind ?? null,
      transports: resolveTransports(name),
      variables: argDefs.map((a) => ({
        name: a.name,
        defaultValue: a.defaultValue ?? null,
      })),
      response: normalizeSelections(cr.operation.selections),
    });
  }

  operations.sort((a, b) => String(a.name).localeCompare(String(b.name)));
  return {
    operations,
    relaySources: ((window as any).__waModuleSource || {}) as Record<string, string>,
  };
}

/**
 * Force-loads the WhatsApp Web bundle and returns every compiled Relay operation it defines.
 *
 * Returns all ".graphql" ConcreteRequest operations (queries and mutations) sorted by name, each
 * tagged with its dispatch transport, alongside the source of every relay consumer.
 */
export async function extractMexSchemas(): Promise<MexPageResult> {
  return await withForceLoadedBundle(interceptMexInPage);
}
