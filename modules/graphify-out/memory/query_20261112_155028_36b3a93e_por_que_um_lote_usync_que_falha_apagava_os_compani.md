---
type: "query"
date: "2026-11-12T15:50:28.128874+00:00"
question: "Por que um lote usync que falha apagava os companions conhecidos?"
contributor: "graphify"
outcome: "useful"
source_nodes: ["fetchDeviceListsFromServer", "getDevicesFetchedResults", "createPrimaryOnlyDeviceList"]
---

# Q: Por que um lote usync que falha apagava os companions conhecidos?

## Answer

fetchDeviceListsFromServer criava lista so-primario para usuarios sem entrada na resposta; getDevicesFetchedResults engole falhas de lote retornando lista vazia, entao uma falha de rede sobrescrevia o cache. Corrigido: mantem a lista em cache (nao deletada) quando presente.

## Outcome

- Signal: useful

## Source Nodes

- fetchDeviceListsFromServer
- getDevicesFetchedResults
- createPrimaryOnlyDeviceList