#!/usr/bin/env node
// Reverse-dependency closure + leaves-first topological sort for the /validate
// incremental mode (Phase 1-INC, steps 6 and 8). Pure read/compute: it reads a
// JSON spec, writes a JSON result, spawns no agents and touches no source. This
// is an allowed read-only driver helper, not an agent replacement.
//
// Input (argv[2] = file path, or stdin):
//   {
//     "graph": { "<module>": ["<dep>", ...], ... },  // forward edges module -> dep
//     "seed":  ["<module>", ...]                       // directly-invalidated modules
//   }
//
// Output (stdout, JSON):
//   {
//     "closureCount":   N,
//     "closure":        [...],   // seed plus every transitive consumer
//     "topoOrder":      [...],   // closure, leaves first (deps before consumers)
//     "cyclesDetected": bool     // true if the closure contains a dependency cycle
//   }

import { readFileSync } from "node:fs";

function readSpec() {
  const path = process.argv[2];
  const raw = path ? readFileSync(path, "utf8") : readFileSync(0, "utf8");
  return JSON.parse(raw);
}

function buildReverseEdges(graph) {
  const reverse = new Map();
  for (const module of Object.keys(graph)) {
    if (!reverse.has(module)) reverse.set(module, new Set());
  }
  for (const [module, deps] of Object.entries(graph)) {
    for (const dep of deps) {
      if (!reverse.has(dep)) reverse.set(dep, new Set());
      reverse.get(dep).add(module);
    }
  }
  return reverse;
}

function reverseClosure(seed, reverseEdges) {
  const closure = new Set();
  const stack = [...seed];
  while (stack.length > 0) {
    const module = stack.pop();
    if (closure.has(module)) continue;
    closure.add(module);
    for (const consumer of reverseEdges.get(module) ?? []) {
      if (!closure.has(consumer)) stack.push(consumer);
    }
  }
  return closure;
}

function topoSortLeavesFirst(closure, graph) {
  const outstanding = new Map();
  const dependents = new Map();
  for (const module of closure) {
    const depsInClosure = (graph[module] ?? []).filter((dep) => closure.has(dep));
    outstanding.set(module, new Set(depsInClosure));
    for (const dep of depsInClosure) {
      if (!dependents.has(dep)) dependents.set(dep, new Set());
      dependents.get(dep).add(module);
    }
  }

  const ready = [...closure].filter((module) => outstanding.get(module).size === 0).sort();
  const order = [];
  while (ready.length > 0) {
    const module = ready.shift();
    order.push(module);
    for (const consumer of dependents.get(module) ?? []) {
      const remaining = outstanding.get(consumer);
      remaining.delete(module);
      if (remaining.size === 0) {
        ready.push(consumer);
        ready.sort();
      }
    }
  }

  const cyclesDetected = order.length < closure.size;
  if (cyclesDetected) {
    const placed = new Set(order);
    for (const module of [...closure].sort()) {
      if (!placed.has(module)) order.push(module);
    }
  }
  return { order, cyclesDetected };
}

function main() {
  const spec = readSpec();
  const graph = spec.graph ?? {};
  const seed = spec.seed ?? [];
  const reverseEdges = buildReverseEdges(graph);
  const closure = reverseClosure(seed, reverseEdges);
  const { order, cyclesDetected } = topoSortLeavesFirst(closure, graph);
  process.stdout.write(
    JSON.stringify(
      {
        closureCount: closure.size,
        closure: [...closure].sort(),
        topoOrder: order,
        cyclesDetected,
      },
      null,
      2
    ) + "\n"
  );
}

main();
