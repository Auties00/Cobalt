---
type: "query"
date: "2026-11-12T15:50:28.005181+00:00"
question: "Por que o reenvio por phash divergente nao chegava aos novos dispositivos do destinatario?"
contributor: "graphify"
outcome: "useful"
source_nodes: ["getDeviceLists", "getUserFanout", "handlePhashMismatch", "fetchDeviceListsFromServer"]
---

# Q: Por que o reenvio por phash divergente nao chegava aos novos dispositivos do destinatario?

## Answer

LiveDeviceService.getDeviceLists so consultava o servidor para usuarios sem lista em cache; com phash divergente reutilizava a lista velha, e o servidor reportava phash a cada ack. Corrigido: phash divergente forca usync de todos os userJids (com hashes para resposta delta).

## Outcome

- Signal: useful

## Source Nodes

- getDeviceLists
- getUserFanout
- handlePhashMismatch
- fetchDeviceListsFromServer