# Revisão do motor Cobalt: ledger de posição e validação

Fonte única de "onde estamos" na revisão/refatoração do fork. Cada item tem estado,
evidência (arquivo:linha, teste ou consulta ao grafo) e próximo passo. Atualize este
arquivo e registre a memória Graphify correspondente a cada achado.

- Fork: `jonales/Cobalt`, base `3a53a668` (2026-11-12)
- Build: Java 25 (`openjdk-25-jdk-headless`), `mvn -DskipTests install -pl modules/lib -am` OK
- Grafo: `graphify update modules` -> `modules/graphify-out/` (83k nós, 221k arestas; `graph.json` não versionado)
- Memórias Graphify: `modules/graphify-out/memory/` (versionadas); consolidar com `graphify reflect --memory-dir modules/graphify-out/memory`

## Legenda

| Estado | Significado |
|---|---|
| CORRIGIDO | Mudança aplicada, compilada e coberta por teste |
| VALIDADO | Analisado; comportamento correto, nada a fazer |
| PENDENTE | Problema confirmado, ainda sem correção |
| HIPÓTESE | Suspeita, falta evidência (log/captura) |

## Sintoma 1: conexão lenta com o WhatsApp

| # | Área | Estado | Evidência | Próximo passo |
|---|---|---|---|---|
| 1.1 | Bootstrap pós-`<success>` serial (~10 round-trips antes do `LoggedIn`) | CORRIGIDO (parcial) | `stream/control/SuccessStreamHandler.java` `bootstrap`: probes de compliance agora em background; privacy settings + 4 MEX de disallowed lists em paralelo com join antes do `LoggedIn` | Medir tempo `success -> LoggedIn` com log em sessão real |
| 1.2 | `abPropsService.sync()` com 3 tentativas e `sleep` aleatório de até 10 s entre elas | PENDENTE | `props/LiveABPropsService.java` `sync(Long, boolean)` | Se o 1º sync falhar o login atrasa até ~20 s; avaliar backoff menor ou sync em background (cuidado: `LID_ONE_ON_ONE_MIGRATION_ENABLED` depende dele) |
| 1.3 | Leituras bloqueantes de AB-prop no caminho de envio (`getBool(prop)` espera até 30 s pelo 1º sync) | HIPÓTESE | `UserMessageSender.shouldConvertToLid` usa `getBool(ABProp.WEB_PNLESS_STANZAS)` | Só afeta envio feito antes do 1º sync; confirmar com log de timestamps |
| 1.4 | `flushStore()` a cada envio | VALIDADO | `PersistentStore.save()` é debounced (flusher daemon) | Nada |

## Sintoma 2: "mensagem de validação do código" a cada mensagem enviada

| # | Área | Estado | Evidência | Próximo passo |
|---|---|---|---|---|
| 2.1 | Phash divergente no ack não refazia a lista de dispositivos (cache reutilizado para sempre; o servidor reporta phash em todo ack) | CORRIGIDO | `device/LiveDeviceService.java` `getDeviceLists`; teste `DeviceServiceTest.mismatchedPhashForcesServerSync` | Confirmar em sessão real que o ack para de trazer `phash` após o reenvio |
| 2.2 | Lote usync com falha sobrescrevia a lista em cache com "só primário" (perdia companions) | CORRIGIDO | `device/LiveDeviceService.java` `fetchDeviceListsFromServer` (loop de fallback) | Nada |
| 2.3 | `refresh_lid="true"` no ack chama `getDeviceLists(..., null, ...)`, que é no-op quando o usuário já está em cache | PENDENTE | `message/send/UserMessageSender.java` `maybeRefreshLid` | Forçar usync (mesma técnica de 2.1) após confirmar o fluxo `syncContactListJob` no WA Web |
| 2.4 | Texto exato do aviso não identificado ("código de segurança mudou" x "Aguardando mensagem" x outro) | HIPÓTESE | Precisa de print/log do usuário | Coletar: texto exato, onde aparece (celular do destinatário? console?), log `DEBUG` de um envio |

## Testes conhecidos falhando (pré-existentes, fora do escopo)

- `DeviceServiceHandleDeviceNotificationTest` (3 erros): `IllegalArgumentException: event missing 'stanza' subtree` em `DeviceFixtures.buildNodeFromEvent`; problema de fixture, não de código de produção.

## Como continuar

```
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
mvn -DskipTests install -pl modules/lib -am
mvn -pl modules/lib test -Dtest='DeviceServiceTest'
graphify update modules                               # reconstrói o grafo após mudanças
graphify affected "<path>::<symbol>" --graph modules/graphify-out/graph.json
graphify save-result --memory-dir modules/graphify-out/memory --outcome useful --question "..." --answer "..." --nodes A B
```
