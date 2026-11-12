---
type: "query"
date: "2026-11-12T15:50:28.371916+00:00"
question: "flushStore() em cada envio e a causa da lentidao do envio?"
contributor: "graphify"
outcome: "dead_end"
source_nodes: ["flushStore", "PersistentStore", "save"]
---

# Q: flushStore() em cada envio e a causa da lentidao do envio?

## Answer

Nao. PersistentStore.save() e debounced: apenas marca pendente e acorda o flusher daemon; a serializacao do store.proto e coalescida fora do caminho de envio.

## Outcome

- Signal: dead_end

## Source Nodes

- flushStore
- PersistentStore
- save