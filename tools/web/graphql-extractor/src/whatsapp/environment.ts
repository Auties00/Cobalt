import * as acorn from "acorn";
import * as walk from "acorn-walk";
import type { ClassifiedOperation, Environment, MexOperation } from "../parser/types.js";

const DISPATCH_METHODS = new Set(["fetchQuery", "commitMutation"]);
const DOC_CONFIG_KEYS = new Set(["mutation", "query", "subscription"]);
const DEFAULT_RELAY_ENVIRONMENT: Environment = "whatsapp_catalog";

export function assignEnvironments(
  operations: MexOperation[],
  relaySources: Record<string, string>,
): { operations: ClassifiedOperation[]; unresolvedRelay: string[] } {
  const artifacts = new Set(operations.map((op) => op.module));
  const artifactEnvironments = new Map<string, Set<Environment>>();
  for (const [module, source] of Object.entries(relaySources)) {
    try {
      collectDispatches(source, artifacts, artifactEnvironments);
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.warn(`[WARN] failed to parse relay consumer ${module}: ${message}`);
    }
  }

  const unresolvedRelay: string[] = [];
  const classified = operations.map((op): ClassifiedOperation => {
    const resolved = artifactEnvironments.get(op.module);
    let environments: Environment[];
    if (resolved && resolved.size > 0) {
      environments = [...resolved].sort();
    } else if (op.transports.includes("http_relay")) {
      environments = [DEFAULT_RELAY_ENVIRONMENT];
      unresolvedRelay.push(op.name ?? op.module);
    } else if (op.transports.includes("http_comet")) {
      environments = ["facebook"];
    } else {
      environments = [];
    }
    return { ...op, environments };
  });

  return { operations: classified, unresolvedRelay };
}

function collectDispatches(
  source: string,
  artifacts: Set<string>,
  out: Map<string, Set<Environment>>,
): void {
  const ast = acorn.parse(`(${source})`, { ecmaVersion: "latest" });

  const artifactByBinding = new Map<string, string>();
  const environmentByBinding = new Map<string, Environment>();
  walk.full(ast, (node: any) => {
    const target = bindingTarget(node);
    if (!target) return;
    const artifact = artifactRefIn(target.init, artifacts);
    if (artifact) artifactByBinding.set(target.name, artifact);
    const environment = getEnvironmentTypeIn(target.init);
    if (environment) environmentByBinding.set(target.name, environment);
  });

  walk.full(ast, (node: any) => {
    if (node.type !== "CallExpression" || node.callee?.type !== "MemberExpression") return;
    if (!isMethodNamed(node.callee.property, DISPATCH_METHODS)) return;

    const args: any[] = node.arguments ?? [];
    const artifact = resolveArtifact(args, artifactByBinding, artifacts);
    if (!artifact) return;

    const environment = resolveEnvironment(args, environmentByBinding);
    let set = out.get(artifact);
    if (!set) out.set(artifact, (set = new Set<Environment>()));
    set.add(environment);
  });
}

function bindingTarget(node: any): { name: string; init: any } | null {
  if (node.type === "VariableDeclarator" && node.id?.type === "Identifier" && node.init) {
    return { name: node.id.name, init: node.init };
  }
  if (node.type === "AssignmentExpression" && node.left?.type === "Identifier" && node.right) {
    return { name: node.left.name, init: node.right };
  }
  return null;
}

function resolveArtifact(
  args: any[],
  artifactByBinding: Map<string, string>,
  artifacts: Set<string>,
): string | null {
  for (const arg of args) {
    const found = resolveArtifactNode(arg, artifactByBinding, artifacts);
    if (found) return found;
  }
  return null;
}

function resolveArtifactNode(
  node: any,
  artifactByBinding: Map<string, string>,
  artifacts: Set<string>,
): string | null {
  const ref = artifactRefIn(node, artifacts);
  if (ref) return ref;
  if (node?.type === "Identifier" && artifactByBinding.has(node.name)) {
    return artifactByBinding.get(node.name)!;
  }
  if (node?.type === "ObjectExpression") {
    for (const prop of node.properties ?? []) {
      if (prop.type === "Property" && isKeyNamed(prop.key, DOC_CONFIG_KEYS) && prop.value) {
        const nested = resolveArtifactNode(prop.value, artifactByBinding, artifacts);
        if (nested) return nested;
      }
    }
  }
  return null;
}

function artifactRefIn(node: any, artifacts: Set<string>): string | null {
  let found: string | null = null;
  walk.full(node, (n: any) => {
    if (found) return;
    if (n.type === "Literal" && typeof n.value === "string" && n.value.endsWith(".graphql")) {
      found = n.value;
    } else if (n.type === "CallExpression" && n.arguments?.length === 1) {
      const arg = n.arguments[0];
      if (arg.type === "Literal" && typeof arg.value === "string" && artifacts.has(`${arg.value}.graphql`)) {
        found = `${arg.value}.graphql`;
      }
    }
  });
  return found;
}

function resolveEnvironment(args: any[], environmentByBinding: Map<string, Environment>): Environment {
  for (const arg of args) {
    if (arg?.type === "ObjectExpression") {
      const env = environmentTypeInObject(arg);
      if (env) return env;
    }
  }
  for (const arg of args) {
    if (arg?.type === "Identifier" && environmentByBinding.has(arg.name)) {
      return environmentByBinding.get(arg.name)!;
    }
  }
  return DEFAULT_RELAY_ENVIRONMENT;
}

function getEnvironmentTypeIn(node: any): Environment | null {
  let found: Environment | null = null;
  walk.full(node, (n: any) => {
    if (found) return;
    if (n.type === "CallExpression" && isMethodNamed(n.callee?.property, GET_ENVIRONMENT)) {
      for (const arg of n.arguments ?? []) {
        if (arg?.type === "ObjectExpression") {
          const env = environmentTypeInObject(arg);
          if (env) found = env;
        }
      }
    }
  });
  return found;
}

function environmentTypeInObject(obj: any): Environment | null {
  for (const prop of obj.properties ?? []) {
    if (prop.type !== "Property") continue;
    if (keyName(prop.key) === "environmentType" && prop.value?.type === "Literal" && typeof prop.value.value === "string") {
      return prop.value.value as Environment;
    }
  }
  return null;
}

const GET_ENVIRONMENT = new Set(["getEnvironment"]);

function isMethodNamed(property: any, names: Set<string>): boolean {
  const name = keyName(property);
  return name !== null && names.has(name);
}

function isKeyNamed(key: any, names: Set<string>): boolean {
  const name = keyName(key);
  return name !== null && names.has(name);
}

function keyName(node: any): string | null {
  if (node?.type === "Identifier") return node.name;
  if (node?.type === "Literal" && typeof node.value === "string") return node.value;
  return null;
}
