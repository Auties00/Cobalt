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

## Referência de comparação

Os testes do usuário compararam o Cobalt com o **whatsmeow** (Go), que não apresentou os sintomas. O whatsmeow faz um bootstrap mínimo após o login; o Cobalt replica o bootstrap completo do WA Web (AB-props, privacidade, probes, sync de app-state), o que por definição é mais lento até o evento de login.

## Relação com a API (`jonales/msgforge-api`)

- A API usa `it.auties00:cobalt:0.0.10` (Maven Central). **Nenhuma correção deste fork chega à API** até ela migrar para o fork (ADR 0007 da API; pendência 5a do `CLAUDE.md` dela).
- Na 0.0.10 o login leva cerca de 60 s por causa de um timeout de IQ (`erro em LOGIN: Node timed out`), registrado nas memórias da API.
- O spike `docs/spikes/cobalt-0.1` da API é o teste de aceitação do fork: código de segurança, tempo de login, status, LID, nono dígito, envio repetido, sessão restaurada.

## Sintoma 1: conexão lenta com o WhatsApp

| # | Área | Estado | Evidência | Próximo passo |
|---|---|---|---|---|
| 1.1 | Bootstrap pós-`<success>` serial (~10 round-trips antes do `LoggedIn`) | CORRIGIDO (parcial) | `stream/control/SuccessStreamHandler.java` `bootstrap`: probes de compliance agora em background; privacy settings + 4 MEX de disallowed lists em paralelo com join antes do `LoggedIn` | Medir tempo `success -> LoggedIn` com log em sessão real |
| 1.2 | `abPropsService.sync()` com 3 tentativas e `sleep` aleatório de até 10 s entre elas | PENDENTE | `props/LiveABPropsService.java` `sync(Long, boolean)` | Se o 1º sync falhar o login atrasa até ~20 s; avaliar backoff menor ou sync em background (cuidado: `LID_ONE_ON_ONE_MIGRATION_ENABLED` depende dele) |
| 1.3 | Leituras bloqueantes de AB-prop no caminho de envio (`getBool(prop)` espera até 30 s pelo 1º sync) | HIPÓTESE | `UserMessageSender.shouldConvertToLid` usa `getBool(ABProp.WEB_PNLESS_STANZAS)` | Só afeta envio feito antes do 1º sync; confirmar com log de timestamps |
| 1.4 | `flushStore()` a cada envio | VALIDADO | `PersistentStore.save()` é debounced (flusher daemon) | Nada |

## Sintoma 2: "Não foi possível confirmar seu código de segurança" a cada mensagem enviada

Texto real do aviso (registrado no `msgforge-api`, `docs/spikes/cobalt-0.1/README.md`): aparece no celular de quem **recebe**, em toda mensagem, com a API rodando o **Cobalt 0.0.10 do Maven Central**, não este fork. O aviso vem da verificação ICDC: o destinatário compara o `deviceListMetadata` (hash das chaves e timestamp da lista de aparelhos de quem envia) com a lista que ele conhece. O fork calcula e envia o ICDC (`IcdcComputer`, `IcdcEnricher`, `MessageSender.encryptForDevices`); a validação ao vivo (teste 1 do spike) ainda está pendente.

| # | Área | Estado | Evidência | Próximo passo |
|---|---|---|---|---|
| 2.1 | Phash divergente no ack não refazia a lista de dispositivos (cache reutilizado para sempre; o servidor reporta phash em todo ack) | CORRIGIDO | `device/LiveDeviceService.java` `getDeviceLists`; teste `DeviceServiceTest.mismatchedPhashForcesServerSync` | Confirmar em sessão real que o ack para de trazer `phash` após o reenvio |
| 2.2 | Lote usync com falha sobrescrevia a lista em cache com "só primário" (perdia companions) | CORRIGIDO | `device/LiveDeviceService.java` `fetchDeviceListsFromServer` (loop de fallback) | Nada |
| 2.3 | `refresh_lid="true"` no ack chama `getDeviceLists(..., null, ...)`, que é no-op quando o usuário já está em cache | PENDENTE | `message/send/UserMessageSender.java` `maybeRefreshLid` | Forçar usync (mesma técnica de 2.1) após confirmar o fluxo `syncContactListJob` no WA Web |
| 2.5 | Sessão recriada a cada execução (novo pareamento) | DESCARTADO | Builder: `createConnection()` sempre cria sessão nova; `loadLatestOrCreateConnection()` / `registered()` reusam. `WhatsAppStoreFactory.temporary()` não persiste nada. O `msgforge-api` restaura a sessão do Postgres com `registered()` (`CobaltWhatsAppGateway.start`) | Nada |
| 2.4 | ICDC do remetente calculado sobre a própria lista de aparelhos em cache; se ela não tiver o companion atual, o hash não confere no destinatário | HIPÓTESE | `IcdcComputer.compute` usa `findDeviceList(self)`; a correção 2.1 refaz essa lista quando o ack traz `phash` | Teste 1 do spike `msgforge-api/docs/spikes/cobalt-0.1` com este branch instalado |

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
