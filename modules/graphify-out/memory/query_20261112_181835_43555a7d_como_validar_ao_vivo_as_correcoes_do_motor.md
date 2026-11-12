---
type: "query"
date: "2026-11-12T18:18:35.259238+00:00"
question: "Como validar ao vivo as correcoes do motor"
contributor: "graphify"
outcome: "useful"
source_nodes: ["MessageEncryption", "IcdcComputer", "LiveDeviceService", "SuccessStreamHandler"]
---

# Q: Como validar ao vivo as correcoes do motor

## Answer

Roteiro em docs/engine-review/VALIDACAO.md. Caminho recomendado: pela msgforge-api (PR #12), roteiro docs/VALIDACAO-COBALT-FORK.md, que instala este fork no Docker. Caminho curto: mvn install deste branch e o spike msgforge-api/docs/spikes/cobalt-0.1. Medir: tempo do login, aviso de codigo de seguranca no destinatario, enc type=msg depois da resposta (na 0.0.10 era sempre pkmsg), ack sem phash, status. Resultados voltam para STATUS.md (itens 1.x e 2.x). Nao roda na nuvem.

## Outcome

- Signal: useful

## Source Nodes

- MessageEncryption
- IcdcComputer
- LiveDeviceService
- SuccessStreamHandler