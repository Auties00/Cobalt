---
type: "query"
date: "2026-11-12T15:50:28.236823+00:00"
question: "O que deixa o login (bootstrap pos-success) lento?"
contributor: "graphify"
outcome: "useful"
source_nodes: ["bootstrap", "SuccessStreamHandler", "syncPrivacyDisallowedListsMex", "runComplianceProbe"]
---

# Q: O que deixa o login (bootstrap pos-success) lento?

## Answer

SuccessStreamHandler.bootstrap executa ~10 round-trips em serie antes do LoggedIn: abProps.sync (3 tentativas com sleep aleatorio ate 10s), media_conn, presenca, 2 probes de compliance, privacy settings e 4 MEX de disallowed lists. Corrigido: probes em background; privacy + 4 MEX em paralelo com join antes do LoggedIn.

## Outcome

- Signal: useful

## Source Nodes

- bootstrap
- SuccessStreamHandler
- syncPrivacyDisallowedListsMex
- runComplianceProbe